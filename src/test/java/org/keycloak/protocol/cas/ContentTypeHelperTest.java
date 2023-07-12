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
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/", null).selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/?format=json", null).selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/?format=xml", null).selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/?format=JSON", null).selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/?format=XML", null).selectResponseType());

        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/", MediaType.APPLICATION_XML).selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/?format=json", MediaType.APPLICATION_XML).selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/?format=xml", MediaType.APPLICATION_XML).selectResponseType());

        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/", MediaType.APPLICATION_JSON).selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("http://example.com/?format=json", MediaType.APPLICATION_JSON).selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/?format=xml", MediaType.APPLICATION_JSON).selectResponseType());

        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/", MediaType.TEXT_PLAIN).selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("http://example.com/", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2").selectResponseType());
    }

    private ContentTypeHelper get(String uri, String acceptHeader) throws Exception {
        MockHttpRequest req = MockHttpRequest.get(uri);
        MockHttpResponse res = new MockHttpResponse();
        RequestImpl restReq = new RequestImpl(req, res);

        if (acceptHeader != null) {
            req = req.header(HttpHeaders.ACCEPT, acceptHeader);
        }

        return new ContentTypeHelper(req, restReq, req.getUri());
    }
}
