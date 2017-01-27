package org.keycloak.protocol.cas.representations;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "serviceResponse")
public class CasServiceResponse {
    private CasServiceResponseAuthenticationFailure authenticationFailure;
    private CasServiceResponseAuthenticationSuccess authenticationSuccess;

    public CasServiceResponseAuthenticationFailure getAuthenticationFailure() {
        return this.authenticationFailure;
    }

    public void setAuthenticationFailure(final CasServiceResponseAuthenticationFailure authenticationFailure) {
        this.authenticationFailure = authenticationFailure;
    }

    public CasServiceResponseAuthenticationSuccess getAuthenticationSuccess() {
        return this.authenticationSuccess;
    }

    public void setAuthenticationSuccess(final CasServiceResponseAuthenticationSuccess authenticationSuccess) {
        this.authenticationSuccess = authenticationSuccess;
    }
}
