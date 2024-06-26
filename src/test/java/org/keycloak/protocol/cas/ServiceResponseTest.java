package org.keycloak.protocol.cas;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseMarshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xmlunit.xpath.JAXPXPathEngine;
import org.xmlunit.xpath.XPathEngine;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.keycloak.protocol.cas.XMLValidator.parseAndValidate;
import static org.keycloak.protocol.cas.XMLValidator.schemaFromClassPath;

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
        Document doc = parseAndValidate(xml, schemaFromClassPath("cas-response-schema.xsd"));
        assertEquals("username", xpath.evaluate("/cas:serviceResponse/cas:authenticationSuccess/cas:user", doc));
        int idx = 0;
        for (Node node : xpath.selectNodes("/cas:serviceResponse/cas:authenticationSuccess/cas:attributes/cas:list", doc)) {
            assertEquals(((List<?>)attributes.get("list")).get(idx), node.getTextContent());
            idx++;
        }
        assertEquals(((List<?>)attributes.get("list")).size(), idx);
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
        Document doc = parseAndValidate(xml, schemaFromClassPath("cas-response-schema.xsd"));
        assertEquals(CASErrorCode.INVALID_REQUEST.name(), xpath.evaluate("/cas:serviceResponse/cas:authenticationFailure/@code", doc));
        assertEquals("Error description", xpath.evaluate("/cas:serviceResponse/cas:authenticationFailure", doc));
    }
}
