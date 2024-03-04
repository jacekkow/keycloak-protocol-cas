package org.keycloak.protocol.cas;

import jakarta.ws.rs.core.*;
import org.junit.Test;
import org.keycloak.protocol.cas.utils.ContentTypeHelper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ContentTypeHelperTest {
    @Test
    public void test() {
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("").selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("json").selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("xml").selectResponseType());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, get("JSON").selectResponseType());
        assertEquals(MediaType.APPLICATION_XML_TYPE, get("XML").selectResponseType());
    }

    private ContentTypeHelper get(String format) {
        MultivaluedHashMap<String,String> queryParams = mock(MultivaluedHashMap.class);
        when(queryParams.getFirst(CASLoginProtocol.FORMAT_PARAM)).thenReturn(format);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);
        return new ContentTypeHelper(uriInfo);
    }
}
