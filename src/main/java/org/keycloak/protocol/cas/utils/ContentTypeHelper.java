package org.keycloak.protocol.cas.utils;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.protocol.cas.CASLoginProtocol;

import javax.ws.rs.core.*;

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
            request.getMutableHeaders().add(HttpHeaders.ACCEPT, "application/" + format);
        }
        Variant variant = restRequest.selectVariant(Variant.mediaTypes(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE).build());
        return variant == null ? MediaType.APPLICATION_XML_TYPE : variant.getMediaType();
    }
}
