package org.keycloak.protocol.cas;

import com.jayway.jsonpath.JsonPath;
import com.sun.xml.bind.v2.util.FatalAdapter;
import org.junit.Test;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseMarshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlunit.xpath.JAXPXPathEngine;
import org.xmlunit.xpath.XPathEngine;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ServiceResponseTest {
    private final XPathEngine xpath = new JAXPXPathEngine();

    public ServiceResponseTest() {
        xpath.setNamespaceContext(Collections.singletonMap("cas", "http://www.yale.edu/tp/cas"));
    }

    @Test
    public void testSuccessResponse() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("list", Arrays.asList("a", "b"));
        attributes.put("int", 123);
        attributes.put("string", "abc");

        List<String> proxies = Arrays.asList("https://proxy1/pgtUrl", "https://proxy2/pgtUrl");
        CASServiceResponse response = ServiceResponseHelper.createSuccess("username", attributes, "PGTIOU-test",
                proxies);

        // Build and validate JSON response

        String json = ServiceResponseMarshaller.marshalJson(response);
        assertEquals("username", JsonPath.read(json, "$.serviceResponse.authenticationSuccess.user"));
        assertEquals(attributes.get("list"), JsonPath.read(json, "$.serviceResponse.authenticationSuccess.attributes.list"));
        assertEquals(attributes.get("int"), JsonPath.read(json, "$.serviceResponse.authenticationSuccess.attributes.int"));
        assertEquals(attributes.get("string"), JsonPath.read(json, "$.serviceResponse.authenticationSuccess.attributes.string"));
        assertEquals("PGTIOU-test", JsonPath.read(json, "$.serviceResponse.authenticationSuccess.proxyGrantingTicket"));
        assertEquals(proxies, JsonPath.read(json, "$.serviceResponse.authenticationSuccess.proxies"));

        // Build and validate XML response

        String xml = ServiceResponseMarshaller.marshalXml(response);
        Document doc = parseAndValidate(xml);
        assertEquals("username", xpath.evaluate("/cas:serviceResponse/cas:authenticationSuccess/cas:user", doc));
        int idx = 0;
        for (Node node : xpath.selectNodes("/cas:serviceResponse/cas:authenticationSuccess/cas:attributes/cas:list", doc)) {
            assertEquals(((List)attributes.get("list")).get(idx), node.getTextContent());
            idx++;
        }
        assertEquals(((List)attributes.get("list")).size(), idx);
        assertEquals(attributes.get("int").toString(), xpath.evaluate("/cas:serviceResponse/cas:authenticationSuccess/cas:attributes/cas:int", doc));
        assertEquals(attributes.get("string").toString(), xpath.evaluate("/cas:serviceResponse/cas:authenticationSuccess/cas:attributes/cas:string", doc));

        assertEquals("PGTIOU-test", xpath.evaluate("/cas:serviceResponse/cas:authenticationSuccess/cas:proxyGrantingTicket", doc));
        idx = 0;
        for (Node node : xpath.selectNodes("/cas:serviceResponse/cas:authenticationSuccess/cas:proxies/cas:proxy", doc)) {
            assertEquals(proxies.get(idx), node.getTextContent());
            idx++;
        }
        assertEquals(proxies.size(), idx);
    }

    @Test
    public void testErrorResponse() throws Exception {
        CASServiceResponse response = ServiceResponseHelper.createFailure(CASErrorCode.INVALID_REQUEST, "Error description");

        // Build and validate JSON response

        String json = ServiceResponseMarshaller.marshalJson(response);
        assertEquals(CASErrorCode.INVALID_REQUEST.name(), JsonPath.read(json, "$.serviceResponse.authenticationFailure.code"));
        assertEquals("Error description", JsonPath.read(json, "$.serviceResponse.authenticationFailure.description"));

        // Build and validate XML response

        String xml = ServiceResponseMarshaller.marshalXml(response);
        Document doc = parseAndValidate(xml);
        assertEquals(CASErrorCode.INVALID_REQUEST.name(), xpath.evaluate("/cas:serviceResponse/cas:authenticationFailure/@code", doc));
        assertEquals("Error description", xpath.evaluate("/cas:serviceResponse/cas:authenticationFailure", doc));
    }

    /**
     * Parse XML document and validate against CAS schema
     */
    private Document parseAndValidate(String xml) throws Exception {
        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(getClass().getResource("cas-response-schema.xsd"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setSchema(schema);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new FatalAdapter(new DefaultHandler()));
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
