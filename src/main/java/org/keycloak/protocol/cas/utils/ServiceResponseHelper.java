package org.keycloak.protocol.cas.utils;

import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.representations.CASServiceResponseAuthenticationFailure;
import org.keycloak.protocol.cas.representations.CASServiceResponseAuthenticationSuccess;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public final class ServiceResponseHelper {
    private ServiceResponseHelper() {
    }

    public static CASServiceResponse createSuccess(String username, Map<String, Object> attributes) {
        return createSuccess(username, attributes, null, null);
    }

    public static CASServiceResponse createSuccess(String username, Map<String, Object> attributes,
                                                   String proxyGrantingTicket, List<String> proxies) {
        CASServiceResponse response = new CASServiceResponse();
        CASServiceResponseAuthenticationSuccess success = new CASServiceResponseAuthenticationSuccess();
        success.setUser(username);
        success.setProxies(proxies);
        success.setProxyGrantingTicket(proxyGrantingTicket);
        success.setAttributes(attributes);

        response.setAuthenticationSuccess(success);

        return response;
    }

    public static CASServiceResponse createFailure(CASErrorCode errorCode, String errorDescription) {
        CASServiceResponse response = new CASServiceResponse();
        CASServiceResponseAuthenticationFailure failure = new CASServiceResponseAuthenticationFailure();
        failure.setCode(errorCode == null ? CASErrorCode.INTERNAL_ERROR.name() : errorCode.name());
        failure.setDescription(errorDescription);
        response.setAuthenticationFailure(failure);

        return response;
    }

    public static Response createResponse(Response.Status status, MediaType mediaType, CASServiceResponse serviceResponse) {
        Response.ResponseBuilder builder = Response.status(status)
                .header(HttpHeaders.CONTENT_TYPE, mediaType.withCharset("utf-8"));
        if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            return builder.entity(ServiceResponseMarshaller.marshalJson(serviceResponse)).build();
        } else {
            return builder.entity(ServiceResponseMarshaller.marshalXml(serviceResponse)).build();
        }
    }
}
