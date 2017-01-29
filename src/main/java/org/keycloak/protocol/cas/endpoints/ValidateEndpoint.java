package org.keycloak.protocol.cas.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.GET;
import javax.ws.rs.core.*;

public class ValidateEndpoint {
    protected static final Logger logger = Logger.getLogger(org.keycloak.protocol.oidc.endpoints.LogoutEndpoint.class);

    private static final String RESPONSE_OK = "yes\n";
    private static final String RESPONSE_FAILED = "no\n";

    @Context
    protected KeycloakSession session;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    protected UriInfo uriInfo;

    protected RealmModel realm;
    protected EventBuilder event;
    protected ClientModel client;
    protected ClientSessionModel clientSession;

    public ValidateEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @GET
    @NoCache
    public Response build() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        String service = params.getFirst(CASLoginProtocol.SERVICE_PARAM);
        String ticket = params.getFirst(CASLoginProtocol.TICKET_PARAM);
        boolean renew = "true".equalsIgnoreCase(params.getFirst(CASLoginProtocol.RENEW_PARAM));

        event.event(EventType.CODE_TO_TOKEN);

        try {
            checkSsl();
            checkRealm();
            checkClient(service);

            checkTicket(ticket, renew);

            event.success();
            return successResponse();
        } catch (CASValidationException e) {
            return errorResponse(e);
        }
    }

    protected Response successResponse() {
        return Response.ok(RESPONSE_OK).type(MediaType.TEXT_PLAIN).build();
    }

    protected Response errorResponse(CASValidationException e) {
        return Response.status(e.getStatus()).entity(RESPONSE_FAILED).type(MediaType.TEXT_PLAIN).build();
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CASValidationException(CASErrorCode.INTERNAL_ERROR, "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient(String service) {
        if (service == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Missing parameter: " + CASLoginProtocol.SERVICE_PARAM, Response.Status.BAD_REQUEST);
        }

        client = realm.getClients().stream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(uriInfo, service, realm, c) != null)
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

    private void checkTicket(String ticket, boolean requireReauth) {
        if (ticket == null) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Missing parameter: " + CASLoginProtocol.TICKET_PARAM, Response.Status.BAD_REQUEST);
        }
        if (!ticket.startsWith(CASLoginProtocol.SERVICE_TICKET_PREFIX)) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET_SPEC, "Malformed service ticket", Response.Status.BAD_REQUEST);
        }

        String code = ticket.substring(CASLoginProtocol.SERVICE_TICKET_PREFIX.length());

        ClientSessionCode.ParseResult parseResult = ClientSessionCode.parseResult(code, session, realm);
        if (parseResult.isClientSessionNotFound() || parseResult.isIllegalHash()) {
            String[] parts = code.split("\\.");
            if (parts.length == 2) {
                event.detail(Details.CODE_ID, parts[1]);
            }
            event.error(Errors.INVALID_CODE);
            if (parseResult.getClientSession() != null) {
                session.sessions().removeClientSession(realm, parseResult.getClientSession());
            }
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code not valid", Response.Status.BAD_REQUEST);
        }

        clientSession = parseResult.getClientSession();
        event.detail(Details.CODE_ID, clientSession.getId());

        if (!parseResult.getCode().isValid(ClientSessionModel.Action.CODE_TO_TOKEN.name(), ClientSessionCode.ActionType.CLIENT)) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code is expired", Response.Status.BAD_REQUEST);
        }

        parseResult.getCode().setAction(null);

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
}
