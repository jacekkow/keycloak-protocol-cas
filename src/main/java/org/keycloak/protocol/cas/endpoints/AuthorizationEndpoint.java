package org.keycloak.protocol.cas.endpoints;

import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.GET;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class AuthorizationEndpoint extends AuthorizationEndpointBase {
    private static final Logger logger = Logger.getLogger(AuthorizationEndpoint.class);

    private ClientModel client;
    private AuthenticationSessionModel authenticationSession;
    private String redirectUri;

    public AuthorizationEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
        event.event(EventType.LOGIN);
    }

    @GET
    public Response build() {
        MultivaluedMap<String, String> params = session.getContext().getUri().getQueryParameters();
        String service = params.getFirst(CASLoginProtocol.SERVICE_PARAM);

        boolean isSaml11Request = false;
        if (service == null && params.containsKey(CASLoginProtocol.TARGET_PARAM)) {
            // SAML 1.1 authorization uses the TARGET parameter instead of service
            service = params.getFirst(CASLoginProtocol.TARGET_PARAM);
            isSaml11Request = true;
        }
        boolean renew = params.containsKey(CASLoginProtocol.RENEW_PARAM);
        boolean gateway = params.containsKey(CASLoginProtocol.GATEWAY_PARAM);

        checkSsl();
        checkRealm();
        checkClient(service);

        authenticationSession = createAuthenticationSession(client, null);
        updateAuthenticationSession();

        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();

        if (renew) {
            authenticationSession.setClientNote(CASLoginProtocol.RENEW_PARAM, "true");
        }
        if (gateway) {
            authenticationSession.setClientNote(CASLoginProtocol.GATEWAY_PARAM, "true");
        }
        if (isSaml11Request) {
            // Flag the session so we can return the ticket as "SAMLart" in the response
            authenticationSession.setClientNote(CASLoginProtocol.TARGET_PARAM, "true");
        }

        this.event.event(EventType.LOGIN);
        return handleBrowserAuthenticationRequest(authenticationSession, new CASLoginProtocol(session, realm, session.getContext().getUri(), headers, event), gateway, false);
    }

    private void checkClient(String service) {
        if (service == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER, CASLoginProtocol.SERVICE_PARAM);
        }

        event.detail(Details.REDIRECT_URI, service);

        client = realm.getClientsStream()
                .filter(c -> CASLoginProtocol.LOGIN_PROTOCOL.equals(c.getProtocol()))
                .filter(c -> RedirectUtils.verifyRedirectUri(session, service, c) != null)
                .findFirst().orElse(null);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.CLIENT_DISABLED);
        }

        redirectUri = RedirectUtils.verifyRedirectUri(session, service, client);

        event.client(client.getClientId());
        event.detail(Details.REDIRECT_URI, redirectUri);

        session.getContext().setClient(client);
    }

    private void updateAuthenticationSession() {
        authenticationSession.setProtocol(CASLoginProtocol.LOGIN_PROTOCOL);
        authenticationSession.setRedirectUri(redirectUri);
        authenticationSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
    }
}
