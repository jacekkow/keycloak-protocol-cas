package org.keycloak.protocol.cas.endpoints;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.cas.mappers.CASAttributeMapper;
import org.keycloak.protocol.cas.representations.CasServiceResponse;
import org.keycloak.protocol.cas.utils.ContentTypeHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServiceValidateEndpoint extends ValidateEndpoint {
    @Context
    private Request restRequest;

    public ServiceValidateEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
    }

    @Override
    protected Response successResponse() {
        UserSessionModel userSession = clientSession.getUserSession();

        Set<ProtocolMapperModel> mappings = new ClientSessionCode(session, realm, clientSession).getRequestedProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Map<String, Object> attributes = new HashMap<>();
        for (ProtocolMapperModel mapping : mappings) {
            ProtocolMapper mapper = (ProtocolMapper) sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper instanceof CASAttributeMapper) {
                ((CASAttributeMapper) mapper).setAttribute(attributes, mapping, userSession);
            }
        }

        CasServiceResponse serviceResponse = ServiceResponseHelper.createSuccess(userSession.getUser().getUsername(), attributes);
        return prepare(Response.Status.OK, serviceResponse);
    }

    @Override
    protected Response errorResponse(ErrorResponseException e) {
        CasServiceResponse serviceResponse = ServiceResponseHelper.createFailure("CODE", "Description");
        return prepare(Response.Status.FORBIDDEN, serviceResponse);
    }

    private Response prepare(Response.Status status, CasServiceResponse serviceResponse) {
        MediaType responseMediaType = new ContentTypeHelper(request, restRequest, uriInfo).selectResponseType();
        return ServiceResponseHelper.createResponse(status, responseMediaType, serviceResponse);
    }
}
