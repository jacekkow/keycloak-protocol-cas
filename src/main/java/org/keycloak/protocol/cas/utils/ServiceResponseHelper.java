package org.keycloak.protocol.cas.utils;

import org.keycloak.protocol.cas.representations.CasServiceResponse;
import org.keycloak.protocol.cas.representations.CasServiceResponseAuthenticationFailure;
import org.keycloak.protocol.cas.representations.CasServiceResponseAuthenticationSuccess;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public final class ServiceResponseHelper {
    private ServiceResponseHelper() {
    }

    public static CasServiceResponse createSuccess(String username, Map<String, Object> attributes) {
        return createSuccess(username, attributes, null, null);
    }

    public static CasServiceResponse createSuccess(String username, Map<String, Object> attributes,
                                                   String proxyGrantingTicket, List<String> proxies) {
        CasServiceResponse response = new CasServiceResponse();
        CasServiceResponseAuthenticationSuccess success = new CasServiceResponseAuthenticationSuccess();
        success.setUser(username);
        success.setProxies(proxies);
        success.setProxyGrantingTicket(proxyGrantingTicket);
        success.setAttributes(attributes);

        response.setAuthenticationSuccess(success);

        return response;
    }

    public static CasServiceResponse createFailure(String errorCode, String errorDescription) {
        CasServiceResponse response = new CasServiceResponse();
        CasServiceResponseAuthenticationFailure failure = new CasServiceResponseAuthenticationFailure();
        failure.setCode(errorCode);
        failure.setDescription(errorDescription);
        response.setAuthenticationFailure(failure);

        return response;
    }

    public static Response createResponse(Response.Status status, MediaType mediaType, CasServiceResponse serviceResponse) {
        Response.ResponseBuilder builder = Response.status(status)
                .header(HttpHeaders.CONTENT_TYPE, mediaType.withCharset("utf-8"));
        if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            return builder.entity(ServiceResponseMarshaller.marshalJson(serviceResponse)).build();
        } else {
            return builder.entity(ServiceResponseMarshaller.marshalXml(serviceResponse)).build();
        }
    }
}
