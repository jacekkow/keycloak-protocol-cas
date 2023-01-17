package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FullNameMapper extends AbstractUserAttributeMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
    }

    public static final String PROVIDER_ID = "cas-full-name-mapper";


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
        return "User's full name";
    }

    @Override
    public String getHelpText() {
        return "Maps the user's first and last name to the OpenID Connect 'name' claim. Format is <first> + ' ' + <last>";
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                             KeycloakSession session, ClientSessionContext clientSessionCt) {
        UserModel user = userSession.getUser();
        String first = user.getFirstName() == null ? "" : user.getFirstName() + " ";
        String last = user.getLastName() == null ? "" : user.getLastName();
        setMappedAttribute(attributes, mappingModel, first + last);
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName) {
        return CASAttributeMapperHelper.createClaimMapper(name, tokenClaimName,
                "String", PROVIDER_ID);
    }
}
