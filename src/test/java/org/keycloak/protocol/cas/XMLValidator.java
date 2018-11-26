package org.keycloak.protocol.cas;

import com.sun.xml.bind.v2.util.FatalAdapter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;

public abstract class XMLValidator {
    private XMLValidator(){}

    public static Schema schemaFromClassPath(String path) throws SAXException {
        return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(XMLValidator.class.getResource(path));
    }

    /**
     * Parse XML document and validate against CAS schema
     */
    public static Document parseAndValidate(String xml, Schema schema) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setSchema(schema);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new FatalAdapter(new DefaultHandler()));
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
