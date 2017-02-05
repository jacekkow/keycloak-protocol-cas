package org.keycloak.protocol.cas.representations;

import org.keycloak.protocol.cas.utils.AttributesMapAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
