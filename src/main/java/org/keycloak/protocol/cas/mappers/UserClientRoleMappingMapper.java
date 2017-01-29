package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.function.Predicate;

public class UserClientRoleMappingMapper extends AbstractUserRoleMappingMapper {

    public static final String PROVIDER_ID = "cas-usermodel-client-role-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {

        ProviderConfigProperty clientId = new ProviderConfigProperty();
        clientId.setName(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID);
        clientId.setLabel(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_LABEL);
        clientId.setHelpText(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_HELP_TEXT);
        clientId.setType(ProviderConfigProperty.CLIENT_LIST_TYPE);
        CONFIG_PROPERTIES.add(clientId);

        ProviderConfigProperty clientRolePrefix = new ProviderConfigProperty();
        clientRolePrefix.setName(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX);
        clientRolePrefix.setLabel(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_LABEL);
        clientRolePrefix.setHelpText(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT);
        clientRolePrefix.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(clientRolePrefix);

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
        return "User Client Role";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a user client role to a token claim.";
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        String clientId = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID);
        String rolePrefix = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX);

        setAttribute(attributes, mappingModel, userSession, getClientRoleFilter(clientId, userSession), rolePrefix);
    }

    private static Predicate<RoleModel> getClientRoleFilter(String clientId, UserSessionModel userSession) {
        if (clientId == null) {
            return RoleModel::isClientRole;
        }

        RealmModel clientRealm = userSession.getRealm();
        ClientModel client = clientRealm.getClientByClientId(clientId.trim());

        if (client == null) {
            return RoleModel::isClientRole;
        }

        ClientTemplateModel template = client.getClientTemplate();
        boolean useTemplateScope = template != null && client.useTemplateScope();
        boolean fullScopeAllowed = (useTemplateScope && template.isFullScopeAllowed()) || client.isFullScopeAllowed();

        Set<RoleModel> clientRoleMappings = client.getRoles();
        if (fullScopeAllowed) {
            return clientRoleMappings::contains;
        }

        Set<RoleModel> scopeMappings = new HashSet<>();

        if (useTemplateScope) {
            Set<RoleModel> templateScopeMappings = template.getScopeMappings();
            if (templateScopeMappings != null) {
                scopeMappings.addAll(templateScopeMappings);
            }
        }

        Set<RoleModel> clientScopeMappings = client.getScopeMappings();
        if (clientScopeMappings != null) {
            scopeMappings.addAll(clientScopeMappings);
        }

        return role -> clientRoleMappings.contains(role) && scopeMappings.contains(role);
    }

    public static ProtocolMapperModel create(String clientId, String clientRolePrefix,
                                             String name, String tokenClaimName) {
        ProtocolMapperModel mapper = CASAttributeMapperHelper.createClaimMapper(name, tokenClaimName,
                "String", true, name, PROVIDER_ID);
        mapper.getConfig().put(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID, clientId);
        mapper.getConfig().put(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX, clientRolePrefix);
        return mapper;
    }
}
