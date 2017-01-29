package org.keycloak.protocol.cas.utils;

import org.keycloak.protocol.cas.representations.CASErrorCode;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class CASValidationException extends WebApplicationException {
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
