package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserSessionNoteMapper extends AbstractUserAttributeMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_SESSION_NOTE);
        property.setLabel(ProtocolMapperUtils.USER_SESSION_MODEL_NOTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_SESSION_MODEL_NOTE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addJsonTypeConfig(configProperties);
    }

    public static final String PROVIDER_ID = "cas-usersessionmodel-note-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Session Note";
    }


    @Override
    public String getHelpText() {
        return "Map a custom user session note to a token claim.";
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                             KeycloakSession session, ClientSessionContext clientSessionCt) {
        String noteName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE);
        String noteValue = userSession.getNote(noteName);
        if (noteValue == null) return;
        setMappedAttribute(attributes, mappingModel, noteValue);
    }

    public static ProtocolMapperModel create(String name,
                                             String userSessionNote,
                                             String tokenClaimName, String jsonType) {
        ProtocolMapperModel mapper = CASAttributeMapperHelper.createClaimMapper(name, tokenClaimName,
                jsonType, PROVIDER_ID);
        mapper.getConfig().put(ProtocolMapperUtils.USER_SESSION_NOTE, userSessionNote);
        return mapper;
    }
}
