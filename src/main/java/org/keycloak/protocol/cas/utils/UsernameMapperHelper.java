package org.keycloak.protocol.cas.utils;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.cas.mappers.CASUsernameMapper;
import org.keycloak.services.util.DefaultClientSessionContext;

import java.util.Map;

public class UsernameMapperHelper {
    public static String getMappedUsername(KeycloakSession session, AuthenticatedClientSessionModel clientSession) {
        // CAS protocol does not support scopes, so pass null scopeParam
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, null, session);
        UserSessionModel userSession = clientSession.getUserSession();


        Map.Entry<ProtocolMapperModel, ProtocolMapper> mapperPair = ProtocolMapperUtils.getSortedProtocolMappers(session,clientSessionCtx)
                .filter(e -> e.getValue() instanceof CASUsernameMapper)
                .findFirst()
                .orElse(null);

        String mappedUsername = userSession.getUser().getUsername();

        if(mapperPair != null) {
            ProtocolMapperModel mapping = mapperPair.getKey();
            CASUsernameMapper casUsernameMapper = (CASUsernameMapper) mapperPair.getValue();
            mappedUsername = casUsernameMapper.getMappedUsername(mapping, session, userSession, clientSession);
        }
        return mappedUsername;
    }
}
