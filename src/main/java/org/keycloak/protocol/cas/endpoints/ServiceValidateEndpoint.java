package org.keycloak.protocol.cas.endpoints;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

public class ServiceValidateEndpoint extends ValidateEndpoint {
    public ServiceValidateEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
    }

    @Override
    protected Response successResponse() {
        UserSessionModel userSession = clientSession.getUserSession();

        Set<ProtocolMapperModel> mappings = new ClientSessionCode(session, realm, clientSession).getRequestedProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {
            ProtocolMapper mapper = (ProtocolMapper) sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
        }

        return Response.ok()
                .header(HttpHeaders.CONTENT_TYPE, (jsonFormat() ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE).withCharset("utf-8"))
                .entity("<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n" +
                        "    <cas:authenticationSuccess>\n" +
                        "        <cas:user>" + userSession.getUser().getUsername() + "</cas:user>\n" +
                        "        <cas:attributes>\n" +
                        "        </cas:attributes>\n" +
                        "    </cas:authenticationSuccess>\n" +
                        "</cas:serviceResponse>")
                .build();
    }

    @Override
    protected Response errorResponse(ErrorResponseException e) {
        return super.errorResponse(e);
    }

    private boolean jsonFormat() {
        return "json".equalsIgnoreCase(uriInfo.getQueryParameters().getFirst(CASLoginProtocol.FORMAT_PARAM));
    }
}
