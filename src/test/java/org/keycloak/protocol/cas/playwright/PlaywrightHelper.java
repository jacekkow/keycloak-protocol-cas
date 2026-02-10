package org.keycloak.protocol.cas.playwright;

import com.microsoft.playwright.Page;
import org.keycloak.admin.client.Keycloak;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlaywrightHelper {

  static void fillKeycloakLoginForm(Page page, String username, String password) {
    page.waitForSelector("#username").isVisible();
    page.fill("#username", username);
    page.fill("#password", password);
    page.click("#kc-login");
  }

  static void goToRealmAndCreateNewClient(Page page,
                                          Keycloak keycloakClient,
                                          String authUrl,
                                          String realmName,
                                          String clientId,
                                          String adminUser,
                                          String adminPassword,
                                          String serviceUrl) {
    // Login into Keycloak Admin Console
    String baseUrl = authUrl;
    page.navigate(baseUrl + "/admin/");
    fillKeycloakLoginForm(page, adminUser, adminPassword);
    page.waitForSelector("#nav-item-realms");
    // Keycloak UI has been loaded

    // Switching Realm
    page.click("#nav-item-realms");
    page.click(String.format("text=%s", realmName));
    // Create Client
    page.click("#nav-item-clients");
    page.click(String.format("text=%s", "Create client"));
    page.click("#protocol");
    // Wait for Dropdown to be visible
    String casButton = "button[role='option']:has-text('cas')";
    page.waitForSelector(casButton);
    page.locator(casButton).click();
    page.fill("#clientId", clientId);
    page.fill("#name", clientId.toUpperCase());
    page.click(String.format("text=%s", "Next"));
    // Capability Page
    page.click(String.format("text=%s", "Next"));
    page.getByTestId("redirectUris0").fill(serviceUrl);
    page.getByLabel("Valid post logout redirect URIs").fill(serviceUrl);
    page.getByLabel("Web origins").fill("*");
    page.click(String.format("text=%s", "Save"));
    // Client Creation done
    assertTrue(page.waitForSelector("#general-settings").isVisible());

    // verify the correct parameters were inserted into the UI
    var casClient = keycloakClient.realm(realmName).clients().findByClientId(clientId).get(0);
    assertTrue(casClient.getRedirectUris().contains(serviceUrl));
    assertTrue(casClient.getWebOrigins().contains("*"));
  }

  static void assertOnGivenUserData(String xmlCasData, String username, String firstName, String lastName) {
    assertTrue(xmlCasData.contains(String.format("<cas:user>%s</cas:user>", username)));
    assertTrue(xmlCasData.contains(String.format("<cas:mail>%s</cas:mail>", username)));
    assertTrue(xmlCasData.contains(String.format("<cas:givenName>%s</cas:givenName>", firstName)));
    assertTrue(xmlCasData.contains(String.format("<cas:sn>%s</cas:sn>", lastName)));
    assertTrue(xmlCasData.contains(String.format("<cas:cn>%s %s</cas:cn>", firstName, lastName)));
  }
}
