package org.keycloak.protocol.cas.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.mappers.CASAttributeMapper;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.util.DefaultClientSessionContext;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractValidateEndpoint {
    protected final Logger logger = Logger.getLogger(getClass());
    @Context
    protected KeycloakSession session;
    @Context
    protected ClientConnection clientConnection;
    @Context
    protected HttpRequest request;
    @Context
    protected HttpHeaders headers;
    protected RealmModel realm;
    protected EventBuilder event;
    protected ClientModel client;
    protected AuthenticatedClientSessionModel clientSession;

    public AbstractValidateEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    protected void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    protected void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CASValidationException(CASErrorCode.INTERNAL_ERROR, "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    protected void checkClient(String service) {
        if (service == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Missing parameter: " + CASLoginProtocol.SERVICE_PARAM, Response.Status.BAD_REQUEST);
        }

        client = realm.getClients().stream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(session.getContext().getUri(), service, realm, c) != null)
                .findFirst().orElse(null);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new CASValidationException(CASErrorCode.INVALID_SERVICE, "Client not found", Response.Status.BAD_REQUEST);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new CASValidationException(CASErrorCode.INVALID_SERVICE, "Client disabled", Response.Status.BAD_REQUEST);
        }

        event.client(client.getClientId());

        session.getContext().setClient(client);
    }

    protected void checkTicket(String ticket, boolean requireReauth) {
        if (ticket == null) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Missing parameter: " + CASLoginProtocol.TICKET_PARAM, Response.Status.BAD_REQUEST);
        }
        if (!ticket.startsWith(CASLoginProtocol.SERVICE_TICKET_PREFIX)) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET_SPEC, "Malformed service ticket", Response.Status.BAD_REQUEST);
        }

        String code = ticket.substring(CASLoginProtocol.SERVICE_TICKET_PREFIX.length());

        String[] parts = code.split("\\.");
        if (parts.length == 4) {
            event.detail(Details.CODE_ID, parts[2]);
        }

        ClientSessionCode.ParseResult<AuthenticatedClientSessionModel> parseResult = ClientSessionCode.parseResult(code, null, session, realm, client, event, AuthenticatedClientSessionModel.class);
        if (parseResult.isAuthSessionNotFound() || parseResult.isIllegalHash()) {
            event.error(Errors.INVALID_CODE);

            // Attempt to use same code twice should invalidate existing clientSession
            AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();
            if (clientSession != null) {
                clientSession.detachFromUserSession();
            }

            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code not valid", Response.Status.BAD_REQUEST);
        }

        clientSession = parseResult.getClientSession();

        if (parseResult.isExpiredToken()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code is expired", Response.Status.BAD_REQUEST);
        }

        clientSession.setNote(CASLoginProtocol.SESSION_SERVICE_TICKET, ticket);

        if (requireReauth && AuthenticationManager.isSSOAuthentication(clientSession)) {
            event.error(Errors.SESSION_EXPIRED);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Interactive authentication was requested but not performed", Response.Status.BAD_REQUEST);
        }

        UserSessionModel userSession = clientSession.getUserSession();

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "User session not found", Response.Status.BAD_REQUEST);
        }

        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "User not found", Response.Status.BAD_REQUEST);
        }
        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "User disabled", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());
        event.session(userSession.getId());

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_SERVICE, "Auth error", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Session not active", Response.Status.BAD_REQUEST);
        }
    }

    protected Map<String, Object> getUserAttributes() {
        UserSessionModel userSession = clientSession.getUserSession();
        // CAS protocol does not support scopes, so pass null scopeParam
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, null);

        Set<ProtocolMapperModel> mappings = clientSessionCtx.getProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Map<String, Object> attributes = new HashMap<>();
        for (ProtocolMapperModel mapping : mappings) {
            ProtocolMapper mapper = (ProtocolMapper) sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper instanceof CASAttributeMapper) {
                ((CASAttributeMapper) mapper).setAttribute(attributes, mapping, userSession, session, clientSessionCtx);
            }
        }
        return attributes;
    }
}
