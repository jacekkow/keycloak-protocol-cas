package org.keycloak.protocol.cas.endpoints;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASValidationException;

import javax.ws.rs.GET;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class ValidateEndpoint extends AbstractValidateEndpoint {

    private static final String RESPONSE_OK = "yes\n";
    private static final String RESPONSE_FAILED = "no\n";

    public ValidateEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
    }

    @GET
    @NoCache
    public Response build() {
        MultivaluedMap<String, String> params = session.getContext().getUri().getQueryParameters();
        String service = params.getFirst(CASLoginProtocol.SERVICE_PARAM);
        String ticket = params.getFirst(CASLoginProtocol.TICKET_PARAM);
        boolean renew = params.containsKey(CASLoginProtocol.RENEW_PARAM);

        event.event(EventType.CODE_TO_TOKEN);

        try {
            checkSsl();
            checkRealm();
            checkClient(service);

            checkTicket(ticket, renew);

            event.success();
            return successResponse();
        } catch (CASValidationException e) {
            return errorResponse(e);
        }
    }

    protected Response successResponse() {
        StringBuilder sb = new StringBuilder(RESPONSE_OK);
        sb.append(clientSession.getUserSession().getUser().getUsername());
        sb.append("\n");
        return Response.ok(sb.toString()).type(MediaType.TEXT_PLAIN).build();
    }

    protected Response errorResponse(CASValidationException e) {
        return Response.status(e.getStatus()).entity(RESPONSE_FAILED).type(MediaType.TEXT_PLAIN).build();
    }

}
