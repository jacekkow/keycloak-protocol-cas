package org.keycloak.protocol.cas;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.cas.endpoints.*;
import org.keycloak.services.resources.RealmsResource;

public class CASLoginProtocolService {
    private KeycloakSession session;
    private RealmModel realm;
    private EventBuilder event;

    public CASLoginProtocolService(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.event = event;
    }

    public static UriBuilder serviceBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path("{realm}/protocol/" + CASLoginProtocol.LOGIN_PROTOCOL);
    }

    @Path("login")
    public Object login() {
        return new AuthorizationEndpoint(session, event);
    }

    @Path("logout")
    public Object logout() {
        return new LogoutEndpoint(session, realm);
    }

    @Path("validate")
    public Object validate() {
        return new ValidateEndpoint(session, realm, event);
    }

    @Path("samlValidate")
    public Object validateSaml11() {
        return new SamlValidateEndpoint(session, realm, event);
    }

    @Path("serviceValidate")
    public Object serviceValidate() {
        return new ServiceValidateEndpoint(session, realm, event);
    }

    @Path("proxyValidate")
    public Object proxyValidate() {
        return new ProxyValidateEndpoint(session, realm, event);
    }

    @Path("proxy")
    public Object proxy() {
        return new ProxyEndpoint(session, realm, event);
    }

    @Path("p3/serviceValidate")
    public Object p3ServiceValidate() {
        return serviceValidate();
    }

    @Path("p3/proxyValidate")
    public Object p3ProxyValidate() {
        return proxyValidate();
    }
}
