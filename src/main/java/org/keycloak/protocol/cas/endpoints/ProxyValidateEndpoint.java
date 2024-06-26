package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.cas.utils.ContentTypeHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;

public class ProxyValidateEndpoint extends AbstractValidateEndpoint {

    public ProxyValidateEndpoint(KeycloakSession session,RealmModel realm, EventBuilder event) {
        super(session, realm, event);
    }

    @GET
    @NoCache
    public Response build() {
        MultivaluedMap<String, String> params = session.getContext().getUri().getQueryParameters();
        String ticket = params.getFirst(CASLoginProtocol.TICKET_PARAM);
        String pgtUrl = params.getFirst(CASLoginProtocol.PGTURL_PARAM);
        boolean renew = params.containsKey(CASLoginProtocol.RENEW_PARAM);

        event.event(EventType.CODE_TO_TOKEN);

        try {
            String prefix = ticket.startsWith(CASLoginProtocol.PROXY_TICKET_PREFIX)? CASLoginProtocol.PROXY_TICKET_PREFIX:(
                ticket.startsWith(CASLoginProtocol.SERVICE_TICKET_PREFIX)? CASLoginProtocol.SERVICE_TICKET_PREFIX : null
            );

            if (prefix == null) {
                event.error(Errors.INVALID_CODE);
                throw new CASValidationException(CASErrorCode.INVALID_TICKET_SPEC, "Malformed service ticket", Response.Status.BAD_REQUEST);
            }

            checkSsl();
            checkRealm();
            checkTicket(ticket, prefix, renew);
            if (pgtUrl != null) createProxyGrant(pgtUrl);
            event.success();
            return successResponse();
        } catch (CASValidationException e) {
            return errorResponse(e);
        }
    }

    protected Response successResponse() {
        UserSessionModel userSession = clientSession.getUserSession();
        Map<String, Object> attributes = getUserAttributes();
        CASServiceResponse serviceResponse = ServiceResponseHelper.createSuccess(userSession.getUser().getUsername(),attributes);
        return prepare(Response.Status.OK, serviceResponse);
    }

    protected Response errorResponse(CASValidationException e) {
        CASServiceResponse serviceResponse = ServiceResponseHelper.createFailure(e.getError(), e.getErrorDescription());
        return prepare(e.getStatus(), serviceResponse);
    }

    private Response prepare(Response.Status status, CASServiceResponse serviceResponse) {
        MediaType responseMediaType = new ContentTypeHelper(session.getContext().getUri()).selectResponseType();
        return ServiceResponseHelper.createResponse(status, responseMediaType, serviceResponse);
    }
}
