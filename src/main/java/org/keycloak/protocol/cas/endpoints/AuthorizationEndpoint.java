package org.keycloak.protocol.cas.endpoints;

import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;

import javax.ws.rs.GET;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class AuthorizationEndpoint extends AuthorizationEndpointBase {
    private static final Logger logger = Logger.getLogger(AuthorizationEndpoint.class);

    private ClientModel client;
    private ClientSessionModel clientSession;
    private String redirectUri;

    public AuthorizationEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
        event.event(EventType.LOGIN);
    }

    @GET
    public Response build() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        String service = params.getFirst(CASLoginProtocol.SERVICE_PARAM);
        boolean renew = "true".equalsIgnoreCase(params.getFirst(CASLoginProtocol.RENEW_PARAM));
        boolean gateway = "true".equalsIgnoreCase(params.getFirst(CASLoginProtocol.GATEWAY_PARAM));

        checkSsl();
        checkRealm();
        checkClient(service);

        createClientSession();
        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();

        this.event.event(EventType.LOGIN);
        return handleBrowserAuthenticationRequest(clientSession, new CASLoginProtocol(session, realm, uriInfo, headers, event, renew), gateway, false);
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            event.error(Errors.SSL_REQUIRED);
            throw new ErrorPageException(session, Messages.HTTPS_REQUIRED);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new ErrorPageException(session, Messages.REALM_NOT_ENABLED);
        }
    }

    private void checkClient(String service) {
        if (service == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Messages.MISSING_PARAMETER, CASLoginProtocol.SERVICE_PARAM);
        }

        client = realm.getClients().stream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(uriInfo, service, realm, c) != null)
                .findFirst().orElse(null);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, Messages.CLIENT_NOT_FOUND);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, Messages.CLIENT_DISABLED);
        }

        if (client.isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, Messages.BEARER_ONLY);
        }

        redirectUri = RedirectUtils.verifyRedirectUri(uriInfo, service, realm, client);

        event.client(client.getClientId());
        event.detail(Details.REDIRECT_URI, redirectUri);

        session.getContext().setClient(client);
    }

    private void createClientSession() {
        clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(CASLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirectUri);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
    }
}
