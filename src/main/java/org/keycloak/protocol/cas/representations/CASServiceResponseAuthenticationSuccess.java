package org.keycloak.protocol.cas.representations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.keycloak.protocol.cas.utils.AttributesMapAdapter;

import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class CASServiceResponseAuthenticationSuccess {
    private String user;
    @XmlJavaTypeAdapter(AttributesMapAdapter.class)
    private Map<String, Object> attributes;
    private String proxyGrantingTicket;
    @XmlElementWrapper
    @XmlElement(name="proxy")
    private List<String> proxies;

    public String getUser() {
        return this.user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(final Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getProxyGrantingTicket() {
        return this.proxyGrantingTicket;
    }

    public void setProxyGrantingTicket(final String proxyGrantingTicket) {
        this.proxyGrantingTicket = proxyGrantingTicket;
    }

    public List<String> getProxies() {
        return this.proxies;
    }

    public void setProxies(final List<String> proxies) {
        this.proxies = proxies;
    }
}
