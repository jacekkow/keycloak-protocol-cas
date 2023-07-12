@XmlSchema(
        namespace = "http://www.yale.edu/tp/cas",
        xmlns = {
                @XmlNs(namespaceURI = "http://www.yale.edu/tp/cas", prefix = "cas")
        },
        elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED)
package org.keycloak.protocol.cas.representations;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlSchema;
