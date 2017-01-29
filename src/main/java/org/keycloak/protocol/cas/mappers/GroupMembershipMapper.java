package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GroupMembershipMapper extends AbstractCASProtocolMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    private static final String FULL_PATH = "full.path";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        ProviderConfigProperty property1 = new ProviderConfigProperty();
        property1.setName(FULL_PATH);
        property1.setLabel("Full group path");
        property1.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property1.setDefaultValue("true");
        property1.setHelpText("Include full path to group i.e. /top/level1/level2, false will just specify the group name");
        configProperties.add(property1);
    }

    public static final String PROVIDER_ID = "cas-group-membership-mapper";


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Group Membership";
    }

    @Override
    public String getHelpText() {
        return "Map user group membership";
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        List<String> membership = new LinkedList<>();
        boolean fullPath = useFullPath(mappingModel);
        for (GroupModel group : userSession.getUser().getGroups()) {
            if (fullPath) {
                membership.add(ModelToRepresentation.buildGroupPath(group));
            } else {
                membership.add(group.getName());
            }
        }
        setPlainAttribute(attributes, mappingModel, membership);
    }

    public static boolean useFullPath(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(FULL_PATH));
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName,
                                             boolean consentRequired, String consentText, boolean fullPath) {
        ProtocolMapperModel mapper = CASAttributeMapperHelper.createClaimMapper(name, tokenClaimName,
                "String", consentRequired, consentText, PROVIDER_ID);
        mapper.getConfig().put(FULL_PATH, Boolean.toString(fullPath));
        return mapper;
    }
}
