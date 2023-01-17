package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapper;

public interface CASUsernameMapper extends ProtocolMapper {

    String getMappedUsername(ProtocolMapperModel mappingModel, KeycloakSession session,
                             UserSessionModel userSession, AuthenticatedClientSessionModel clientSession);
    
}
