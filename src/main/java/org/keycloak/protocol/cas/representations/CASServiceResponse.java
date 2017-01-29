package org.keycloak.protocol.cas.representations;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "serviceResponse")
public class CASServiceResponse {
    private CASServiceResponseAuthenticationFailure authenticationFailure;
    private CASServiceResponseAuthenticationSuccess authenticationSuccess;

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
}
