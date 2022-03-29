package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;

import java.util.Map;

public abstract class AbstractUserAttributeMapper extends AbstractCASProtocolMapper implements CASAttributeMapper {
    public static final String TOKEN_MAPPER_CATEGORY = "Token mapper";

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    protected void setMappedAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, Object attributeValue) {
        setPlainAttribute(attributes, mappingModel, OIDCAttributeMapperHelper.mapAttributeValue(mappingModel, attributeValue));
    }

    protected void setPlainAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, Object attributeValue) {
        String protocolClaim = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        if (protocolClaim == null || attributeValue == null) {
            return;
        }
        attributes.put(protocolClaim, attributeValue);
    }
}
