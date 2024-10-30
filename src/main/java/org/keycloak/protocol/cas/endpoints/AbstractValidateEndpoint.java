package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.mappers.CASAttributeMapper;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.services.util.DefaultClientSessionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;

public abstract class AbstractValidateEndpoint {
    protected final Logger logger = Logger.getLogger(getClass());
    private static final Pattern DOT = Pattern.compile("\\.");
    protected KeycloakSession session;
    protected RealmModel realm;
    protected EventBuilder event;
    protected ClientModel client;
    protected AuthenticatedClientSessionModel clientSession;
    protected String pgtIou;

    public AbstractValidateEndpoint(KeycloakSession session, RealmModel realm, EventBuilder event) {
        this.session = session;
        this.realm = realm;
        this.event = event;
    }

    protected void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(session.getContext().getConnection())) {
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

        event.detail(Details.REDIRECT_URI, service);

        client = realm.getClientsStream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(session, service, c) != null)
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

    protected void checkTicket(String ticket, String prefix, boolean requireReauth) {
        if (ticket == null) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Missing parameter: " + CASLoginProtocol.TICKET_PARAM, Response.Status.BAD_REQUEST);
        }

        if (!ticket.startsWith(prefix)) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET_SPEC, "Malformed service ticket", Response.Status.BAD_REQUEST);
        }

        boolean isReusable = ticket.startsWith(CASLoginProtocol.PROXY_GRANTING_TICKET_PREFIX);

        String[] parsed = DOT.split(ticket.substring(prefix.length()), 3);
        if (parsed.length != 3) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET_SPEC, "Invalid format of the code", Response.Status.BAD_REQUEST);
        }

        String codeUUID = parsed[0];
        String userSessionId = parsed[1];
        String clientUUID = parsed[2];

        event.detail(Details.CODE_ID, userSessionId);
        event.session(userSessionId);

        // Retrieve UserSession
        UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, userSessionId, clientUUID);
        if (userSession == null) {
            // Needed to track if code is invalid
            userSession = session.sessions().getUserSession(realm, userSessionId);
            if (userSession == null) {
                event.error(Errors.USER_SESSION_NOT_FOUND);
                throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code not valid", Response.Status.BAD_REQUEST);
            }
        }

        clientSession = userSession.getAuthenticatedClientSessionByClient(clientUUID);
        if (clientSession == null) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code not valid", Response.Status.BAD_REQUEST);
        }

        SingleUseObjectProvider codeStore = session.singleUseObjects();
        Map<String, String> codeDataSerialized = isReusable ? codeStore.get(prefix + codeUUID) : codeStore.remove(prefix + codeUUID);

        // Either code not available
        if (codeDataSerialized == null) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code not valid", Response.Status.BAD_REQUEST);
        }

        OAuth2Code codeData = OAuth2Code.deserializeCode(codeDataSerialized);

        String persistedUserSessionId = codeData.getUserSessionId();
        if (!userSessionId.equals(persistedUserSessionId)) {
            event.error(Errors.INVALID_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code not valid", Response.Status.BAD_REQUEST);
        }

        // Finally doublecheck if code is not expired
        int currentTime = Time.currentTime();
        if (currentTime > codeData.getExpiration()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Code is expired", Response.Status.BAD_REQUEST);
        }

        clientSession.setNote(CASLoginProtocol.SESSION_TICKET, ticket);

        if (requireReauth && AuthenticationManager.isSSOAuthentication(clientSession)) {
            event.error(Errors.SESSION_EXPIRED);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Interactive authentication was requested but not performed", Response.Status.BAD_REQUEST);
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

        if (client == null) {
            client = clientSession.getClient();
        } else {
            if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
                event.error(Errors.INVALID_CODE);
                throw new CASValidationException(CASErrorCode.INVALID_SERVICE, "Invalid service", Response.Status.BAD_REQUEST);
            }
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, "Session not active", Response.Status.BAD_REQUEST);
        }

    }

    protected void createProxyGrant(String pgtUrl) {
        if ( RedirectUtils.verifyRedirectUri(session, pgtUrl, client) == null ) {
            event.error(Errors.INVALID_REQUEST);
            throw new CASValidationException(CASErrorCode.INVALID_PROXY_CALLBACK, "Proxy callback is invalid", Response.Status.BAD_REQUEST);
        }

        String pgtIou = getPGTIOU();
        String pgtId  = getPGT(session, clientSession, pgtUrl);

        try {
            HttpResponse response = HttpClientBuilder.create().build().execute(
                new HttpGet(new URIBuilder(pgtUrl).setParameter("pgtIou",pgtIou).setParameter("pgtId",pgtId).build())
            );

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception();
            }

            this.pgtIou = pgtIou;
        } catch (Exception e) {
            event.error(Errors.INVALID_REQUEST);
            throw new CASValidationException(CASErrorCode.PROXY_CALLBACK_ERROR, "Proxy callback returned an error", Response.Status.BAD_REQUEST);
        }
    }

    protected Map<String, Object> getUserAttributes() {
        UserSessionModel userSession = clientSession.getUserSession();
        // CAS protocol does not support scopes, so pass null scopeParam
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, null, session);

        Set<ProtocolMapperModel> mappings = clientSessionCtx.getProtocolMappersStream().collect(Collectors.toSet());
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

    protected String getPGTIOU()
    {
        return CASLoginProtocol.PROXY_GRANTING_TICKET_IOU_PREFIX + UUID.randomUUID().toString();
    }

    protected String getPGT(KeycloakSession session, AuthenticatedClientSessionModel clientSession, String pgtUrl)
    {
        return persistedTicket(pgtUrl, CASLoginProtocol.PROXY_GRANTING_TICKET_PREFIX);
    }

    protected String getPT(KeycloakSession session, AuthenticatedClientSessionModel clientSession, String targetService)
    {
        return persistedTicket(targetService, CASLoginProtocol.PROXY_TICKET_PREFIX);
    }

    protected String getST(String redirectUri)
    {
        return persistedTicket(redirectUri, CASLoginProtocol.SERVICE_TICKET_PREFIX);
    }

    public static String getST(KeycloakSession session, AuthenticatedClientSessionModel clientSession, String redirectUri)
    {
        ValidateEndpoint vp = new ValidateEndpoint(session,null,null);
        vp.clientSession = clientSession;
        return vp.getST(redirectUri);
    }

    protected String persistedTicket(String redirectUriParam, String prefix)
    {
        String key = UUID.randomUUID().toString();
        UserSessionModel userSession = clientSession.getUserSession();
        OAuth2Code codeData = new OAuth2Code(key, Time.currentTime() + userSession.getRealm().getAccessCodeLifespan(), null, null, redirectUriParam, null, null, userSession.getId());
        session.singleUseObjects().put(prefix + key, clientSession.getUserSession().getRealm().getAccessCodeLifespan(), codeData.serializeCode());
        return prefix + key + "." + clientSession.getUserSession().getId() + "." + clientSession.getClient().getId();
    }
}
