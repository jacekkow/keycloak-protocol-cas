package org.keycloak.protocol.cas.representations;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "serviceResponse")
public class CASServiceResponse {
    private CASServiceResponseAuthenticationFailure authenticationFailure;
    private CASServiceResponseAuthenticationSuccess authenticationSuccess;
    private CASServiceResponseProxySuccess proxySuccess;
    private CASServiceResponseProxyFailure proxyFailure;

    public CASServiceResponseAuthenticationFailure getAuthenticationFailure() {
        return this.authenticationFailure;
    }

    public void setAuthenticationFailure(final CASServiceResponseAuthenticationFailure authenticationFailure) {
        this.authenticationFailure = authenticationFailure;
    }

    public CASServiceResponseAuthenticationSuccess getAuthenticationSuccess() {
        return this.authenticationSuccess;
    }

    public void setAuthenticationSuccess(final CASServiceResponseAuthenticationSuccess authenticationSuccess) {
        this.authenticationSuccess = authenticationSuccess;
    }

    public CASServiceResponseProxySuccess getProxySuccess() {
        return this.proxySuccess;
    }

    public void setProxySuccess(final CASServiceResponseProxySuccess proxySuccess) {
        this.proxySuccess = proxySuccess;
    }

    public CASServiceResponseProxyFailure getProxyFailure() {
        return this.proxyFailure;
    }

    public void setProxyFailure(final CASServiceResponseProxyFailure proxyFailure) {
        this.proxyFailure = proxyFailure;
    }
}
