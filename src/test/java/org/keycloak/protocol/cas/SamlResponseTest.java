package org.keycloak.protocol.cas;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.dom.saml.v1.protocol.SAML11ResponseType;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.SamlResponseHelper;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.w3c.dom.Document;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SamlResponseTest {
    @Test
    public void successResponseIsWrappedInSOAP() {
        SAML11ResponseType response = SamlResponseHelper.successResponse("keycloak", "test@example.com", Collections.emptyMap());
        String soapResult = SamlResponseHelper.soap(response);
        assertTrue(soapResult.contains("samlp:Success"));
        assertTrue(soapResult.contains("test@example.com"));
        assertTrue(soapResult.contains("keycloak"));
    }

    @Test
    public void failureResponseIsWrappedInSOAP() {
        SAML11ResponseType response = SamlResponseHelper.errorResponse(new CASValidationException(CASErrorCode.INVALID_TICKET, "Nope", Response.Status.BAD_REQUEST));
        String nope = SamlResponseHelper.soap(response);
        assertTrue(nope.contains("Nope"));
    }

    @Test
    public void validateSchemaResponseFailure() throws Exception {
        SAML11ResponseType response = SamlResponseHelper.errorResponse(new CASValidationException(CASErrorCode.INVALID_TICKET, "Nope", Response.Status.BAD_REQUEST));
        String output = SamlResponseHelper.toString(SamlResponseHelper.toDOM(response));
        Document doc = XMLValidator.parseAndValidate(output, XMLValidator.schemaFromClassPath("oasis-sstc-saml-schema-protocol-1.1.xsd"));
        assertNotNull(doc);
    }

    @Test
    public void validateSchemaResponseSuccess() throws Exception {
        SAML11ResponseType response = SamlResponseHelper.successResponse("keycloak", "test@example.com", Collections.emptyMap());
        String output = SamlResponseHelper.toString(SamlResponseHelper.toDOM(response));
        Document doc = XMLValidator.parseAndValidate(output, XMLValidator.schemaFromClassPath("oasis-sstc-saml-schema-protocol-1.1.xsd"));
        assertNotNull(doc);
    }
}
