package org.keycloak.protocol.cas.utils;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.protocol.cas.representations.CASErrorCode;

public class CASValidationException extends WebApplicationException {
    private static final long serialVersionUID = 4929825917145240776L;
    private final CASErrorCode error;
    private final String errorDescription;
    private final Response.Status status;

    public CASValidationException(CASErrorCode error, String errorDescription, Response.Status status) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.status = status;
    }

    public CASErrorCode getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public Response.Status getStatus() {
        return status;
    }
}
