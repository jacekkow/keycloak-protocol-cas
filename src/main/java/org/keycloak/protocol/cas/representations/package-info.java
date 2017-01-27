@XmlSchema(
        namespace = "http://www.yale.edu/tp/cas",
        xmlns = {
                @XmlNs(namespaceURI = "http://www.yale.edu/tp/cas", prefix = "cas")
        },
        elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)
package org.keycloak.protocol.cas.representations;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;