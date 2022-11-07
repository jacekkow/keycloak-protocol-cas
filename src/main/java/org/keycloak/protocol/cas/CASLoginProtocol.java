package org.keycloak.protocol.cas;

import org.apache.http.HttpEntity;
import org.jboss.logging.Logger;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.cas.utils.LogoutHelper;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public class CASLoginProtocol implements LoginProtocol {
    private static final Logger logger = Logger.getLogger(CASLoginProtocol.class);

    public static final String LOGIN_PROTOCOL = "cas";

    public static final String SERVICE_PARAM = "service";
    public static final String TARGET_PARAM = "TARGET";
    public static final String RENEW_PARAM = "renew";
    public static final String GATEWAY_PARAM = "gateway";
    public static final String TICKET_PARAM = "ticket";
    public static final String FORMAT_PARAM = "format";

    public static final String TICKET_RESPONSE_PARAM = "ticket";
    public static final String SAMLART_RESPONSE_PARAM = "SAMLart";

    public static final String SERVICE_TICKET_PREFIX = "ST-";
    public static final String SESSION_SERVICE_TICKET = "service_ticket";

    public static final String LOGOUT_REDIRECT_URI = "CAS_LOGOUT_REDIRECT_URI";

    protected KeycloakSession session;
    protected RealmModel realm;
    protected UriInfo uriInfo;
    protected HttpHeaders headers;
    protected EventBuilder event;

    public CASLoginProtocol(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, EventBuilder event) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.headers = headers;
        this.event = event;
    }

    public CASLoginProtocol() {
    }

    @Override
    public CASLoginProtocol setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public CASLoginProtocol setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public CASLoginProtocol setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public CASLoginProtocol setHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public CASLoginProtocol setEventBuilder(EventBuilder event) {
        this.event = event;
        return this;
    }

    @Override
    public Response authenticated(AuthenticationSessionModel authSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();

        String service = authSession.getRedirectUri();
        //TODO validate service

        OAuth2Code codeData = new OAuth2Code(UUID.randomUUID().toString(),
                Time.currentTime() + userSession.getRealm().getAccessCodeLifespan(),
                null, null, authSession.getRedirectUri(), null, null);
        String code = OAuth2CodeParser.persistCode(session, clientSession, codeData);

        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(service);

        String loginTicket = SERVICE_TICKET_PREFIX + code;

        if (authSession.getClientNotes().containsKey(CASLoginProtocol.TARGET_PARAM)) {
            // This was a SAML 1.1 auth request so return the ticket ID as "SAMLart" instead of "ticket"
            uriBuilder.queryParam(SAMLART_RESPONSE_PARAM, loginTicket);
        } else {
            uriBuilder.queryParam(TICKET_RESPONSE_PARAM, loginTicket);
        }

        URI redirectUri = uriBuilder.build();

        Response.ResponseBuilder location = Response.status(302).location(redirectUri);
        return location.build();
    }

    @Override
    public Response sendError(AuthenticationSessionModel authSession, Error error) {
        if (authSession.getClientNotes().containsKey(CASLoginProtocol.GATEWAY_PARAM)) {
            if (error == Error.PASSIVE_INTERACTION_REQUIRED || error == Error.PASSIVE_LOGIN_REQUIRED) {
                return Response.status(302).location(URI.create(authSession.getRedirectUri())).build();
            }
        }
        return ErrorPage.error(session, authSession, Response.Status.INTERNAL_SERVER_ERROR, error.name());
    }

    @Override
    public Response backchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        String logoutUrl = clientSession.getRedirectUri();
        String serviceTicket = clientSession.getNote(CASLoginProtocol.SESSION_SERVICE_TICKET);
        //check if session is fully authenticated (i.e. serviceValidate has been called)
        if (serviceTicket != null && !serviceTicket.isEmpty()) {
            sendSingleLogoutRequest(logoutUrl, serviceTicket);
        }
        ClientModel client = clientSession.getClient();
        new ResourceAdminManager(session).logoutClientSession(realm, client, clientSession);
        return Response.ok().build();
    }

    private void sendSingleLogoutRequest(String logoutUrl, String serviceTicket) {
        try {
            HttpEntity requestEntity = LogoutHelper.buildSingleLogoutRequest(serviceTicket);
            LogoutHelper.postWithRedirect(session, logoutUrl, requestEntity);
            logger.debug("Sent CAS single logout for service " + logoutUrl);
        } catch (IOException e) {
            logger.warn("Failed to call CAS service for logout: " + logoutUrl, e);
        }
    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        // todo oidc redirect support
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    @Override
    public Response finishBrowserLogout(UserSessionModel userSession, AuthenticationSessionModel logoutSession) {
        String redirectUri = userSession.getNote(CASLoginProtocol.LOGOUT_REDIRECT_URI);

        event.event(EventType.LOGOUT);
        event.user(userSession.getUser()).session(userSession).success();

        if (redirectUri != null) {
            return Response.status(302).location(URI.create(redirectUri)).build();
        } else {
            LoginFormsProvider infoPage = session.getProvider(LoginFormsProvider.class).setSuccess("Logout successful");
            infoPage.setAttribute("skipLink", true);
            return infoPage.createInfoPage();
        }
    }

    @Override
    public boolean requireReauthentication(UserSessionModel userSession, AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getClientNote(CASLoginProtocol.RENEW_PARAM));
    }

    @Override
    public void close() {

    }
}
