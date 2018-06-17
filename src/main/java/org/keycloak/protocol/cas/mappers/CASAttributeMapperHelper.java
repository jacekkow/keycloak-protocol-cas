package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;

import java.util.HashMap;
import java.util.Map;

public class CASAttributeMapperHelper {
    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String tokenClaimName, String claimType,
                                                        String mapperId) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(CASLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, claimType);
        mapper.setConfig(config);
        return mapper;
    }

}
