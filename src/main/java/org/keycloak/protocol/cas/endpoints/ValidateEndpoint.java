package org.keycloak.protocol.cas.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASValidationException;

public class ValidateEndpoint extends AbstractValidateEndpoint {

    private static final String RESPONSE_OK = "yes\n";
    private static final String RESPONSE_FAILED = "no\n";

    public ValidateEndpoint(KeycloakSession session, RealmModel realm, EventBuilder event) {
        super(session, realm, event);
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
