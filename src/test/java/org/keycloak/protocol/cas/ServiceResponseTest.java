package org.keycloak.protocol.cas;

import org.junit.Test;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.CASServiceResponse;
import org.keycloak.protocol.cas.utils.ServiceResponseHelper;
import org.keycloak.protocol.cas.utils.ServiceResponseMarshaller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ServiceResponseTest {
    private static final String EXPECTED_JSON_SUCCESS = "{\n" +
            "  \"serviceResponse\" : {\n" +
            "    \"authenticationSuccess\" : {\n" +
            "      \"user\" : \"username\",\n" +
            "      \"proxyGrantingTicket\" : \"PGTIOU-test\",\n" +
            "      \"proxies\" : [ \"https://proxy1/pgtUrl\", \"https://proxy2/pgtUrl\" ],\n" +
            "      \"attributes\" : {\n" +
            "        \"string\" : \"abc\",\n" +
            "        \"list\" : [ \"a\", \"b\" ],\n" +
            "        \"int\" : 123\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String EXPECTED_XML_SUCCESS = "<cas:serviceResponse xmlns:cas=\"http://www.yale.edu/tp/cas\">\n" +
            "    <cas:authenticationSuccess>\n" +
            "        <cas:user>username</cas:user>\n" +
            "        <cas:proxyGrantingTicket>PGTIOU-test</cas:proxyGrantingTicket>\n" +
            "        <cas:proxies>\n" +
            "            <cas:proxy>https://proxy1/pgtUrl</cas:proxy>\n" +
            "            <cas:proxy>https://proxy2/pgtUrl</cas:proxy>\n" +
            "        </cas:proxies>\n" +
            "        <cas:attributes>\n" +
            "            <cas:string>abc</cas:string>\n" +
            "            <cas:list>a</cas:list>\n" +
            "            <cas:list>b</cas:list>\n" +
            "            <cas:int>123</cas:int>\n" +
            "        </cas:attributes>\n" +
            "    </cas:authenticationSuccess>\n" +
            "</cas:serviceResponse>";
    private static final String EXPECTED_JSON_FAILURE = "{\n" +
            "  \"serviceResponse\" : {\n" +
            "    \"authenticationFailure\" : {\n" +
            "      \"code\" : \"INVALID_REQUEST\",\n" +
            "      \"description\" : \"Error description\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String EXPECTED_XML_FAILURE = "<cas:serviceResponse xmlns:cas=\"http://www.yale.edu/tp/cas\">\n" +
            "    <cas:authenticationFailure code=\"INVALID_REQUEST\">Error description</cas:authenticationFailure>\n" +
            "</cas:serviceResponse>";

    @Test
    public void testSuccessResponse() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("list", Arrays.asList("a", "b"));
        attributes.put("int", 123);
        attributes.put("string", "abc");

        CASServiceResponse response = ServiceResponseHelper.createSuccess("username", attributes, "PGTIOU-test",
                Arrays.asList("https://proxy1/pgtUrl", "https://proxy2/pgtUrl"));

        assertEquals(EXPECTED_JSON_SUCCESS, ServiceResponseMarshaller.marshalJson(response));
        assertEquals(EXPECTED_XML_SUCCESS, ServiceResponseMarshaller.marshalXml(response));
    }

    @Test
    public void testErrorResponse() throws Exception {
        CASServiceResponse response = ServiceResponseHelper.createFailure(CASErrorCode.INVALID_REQUEST, "Error description");

        assertEquals(EXPECTED_JSON_FAILURE, ServiceResponseMarshaller.marshalJson(response));
        assertEquals(EXPECTED_XML_FAILURE, ServiceResponseMarshaller.marshalXml(response));
    }
}
