package org.keycloak.protocol.cas.utils;

import jakarta.ws.rs.core.*;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.representations.CASErrorCode;

public class ContentTypeHelper {
    private final UriInfo uriInfo;

    public ContentTypeHelper(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public MediaType selectResponseType() {
        String format = uriInfo.getQueryParameters().getFirst(CASLoginProtocol.FORMAT_PARAM);
        if (format != null && !format.isEmpty()) {
            if (format.equalsIgnoreCase("json")) {
                return MediaType.APPLICATION_JSON_TYPE;
            } else if (format.equalsIgnoreCase("xml")) {
                return MediaType.APPLICATION_XML_TYPE;
            } else {
                throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Unsupported value of parameter " + CASLoginProtocol.FORMAT_PARAM, Response.Status.BAD_REQUEST);
            }
        }
        return MediaType.APPLICATION_XML_TYPE;
    }
}
