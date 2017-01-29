package org.keycloak.protocol.cas;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class CASLoginProtocol implements LoginProtocol {
    public static final String LOGIN_PROTOCOL = "cas";

    public static final String SERVICE_PARAM = "service";
    public static final String RENEW_PARAM = "renew";
    public static final String GATEWAY_PARAM = "gateway";
    public static final String TICKET_PARAM = "ticket";
    public static final String FORMAT_PARAM = "format";

    public static final String TICKET_RESPONSE_PARAM = "ticket";

    public static final String SERVICE_TICKET_PREFIX = "ST-";

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
    public Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode) {
        ClientSessionModel clientSession = accessCode.getClientSession();

        String service = clientSession.getRedirectUri();
        //TODO validate service
        accessCode.setAction(ClientSessionModel.Action.CODE_TO_TOKEN.name());
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(service);
        uriBuilder.queryParam(TICKET_RESPONSE_PARAM, SERVICE_TICKET_PREFIX + accessCode.getCode());

        URI redirectUri = uriBuilder.build();

        Response.ResponseBuilder location = Response.status(302).location(redirectUri);
        return location.build();
    }

    @Override
    public Response sendError(ClientSessionModel clientSession, Error error) {
        return Response.serverError().entity(error).build();
    }

    @Override
    public void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        new ResourceAdminManager(session).logoutClientSession(uriInfo.getRequestUri(), realm, client, clientSession);
    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        // todo oidc redirect support
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    @Override
    public Response finishLogout(UserSessionModel userSession) {
        event.event(EventType.LOGOUT);
        event.user(userSession.getUser()).session(userSession).success();
        return Response.ok().build();
    }

    @Override
    public boolean requireReauthentication(UserSessionModel userSession, ClientSessionModel clientSession) {
        return "true".equals(clientSession.getNote(CASLoginProtocol.RENEW_PARAM));
    }

    @Override
    public void close() {

    }
}
