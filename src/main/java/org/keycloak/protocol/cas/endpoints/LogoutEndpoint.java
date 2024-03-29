package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import java.net.URI;

public class LogoutEndpoint {
    private static final Logger logger = Logger.getLogger(LogoutEndpoint.class);

    private KeycloakSession session;

    private RealmModel realm;
    private String redirectUri;

    public LogoutEndpoint(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    @GET
    @NoCache
    public Response logout(@QueryParam(CASLoginProtocol.SERVICE_PARAM) String service) {
        checkClient(service);

        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, CASLoginProtocol.LOGIN_PROTOCOL);

            if (redirectUri != null) {
                userSession.setNote(CASLoginProtocol.LOGOUT_REDIRECT_URI, redirectUri);
            }

            logger.debug("Initiating CAS browser logout");
            Response response = AuthenticationManager.browserLogout(session, realm, authResult.getSession(), session.getContext().getUri(), session.getContext().getConnection(), session.getContext().getRequestHeaders());
            logger.debug("finishing CAS browser logout");
            return response;
        }

        if (redirectUri != null) {
            logger.debugv("no active session, redirecting to {0}", redirectUri);
            return Response.status(302).location(URI.create(redirectUri)).build();
        }

        return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_LOGOUT);
    }

    private void checkClient(String service) {
        if (service == null) {
            return;
        }

        ClientModel client = realm.getClientsStream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(session, service, c) != null)
                .findFirst().orElse(null);
        if (client != null) {
            redirectUri = RedirectUtils.verifyRedirectUri(session, service, client);

            session.getContext().setClient(client);
        }
    }
}
