package org.keycloak.protocol.cas.utils;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RedirectUtils {

    private static final Set<String> FORBIDDEN_OIDC_PARAMS = Set.of(
            "code", "id_token", "access_token", "token_type", "expires_in", "state",
            "issuer", "error", "error_description", "session_state", "response",
            "kc_action", "kc_action_status"
    );

    public static String verifyRedirectUri(KeycloakSession session, String redirectUri, ClientModel client) {
        if (redirectUri == null) {
            return null;
        }

        String sanitizedUri = sanitizeRedirectUri(redirectUri);
        String verified = org.keycloak.protocol.oidc.utils.RedirectUtils.verifyRedirectUri(session, sanitizedUri, client);
        if (verified == null) {
            return null;
        }

        return restoreRedirectUri(redirectUri, verified, sanitizedUri);
    }

    private static String sanitizeRedirectUri(String redirectUri) {
        try {
            URI uri = URI.create(redirectUri);
            String query = uri.getRawQuery();
            if (query == null || query.isEmpty()) {
                return redirectUri;
            }
            String[] params = query.split("&");
            String filteredQuery = Arrays.stream(params)
                    .filter(param -> {
                        int eq = param.indexOf('=');
                        String name = eq >= 0 ? param.substring(0, eq) : param;
                        return !FORBIDDEN_OIDC_PARAMS.contains(name);
                    })
                    .collect(Collectors.joining("&"));
            if (filteredQuery.equals(query)) {
                return redirectUri;
            }

            StringBuilder sb = new StringBuilder();
            if (uri.getScheme() != null) {
                sb.append(uri.getScheme()).append("://");
            }
            if (uri.getRawAuthority() != null) {
                sb.append(uri.getRawAuthority());
            }
            if (uri.getRawPath() != null) {
                sb.append(uri.getRawPath());
            }
            if (!filteredQuery.isEmpty()) {
                sb.append("?").append(filteredQuery);
            }
            if (uri.getRawFragment() != null) {
                sb.append("#").append(uri.getRawFragment());
            }
            return sb.toString();
        } catch (Exception e) {
            return redirectUri;
        }
    }

    private static String restoreRedirectUri(String redirectUri, String verified, String sanitizedUri) {
        if (sanitizedUri.equals(redirectUri)) {
            return verified;
        }

        try {
            URI verifiedUri = URI.create(verified);
            URI originalUri = URI.create(redirectUri);

            StringBuilder sb = new StringBuilder();
            if (verifiedUri.getScheme() != null) {
                sb.append(verifiedUri.getScheme()).append("://");
            }
            if (verifiedUri.getRawAuthority() != null) {
                sb.append(verifiedUri.getRawAuthority());
            }
            if (verifiedUri.getRawPath() != null) {
                sb.append(verifiedUri.getRawPath());
            }
            if (originalUri.getRawQuery() != null && !originalUri.getRawQuery().isEmpty()) {
                sb.append("?").append(originalUri.getRawQuery());
            }
            if (originalUri.getRawFragment() != null) {
                sb.append("#").append(originalUri.getRawFragment());
            }
            return sb.toString();
        } catch (Exception e) {
            return verified;
        }
    }
}
