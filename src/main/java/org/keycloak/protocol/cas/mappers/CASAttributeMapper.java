package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;

import java.util.Map;

public interface CASAttributeMapper {
    void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, UserSessionModel userSession);
}
