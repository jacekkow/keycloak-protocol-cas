package org.keycloak.protocol.cas;

import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.protocol.cas.utils.RedirectUtils;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedirectUtilsTest {

    private KeycloakSession createMockSession() {
        KeycloakSession session = mock(KeycloakSession.class);
        KeycloakContext context = mock(KeycloakContext.class);
        KeycloakUriInfo uriInfo = mock(KeycloakUriInfo.class);
        when(session.getContext()).thenReturn(context);
        when(context.getUri()).thenReturn(uriInfo);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/auth/"));
        return session;
    }

    @Test
    public void testVerifyRedirectUriNull() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        assertNull(RedirectUtils.verifyRedirectUri(session, null, client));
    }

    @Test
    public void testVerifyRedirectUriWithoutForbiddenParams() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String serviceUrl = "https://localhost:5003/signin-cas";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithState() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ81234567890";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithEncodedChars() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8%20123";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithForbiddenOidcParams() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String[] oidcParams = new String[]{"code", "id_token", "access_token", "session_state", "error"};
        for (String param : oidcParams) {
            String serviceUrl = "https://localhost:5003/signin-cas?" + param + "=testValue";
            String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);
            assertEquals(serviceUrl, verified, "Failed for parameter: " + param);
        }
    }

    @Test
    public void testVerifyRedirectUriWithMultipleParams() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8&custom_param=value123";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithFragment() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8#section1";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriInvalidDomain() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of("https://localhost:5003/*"));

        String serviceUrl = "https://evil.com/signin-cas?state=CfDJ8";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertNull(verified);
    }
}
