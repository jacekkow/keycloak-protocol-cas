package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.cas.utils.ContentTypeHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;

import java.util.Map;

public class ServiceValidateEndpoint extends ValidateEndpoint {
    public ServiceValidateEndpoint(KeycloakSession session, RealmModel realm, EventBuilder event) {
        super(session, realm, event);
    }

    @Override
    protected Response successResponse() {
        UserSessionModel userSession = clientSession.getUserSession();
        Map<String, Object> attributes = getUserAttributes();
        CASServiceResponse serviceResponse = ServiceResponseHelper.createSuccess(userSession.getUser().getUsername(), attributes);
        return prepare(Response.Status.OK, serviceResponse);
    }

    @Override
    protected Response errorResponse(CASValidationException e) {
        CASServiceResponse serviceResponse = ServiceResponseHelper.createFailure(e.getError(), e.getErrorDescription());
        return prepare(e.getStatus(), serviceResponse);
    }

    private Response prepare(Response.Status status, CASServiceResponse serviceResponse) {
        MediaType responseMediaType = new ContentTypeHelper(session.getContext().getUri()).selectResponseType();
        return ServiceResponseHelper.createResponse(status, responseMediaType, serviceResponse);
    }
}
