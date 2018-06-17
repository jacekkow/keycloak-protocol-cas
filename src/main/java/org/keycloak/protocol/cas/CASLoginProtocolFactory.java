package org.keycloak.protocol.cas;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.cas.mappers.FullNameMapper;
import org.keycloak.protocol.cas.mappers.UserAttributeMapper;
import org.keycloak.protocol.cas.mappers.UserPropertyMapper;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    static Map<String, ProtocolMapperModel> builtins = new HashMap<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        ProtocolMapperModel model;

        model = UserPropertyMapper.create(EMAIL, "email", "mail", "String");
        builtins.put(EMAIL, model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.create(GIVEN_NAME, "firstName", "givenName", "String");
        builtins.put(GIVEN_NAME, model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.create(FAMILY_NAME, "lastName", "sn", "String");
        builtins.put(FAMILY_NAME, model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.create(EMAIL_VERIFIED,
                "emailVerified",
                "emailVerified", "boolean");
        builtins.put(EMAIL_VERIFIED, model);
        model = UserAttributeMapper.create(LOCALE,
                "locale",
                "locale", "String",
                false);
        builtins.put(LOCALE, model);

        model = FullNameMapper.create(FULL_NAME, "cn");
        builtins.put(FULL_NAME, model);
        defaultBuiltins.add(model);
    }

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
        // no-op
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
}
