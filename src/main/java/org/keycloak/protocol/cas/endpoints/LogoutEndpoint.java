package org.keycloak.protocol.cas.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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

    @Context
    private UriInfo uriInfo;

    private RealmModel realm;
    private EventBuilder event;

    public LogoutEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @GET
    @NoCache
    public Response logout() {

        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, CASLoginProtocol.LOGIN_PROTOCOL);

            logger.debug("Initiating CAS browser logout");
            Response response =  AuthenticationManager.browserLogout(session, realm, authResult.getSession(), uriInfo, clientConnection, headers);
            logger.debug("finishing CAS browser logout");
            return response;
        }
        return Response.ok().build();
    }
}
