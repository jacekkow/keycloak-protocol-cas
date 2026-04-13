package org.keycloak.protocol.cas.playwright;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Nested;
import org.keycloak.admin.client.Keycloak;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class CasLoginIT {
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASSWORD = "keycloak";

  @Nested
  class PlaywrightTestcontainers extends BaseCasLoginIT {
    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");
    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
        .withProviderClassesFrom("target/classes")
        .withAdminUsername(ADMIN_USER)
        .withAdminPassword(ADMIN_PASSWORD)
        .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASSWORD);

    @Override
    protected Keycloak keycloakClient() {
      return keycloakContainer.getKeycloakAdminClient();
    }

    @Override
    protected String getAuthServerUrl() {
      return keycloakContainer.getAuthServerUrl();
    }

    @Override
    protected String getAdminUser() {
      return ADMIN_USER;
    }

    @Override
    protected String getAdminPassword() {
      return ADMIN_PASSWORD;
    }
  }

}
