package org.keycloak.protocol.cas;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.keycloak.protocol.cas.utils.LogoutHelper;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LogoutHelperTest {
    @Test
    public void testLogoutRequest() throws Exception {
        HttpEntity requestEntity = LogoutHelper.buildSingleLogoutRequest("ST-test");

        List<NameValuePair> parameters = URLEncodedUtils.parse(requestEntity).stream().filter(parameter -> "logoutRequest".equals(parameter.getName())).collect(Collectors.toList());
        assertEquals(1, parameters.size());

        String logoutRequest = parameters.get(0).getValue();
        Document doc = DocumentUtil.getDocument(logoutRequest);

        assertEquals("LogoutRequest", doc.getDocumentElement().getLocalName());
        assertEquals(JBossSAMLURIConstants.PROTOCOL_NSURI.get(), doc.getDocumentElement().getNamespaceURI());
        assertEquals("2.0", doc.getDocumentElement().getAttribute("Version"));
        assertFalse(doc.getDocumentElement().getAttribute("ID").isEmpty());
        assertFalse(doc.getDocumentElement().getAttribute("IssueInstant").isEmpty());

        Node nameID = doc.getDocumentElement().getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(), "NameID").item(0);
        assertFalse(nameID.getTextContent() == null || nameID.getTextContent().isEmpty());

        Node sessionIndex = doc.getDocumentElement().getElementsByTagNameNS(JBossSAMLURIConstants.PROTOCOL_NSURI.get(), "SessionIndex").item(0);
        assertEquals("ST-test", sessionIndex.getTextContent());
    }
}
