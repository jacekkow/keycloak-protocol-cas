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

    private ClientModel createMockClient(String... redirectUris) {
        ClientModel client = mock(ClientModel.class);
        when(client.getRedirectUris()).thenReturn(Set.of(redirectUris));
        return client;
    }

    @Test
    public void testVerifyRedirectUriNull() {
        KeycloakSession session = createMockSession();
        ClientModel client = mock(ClientModel.class);
        assertNull(RedirectUtils.verifyRedirectUri(session, null, client));
        assertNull(RedirectUtils.verifyRedirectUri(session, "https://localhost:5003/signin-cas", null));
        assertNull(RedirectUtils.verifyRedirectUri(session, null, null));
    }

    @Test
    public void testVerifyRedirectUriWithoutForbiddenParams() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://localhost:5003/signin-cas";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithState() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ81234567890";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithAllowedQueryParams() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String[] allowedParams = new String[]{"code", "id_token", "access_token", "session_state", "error", "custom_param"};
        for (String param : allowedParams) {
            String serviceUrl = "https://localhost:5003/signin-cas?" + param + "=testValue";
            String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);
            assertEquals(serviceUrl, verified, "Should be allowed for parameter: " + param);
        }
    }

    @Test
    public void testVerifyRedirectUriWithEncodedChars() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8%20123";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithForbiddenCasParams() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String[] forbiddenParams = new String[]{"ticket", "TICKET", "Ticket", "SAMLart", "samlart", "SAMLART"};
        for (String param : forbiddenParams) {
            String serviceUrl = "https://localhost:5003/signin-cas?" + param + "=ST-123456";
            String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);
            assertNull(verified, "Should be rejected for parameter: " + param);
        }
    }

    @Test
    public void testVerifyRedirectUriWithStateAndForbiddenCasParam() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8&ticket=ST-123456";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertNull(verified);

        String samlUrl = "https://localhost:5003/signin-cas?state=CfDJ8&SAMLart=ST-123456";
        String samlVerified = RedirectUtils.verifyRedirectUri(session, samlUrl, client);

        assertNull(samlVerified);
    }

    @Test
    public void testVerifyRedirectUriWithStateAndCustomParam() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8&custom_param=value123";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriWithFragment() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://localhost:5003/signin-cas?state=CfDJ8#section1";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertEquals(serviceUrl, verified);
    }

    @Test
    public void testVerifyRedirectUriInvalidDomain() {
        KeycloakSession session = createMockSession();
        ClientModel client = createMockClient("https://localhost:5003/*");

        String serviceUrl = "https://evil.com/signin-cas?state=CfDJ8";
        String verified = RedirectUtils.verifyRedirectUri(session, serviceUrl, client);

        assertNull(verified);
    }

    @Test
    public void testVerifyRedirectUriWithoutRequireRedirectUri() {
        KeycloakSession session = createMockSession();
        ClientModel singleRedirectClient = createMockClient("https://localhost:5003/*");

        String verified = RedirectUtils.verifyRedirectUri(session, null, singleRedirectClient, false);
        assertEquals("https://localhost:5003", verified);

        ClientModel multiRedirectClient = createMockClient("https://localhost:5003/*", "https://localhost:5004/*");
        assertNull(RedirectUtils.verifyRedirectUri(session, null, multiRedirectClient, false));
    }
}
