package org.keycloak.protocol.cas.installation;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.services.resources.RealmsResource;

import java.net.URI;

public class KeycloakCASClientInstallation implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        UriBuilder bindingUrlBuilder = UriBuilder.fromUri(baseUri);
        String bindingUrl = RealmsResource.protocolUrl(bindingUrlBuilder)
                .build(realm.getName(), CASLoginProtocol.LOGIN_PROTOCOL).toString();
        String description = "CAS Server URL: " + bindingUrl + "\n" +
                "CAS Protocol: CAS 2.0/3.0, SAML 1.1\n" +
                "Use CAS REST API: false (unsupported)";
        return Response.ok(description, MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return CASLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Plain CAS configuration";
    }

    @Override
    public String getHelpText() {
        return "CAS configuration properties required by CAS clients. Enter the values shown below into the configuration dialog of your client SP.";
    }

    @Override
    public void close() {

    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "keycloak-cas-text";
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public String getFilename() {
        return "keycloak-cas.txt";
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_PLAIN;
    }

}
