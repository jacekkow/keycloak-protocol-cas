package org.keycloak.protocol.cas;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.cas.mappers.FullNameMapper;
import org.keycloak.protocol.cas.mappers.UserAttributeMapper;
import org.keycloak.protocol.cas.mappers.UserPropertyMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.JSON_TYPE;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;

public class CASLoginProtocolFactory extends AbstractLoginProtocolFactory {
    private static final Logger logger = Logger.getLogger(CASLoginProtocolFactory.class);

    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email verified";
    public static final String GIVEN_NAME = "given name";
    public static final String FAMILY_NAME = "family name";
    public static final String FULL_NAME = "full name";
    public static final String LOCALE = "locale";

    public static final String EMAIL_CONSENT_TEXT = "${email}";
    public static final String EMAIL_VERIFIED_CONSENT_TEXT = "${emailVerified}";
    public static final String GIVEN_NAME_CONSENT_TEXT = "${givenName}";
    public static final String FAMILY_NAME_CONSENT_TEXT = "${familyName}";
    public static final String FULL_NAME_CONSENT_TEXT = "${fullName}";
    public static final String LOCALE_CONSENT_TEXT = "${locale}";

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new CASLoginProtocol().setSession(session);
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        ProtocolMapperModel model;

        model = UserPropertyMapper.create(EMAIL, "email", "mail", "String",
                true, EMAIL_CONSENT_TEXT);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.create(GIVEN_NAME, "firstName", "givenName", "String",
                true, GIVEN_NAME_CONSENT_TEXT);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.create(FAMILY_NAME, "lastName", "sn", "String",
                true, FAMILY_NAME_CONSENT_TEXT);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.create(EMAIL_VERIFIED,
                "emailVerified",
                "emailVerified", "boolean",
                false, EMAIL_VERIFIED_CONSENT_TEXT);
        builtins.add(model);
        model = UserAttributeMapper.create(LOCALE,
                "locale",
                "locale", "String",
                false, LOCALE_CONSENT_TEXT,
                false);
        builtins.add(model);

        model = FullNameMapper.create(FULL_NAME, "cn",
                true, FULL_NAME_CONSENT_TEXT);
        builtins.add(model);
        defaultBuiltins.add(model);
    }

    @Override
    protected void addDefaults(ClientModel client) {
        for (ProtocolMapperModel model : defaultBuiltins) client.addProtocolMapper(model);
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event) {
        return new CASLoginProtocolService(realm, event);
    }

    @Override
    public String getId() {
        return CASLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        if (rep.getRootUrl() != null && (rep.getRedirectUris() == null || rep.getRedirectUris().isEmpty())) {
            String root = rep.getRootUrl();
            if (root.endsWith("/")) root = root + "*";
            else root = root + "/*";
            newClient.addRedirectUri(root);
        }

        if (rep.getAdminUrl() == null && rep.getRootUrl() != null) {
            newClient.setManagementUrl(rep.getRootUrl());
        }
    }

    @Override
    public void setupTemplateDefaults(ClientTemplateRepresentation clientRep, ClientTemplateModel newClient) {

    }
}
