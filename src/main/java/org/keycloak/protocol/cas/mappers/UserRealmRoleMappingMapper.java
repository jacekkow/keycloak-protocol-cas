package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.RoleResolveUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRealmRoleMappingMapper extends AbstractUserRoleMappingMapper {
    public static final String PROVIDER_ID = "cas-usermodel-realm-role-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {

        ProviderConfigProperty realmRolePrefix = new ProviderConfigProperty();
        realmRolePrefix.setName(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX);
        realmRolePrefix.setLabel(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX_LABEL);
        realmRolePrefix.setHelpText(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT);
        realmRolePrefix.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(realmRolePrefix);

        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Realm Role";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a user realm role to a token claim.";
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                             KeycloakSession session, ClientSessionContext clientSessionCtx) {
        String rolePrefix = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX);

        AccessToken.Access access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, false);
        if (access == null) {
            return;
        }

        setAttribute(attributes, mappingModel, access.getRoles(), rolePrefix);
    }

    public static ProtocolMapperModel create(String realmRolePrefix, String name, String tokenClaimName) {
        ProtocolMapperModel mapper = CASAttributeMapperHelper.createClaimMapper(name, tokenClaimName,
                "String", PROVIDER_ID);
        mapper.getConfig().put(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX, realmRolePrefix);
        return mapper;
    }
}
