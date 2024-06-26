package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.CASValidationException;
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
            event.success();
            return successResponse(getPT(this.session, clientSession, targetService));
        } catch (CASValidationException e) {
            return errorResponse(e);
        }
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
