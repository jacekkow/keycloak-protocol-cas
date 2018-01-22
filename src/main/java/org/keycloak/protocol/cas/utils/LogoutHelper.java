package org.keycloak.protocol.cas.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class LogoutHelper {
    //although it looks alike, the CAS SLO protocol has nothing to do with SAML; so we build the format
    //required by the spec manually
    private static final String TEMPLATE = "<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"$ID\" Version=\"2.0\" IssueInstant=\"$ISSUE_INSTANT\">\n" +
            "  <saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">@NOT_USED@</saml:NameID>\n" +
            "  <samlp:SessionIndex>$SESSION_IDENTIFIER</samlp:SessionIndex>\n" +
            "</samlp:LogoutRequest>";

    public static HttpEntity buildSingleLogoutRequest(String serviceTicket) {
        String id = "ID_" + UUID.randomUUID().toString();
        String issueInstant = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss").format(new Date());
        String document = TEMPLATE.replace("$ID", id).replace("$ISSUE_INSTANT", issueInstant)
                .replace("$SESSION_IDENTIFIER", serviceTicket);
        return new StringEntity(document, ContentType.APPLICATION_XML.withCharset(StandardCharsets.UTF_8));
    }

    public static void postWithRedirect(KeycloakSession session, String url, HttpEntity postBody) throws IOException {
        HttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        for (int i = 0; i < 2; i++) { // follow redirects once
            HttpPost post = new HttpPost(url);
            post.setEntity(postBody);
            HttpResponse response = httpClient.execute(post);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 302 && !url.endsWith("/")) {
                    String redirect = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                    String withSlash = url + "/";
                    if (withSlash.equals(redirect)) {
                        url = withSlash;
                        continue;
                    }
                }
            } finally {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream is = entity.getContent();
                    if (is != null)
                        is.close();
                }

            }
            break;
        }
    }
}
