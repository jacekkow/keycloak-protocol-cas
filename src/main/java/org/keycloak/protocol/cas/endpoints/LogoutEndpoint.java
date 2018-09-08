package org.keycloak.protocol.cas.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class LogoutEndpoint {
    private static final Logger logger = Logger.getLogger(LogoutEndpoint.class);

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    private RealmModel realm;
    private EventBuilder event;
    private ClientModel client;
    private String redirectUri;

    public LogoutEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @GET
    @NoCache
    public Response logout(@QueryParam(CASLoginProtocol.SERVICE_PARAM) String service) {
        checkClient(service);

        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, CASLoginProtocol.LOGIN_PROTOCOL);
            if (redirectUri != null) userSession.setNote(CASLoginProtocol.LOGOUT_REDIRECT_URI, redirectUri);

            logger.debug("Initiating CAS browser logout");
            Response response =  AuthenticationManager.browserLogout(session, realm, authResult.getSession(), session.getContext().getUri(), clientConnection, headers);
            logger.debug("finishing CAS browser logout");
            return response;
        }
        return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_LOGOUT);
    }

    private void checkClient(String service) {
        if (service == null) {
            return;
        }

        client = realm.getClients().stream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(session.getContext().getUri(), service, realm, c) != null)
                .findFirst().orElse(null);
        if (client != null) {
            redirectUri = RedirectUtils.verifyRedirectUri(session.getContext().getUri(), service, realm, client);

            session.getContext().setClient(client);
        }
    }
}
