package org.keycloak.protocol.cas.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.keycloak.protocol.cas.representations.CASServiceResponse;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods to marshal service response object to XML/JSON<br
 * For details on expected format see CAS-Protocol-Specification.html, section 2.5/2.6
 */
public final class ServiceResponseMarshaller {
    private ServiceResponseMarshaller() {
    }

    public static String marshalXml(CASServiceResponse serviceResponse) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CASServiceResponse.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            //disable xml header
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(serviceResponse, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static String marshalJson(CASServiceResponse serviceResponse) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //Force newlines to be LF (default is system dependent)
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"));

        //create wrapper node
        Map<String, Object> casModel = new HashMap<>();
        casModel.put("serviceResponse", serviceResponse);
        try {
            return mapper.writer(printer).writeValueAsString(casModel);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
