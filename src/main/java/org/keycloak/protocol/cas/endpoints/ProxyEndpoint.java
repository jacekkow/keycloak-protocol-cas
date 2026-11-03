package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.cas.utils.RedirectUtils;
import org.keycloak.protocol.cas.utils.ContentTypeHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;

public class ProxyEndpoint extends AbstractValidateEndpoint {

    public ProxyEndpoint(KeycloakSession session, RealmModel realm, EventBuilder event) {
        super(session, realm, event);
    }

    @GET
    @NoCache
    public Response build() {
        MultivaluedMap<String, String> params = session.getContext().getUri().getQueryParameters();
        String targetService = params.getFirst(CASLoginProtocol.TARGET_SERVICE_PARAM);
        String pgt = params.getFirst(CASLoginProtocol.PGT_PARAM);

        event.event(EventType.CODE_TO_TOKEN);

        try {
            checkSsl();
            checkRealm();
            checkTicket(pgt, CASLoginProtocol.PROXY_GRANTING_TICKET_PREFIX, false);
            String verifiedTargetService = checkTargetService(targetService);
            event.success();
            return successResponse(getPT(this.session, clientSession, verifiedTargetService));
        } catch (CASValidationException e) {
            return errorResponse(e);
        }
    }

    private String checkTargetService(String targetService) {
        if (targetService == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new CASValidationException(CASErrorCode.INVALID_REQUEST, "Missing parameter: " + CASLoginProtocol.TARGET_SERVICE_PARAM, Response.Status.BAD_REQUEST);
        }

        ClientModel targetClient = findClient(targetService);
        if (targetClient == null || !targetClient.isEnabled()) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new CASValidationException(CASErrorCode.INVALID_SERVICE, "Target service not allowed", Response.Status.BAD_REQUEST);
        }

        return RedirectUtils.verifyRedirectUri(session, targetService, targetClient);
    }

    protected Response successResponse(String pt) {
        CASServiceResponse serviceResponse = ServiceResponseHelper.createProxySuccess(pt);
        return prepare(Response.Status.OK, serviceResponse);
    }

    protected Response errorResponse(CASValidationException e) {
        CASServiceResponse serviceResponse = ServiceResponseHelper.createProxyFailure(e.getError(), e.getErrorDescription());
        return prepare(e.getStatus(), serviceResponse);
    }

    private Response prepare(Response.Status status, CASServiceResponse serviceResponse) {
        MediaType responseMediaType = new ContentTypeHelper(session.getContext().getUri()).selectResponseType();
        return ServiceResponseHelper.createResponse(status, responseMediaType, serviceResponse);
    }
}
