package org.keycloak.protocol.cas.utils;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.*;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.protocol.cas.CASLoginProtocol;

public class ContentTypeHelper {
    private final HttpRequest request;
    private final Request restRequest;
    private final UriInfo uriInfo;

    public ContentTypeHelper(HttpRequest request, Request restRequest, UriInfo uriInfo) {
        this.request = request;
        this.restRequest = restRequest;
        this.uriInfo = uriInfo;
    }

    public MediaType selectResponseType() {
        String format = uriInfo.getQueryParameters().getFirst(CASLoginProtocol.FORMAT_PARAM);
        if (format != null && !format.isEmpty()) {
            //if parameter is set, it overrides all header values (see spec section 2.5.1)
            request.getMutableHeaders().putSingle(HttpHeaders.ACCEPT, "application/" + format.toLowerCase());
        }
        try {
            Variant variant = restRequest.selectVariant(Variant.mediaTypes(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE).build());
            return variant == null ? MediaType.APPLICATION_XML_TYPE : variant.getMediaType();
        } catch (BadRequestException e) {
            //the default Accept header set by java.net.HttpURLConnection is invalid (cf. RESTEASY-960)
            return MediaType.APPLICATION_XML_TYPE;
        }
    }
}
