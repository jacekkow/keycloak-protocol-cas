package org.keycloak.protocol.cas.representations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;


@XmlAccessorType(XmlAccessType.FIELD)
public class CASServiceResponseProxySuccess {
    private String proxyTicket;

    public String getProxyTicket() {
        return this.proxyTicket;
    }

    public void setProxyTicket(final String proxyTicket) {
        this.proxyTicket = proxyTicket;
    }
}
