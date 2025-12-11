package org.keycloak.protocol.cas;

import com.microsoft.playwright.Page;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.protocol.cas.playwright.PlaywrightDefaults;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.common.util.Encode.urlEncode;

@Testcontainers
public class TicketIT {
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASSWORD = "keycloak";
  private static final String REALM_NAME = "cas-realm";
  private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");
  private static final String CAS_CLIENT_1 = "cas_client_1";
  private static final String SERVICE_URL_1 = "http://localhost:4200/callback";
  private static final String TEST_USERNAME = "tanja@test.de";
  private static final String TEST_USER_PASSWORD = "123";
  private static final String TEST_USER_FIRST_NAME = "Tanja";
  private static final String TEST_USER_LAST_NAME = "Test";

  @Container
  private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
      .withProviderClassesFrom("target/classes")
      .withAdminUsername(ADMIN_USER)
      .withAdminPassword(ADMIN_PASSWORD)
      .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
      .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASSWORD);

  private PlaywrightDefaults playwrightDefaults;
  private Page page;
  private static Keycloak keycloakClient;

  @BeforeAll
  static void beforeAll() {
    keycloakClient = keycloakContainer.getKeycloakAdminClient();
    // Create realm
    RealmRepresentation testRealm = new RealmRepresentation();
    testRealm.setRealm(REALM_NAME);
    testRealm.setEnabled(true);
    keycloakClient.realms().create(testRealm);

    RealmResource realm = keycloakClient.realm(REALM_NAME);

    // Create user
    UserRepresentation user = new UserRepresentation();
    user.setUsername(TEST_USERNAME);
    user.setEmail(TEST_USERNAME);
    user.setFirstName(TEST_USER_FIRST_NAME);
    user.setLastName(TEST_USER_LAST_NAME);
    user.setEnabled(true);
    UsersResource users = realm.users();
    var createUserResponse = users.create(user);
    String userId = createUserResponse.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

    CredentialRepresentation password = new CredentialRepresentation();
    password.setType(CredentialRepresentation.PASSWORD);
    password.setValue(TEST_USER_PASSWORD);
    password.setTemporary(false);
    users.get(userId).resetPassword(password);
  }

  @BeforeEach
  void setUp() {
    playwrightDefaults = new PlaywrightDefaults();
    page = playwrightDefaults.setUp();
  }

  @AfterEach
  void tearDown() {
    playwrightDefaults.tearDown();
  }

  @Test
  void assertTicketContent() {
    goToRealmAndCreateNewClient();

    // Trigger CAS authorization and login fully in the browser
    String casLoginUrl = keycloakContainer.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/cas/login?service=" + urlEncode(SERVICE_URL_1);
    page.close();
    page = playwrightDefaults.setUp();
    page.navigate(casLoginUrl);

    // we need to catch the redirect for the non running service URL.
    AtomicReference<String> ticketUrl = new AtomicReference<>();
    page.onRequest(request -> {
      String url = request.url();
      if (url.contains("ticket=")) {
        ticketUrl.set(url);
      }
    });
    fillKeycloakLoginForm(TEST_USERNAME, TEST_USER_PASSWORD);

    // Redirect after successful auth
    assertTrue(ticketUrl.get() != null && ticketUrl.get().contains("ticket="));
    String finalUrl = ticketUrl.get();
    String ticket = getQueryParam(finalUrl, "ticket");
    assertNotNull(ticket, "Ticket not present in final URL");

    // Call validate URL and assert UserData against TicketData.
    String validateUrl = keycloakContainer.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/cas/serviceValidate?service=" + urlEncode(SERVICE_URL_1) + "&ticket=" + urlEncode(ticket);
    page.close();
    page = playwrightDefaults.setUp();
    page.navigate(validateUrl);
    page.waitForSelector("text=<cas:authenticationSuccess>");
    assertOnGivenUserData(page.content(), TEST_USERNAME, TEST_USER_FIRST_NAME, TEST_USER_LAST_NAME);
  }

  private static String getQueryParam(String url, String name) {
    try {
      URI uri = URI.create(url);
      String query = uri.getRawQuery();
      if (query == null) return null;
      for (String pair : query.split("&")) {
        int idx = pair.indexOf('=');
        String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
        if (name.equals(key)) {
          return idx > 0 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : "";
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private static void assertOnGivenUserData(String xmlCasData, String username, String firstName, String lastName) {
    assertTrue(xmlCasData.contains(String.format("<cas:user>%s</cas:user>", username)));
    assertTrue(xmlCasData.contains(String.format("<cas:mail>%s</cas:mail>", username)));
    assertTrue(xmlCasData.contains(String.format("<cas:givenName>%s</cas:givenName>", firstName)));
    assertTrue(xmlCasData.contains(String.format("<cas:sn>%s</cas:sn>", lastName)));
    assertTrue(xmlCasData.contains(String.format("<cas:cn>%s %s</cas:cn>", firstName, lastName)));
  }

  private void fillKeycloakLoginForm(String username, String password) {
    page.waitForSelector("#username").isVisible();
    page.fill("#username", username);
    page.fill("#password", password);
    page.click("#kc-login");
  }

  private void goToRealmAndCreateNewClient() {
    // Login into Keycloak Admin Console
    String baseUrl = keycloakContainer.getAuthServerUrl();
    page.navigate(baseUrl + "/admin/");
    fillKeycloakLoginForm(ADMIN_USER, ADMIN_PASSWORD);
    page.waitForSelector("#nav-item-realms");
    // Keycloak UI has been loaded

    // Switching Realm
    page.click("#nav-item-realms");
    page.click(String.format("text=%s", REALM_NAME));
    // Create Client
    page.click("#nav-item-clients");
    page.click(String.format("text=%s", "Create client"));
    page.click("#protocol");
    // Wait for Dropdown to be visible
    String casButton = "button[role='option']:has-text('cas')";
    page.waitForSelector(casButton);
    page.locator(casButton).click();
    page.fill("#clientId", CAS_CLIENT_1);
    page.fill("#name", CAS_CLIENT_1.toUpperCase());
    page.click(String.format("text=%s", "Next"));
    // Capability Page
    page.click(String.format("text=%s", "Next"));
    page.getByTestId("redirectUris0").fill(SERVICE_URL_1);
    page.getByLabel("Valid post logout redirect URIs").fill(SERVICE_URL_1);
    page.getByLabel("Web origins").fill("*");
    page.click(String.format("text=%s", "Save"));
    // Client Creation done
    assertTrue(page.waitForSelector("#general-settings").isVisible());

    // verify the correct parameters were inserted into the UI
    var casClient = keycloakClient.realm(REALM_NAME).clients().findByClientId(CAS_CLIENT_1).get(0);
    assertTrue(casClient.getRedirectUris().contains(SERVICE_URL_1));
    assertTrue(casClient.getWebOrigins().contains("*"));
  }
}

