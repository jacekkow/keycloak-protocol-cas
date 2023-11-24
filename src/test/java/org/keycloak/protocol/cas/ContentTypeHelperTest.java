package org.keycloak.protocol.cas;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.specimpl.RequestImpl;
import org.junit.Test;
import org.keycloak.protocol.cas.utils.ContentTypeHelper;

import static org.junit.Assert.assertEquals;

public class ContentTypeHelperTest {
    @Test
    public void test() throws Exception {
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/").selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/?format=json").selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/?format=xml").selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/?format=JSON").selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/?format=XML").selectResponseType());
    }

    private ContentTypeHelper get(String uri) throws Exception {
        MockHttpRequest req = MockHttpRequest.get(uri);
        return new ContentTypeHelper(req.getUri());
    }
}
