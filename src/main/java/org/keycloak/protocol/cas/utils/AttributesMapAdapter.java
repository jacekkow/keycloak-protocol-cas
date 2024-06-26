package org.keycloak.protocol.cas.utils;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.keycloak.protocol.cas.representations.CASServiceResponse;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Transforms the attribute map of the AuthenticationSuccess object (which can contain either simple values or
 * lists) to a flat list of XML nodes, where the key is the node name.<br>
 * Lists output multiple XML nodes with the same name.
 */
public final class AttributesMapAdapter extends XmlAdapter<AttributesMapAdapter.AttributeWrapperType, Map<String, Object>> {
    @Override
    public AttributeWrapperType marshal(Map<String, Object> v) throws Exception {
        return new AttributeWrapperType(v);
    }

    @Override
    public Map<String, Object> unmarshal(AttributeWrapperType v) throws Exception {
        throw new IllegalStateException("not implemented");
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static class AttributeWrapperType {
        @XmlAnyElement
        private final List<JAXBElement<String>> elements;

        AttributeWrapperType(Map<String, Object> attributes) {
            this.elements = new ArrayList<>();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (entry.getValue() instanceof Collection) {
                    for (Object item : ((Collection<?>) entry.getValue())) {
                        addElement(entry.getKey(), item);
                    }
                } else {
                    addElement(entry.getKey(), entry.getValue());
                }
            }
        }

        private void addElement(String name, Object value) {
            if (value != null) {
                String namespace = CASServiceResponse.class.getPackage().getAnnotation(XmlSchema.class).namespace();
                elements.add(new JAXBElement<>(new QName(namespace, name), String.class, value.toString()));
            }
        }
    }
}
