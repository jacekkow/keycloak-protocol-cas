package org.keycloak.protocol.cas.representations;

import org.keycloak.dom.saml.v1.assertion.*;
import org.keycloak.dom.saml.v1.protocol.SAML11ResponseType;
import org.keycloak.dom.saml.v1.protocol.SAML11StatusCodeType;
import org.keycloak.dom.saml.v1.protocol.SAML11StatusType;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;
import org.keycloak.saml.processing.core.saml.v1.writers.SAML11ResponseWriter;
import org.keycloak.services.validation.Validation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SamlResponseHelper {
    private final static DatatypeFactory factory;

    static {
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static SAML11ResponseType errorResponse(CASValidationException ex) {
        ZonedDateTime nowZoned = ZonedDateTime.now(ZoneOffset.UTC);
        XMLGregorianCalendar now = factory.newXMLGregorianCalendar(GregorianCalendar.from(nowZoned));

        return applyTo(new SAML11ResponseType("_" + UUID.randomUUID().toString(), now), obj -> {
            obj.setStatus(applyTo(new SAML11StatusType(), status -> {
                status.setStatusCode(new SAML11StatusCodeType(QName.valueOf("samlp:RequestDenied")));
                status.setStatusMessage(ex.getErrorDescription());
            }));
        });
    }

    public static SAML11ResponseType successResponse(String issuer, String username, Map<String, Object> attributes) {
        ZonedDateTime nowZoned = ZonedDateTime.now(ZoneOffset.UTC);
        XMLGregorianCalendar now = factory.newXMLGregorianCalendar(GregorianCalendar.from(nowZoned));

        return applyTo(new SAML11ResponseType("_" + UUID.randomUUID().toString(), now),
                obj -> {
                    obj.setStatus(applyTo(new SAML11StatusType(), status -> status.setStatusCode(SAML11StatusCodeType.SUCCESS)));
                    obj.add(applyTo(new SAML11AssertionType("_" + UUID.randomUUID().toString(), now), assertion -> {
                        assertion.setIssuer(issuer);
                        assertion.setConditions(applyTo(new SAML11ConditionsType(), conditions -> {
                            conditions.setNotBefore(now);
                            conditions.setNotOnOrAfter(factory.newXMLGregorianCalendar(GregorianCalendar.from(nowZoned.plusMinutes(5))));
                        }));
                        assertion.add(applyTo(new SAML11AuthenticationStatementType(
                                URI.create(SAML11Constants.AUTH_METHOD_PASSWORD),
                                now
                        ), stmt -> stmt.setSubject(toSubject(username))));
                        assertion.addAllStatements(toAttributes(username, attributes));
                    }));
                }
        );
    }

    private static List<SAML11StatementAbstractType> toAttributes(String username, Map<String, Object> attributes) {
        List<SAML11AttributeType> converted = attributeElements(attributes);
        if (converted.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(applyTo(
                new SAML11AttributeStatementType(),
                attrs -> {
                    attrs.setSubject(toSubject(username));
                    attrs.addAllAttributes(converted);
                })
        );
    }

    private static List<SAML11AttributeType> attributeElements(Map<String, Object> attributes) {
        return attributes.entrySet().stream().flatMap(e ->
                toAttribute(e.getKey(), e.getValue())
        ).filter(a -> !a.get().isEmpty()).collect(Collectors.toList());
    }

    private static Stream<SAML11AttributeType> toAttribute(String name, Object value) {
        if (name == null || value == null) {
            return Stream.empty();
        }

        if (value instanceof Collection) {
            return Stream.of(samlAttribute(name, listString((Collection<?>) value)));
        }
        return Stream.of(samlAttribute(name, Collections.singletonList(value.toString())));
    }

    private static SAML11AttributeType samlAttribute(String name, List<Object> listString) {
        return applyTo(
                new SAML11AttributeType(name, URI.create("http://www.ja-sig.org/products/cas/")),
                attr -> attr.addAll(listString)
        );
    }

    private static List<Object> listString(Collection<?> value) {
        return value.stream().map(Object::toString).collect(Collectors.toList());
    }

    private static SAML11SubjectType toSubject(String username) {
        return applyTo(
                new SAML11SubjectType(),
                subject -> subject.setChoice(
                        new SAML11SubjectType.SAML11SubjectTypeChoice(
                                applyTo(
                                        new SAML11NameIdentifierType(username),
                                        ctype -> ctype.setFormat(nameIdFormat(username))
                                )
                        )
                )
        );
    }

    private static URI nameIdFormat(String username) {
        return URI.create(Validation.isEmailValid(username) ?
                SAML11Constants.FORMAT_EMAIL_ADDRESS :
                SAML11Constants.FORMAT_UNSPECIFIED
        );
    }

    private static <A> A applyTo(A input, Consumer<A> setter) {
        setter.accept(input);
        return input;
    }

    public static String soap(SAML11ResponseType response) {
        try {
            Document result = toDOM(response);

            Document doc = wrapSoap(result.getDocumentElement());
            return toString(doc);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Document toDOM(SAML11ResponseType response) throws ParserConfigurationException, XMLStreamException, ProcessingException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        XMLOutputFactory factory = XMLOutputFactory.newFactory();

        Document doc = dbf.newDocumentBuilder().newDocument();
        DOMResult result = new DOMResult(doc);
        XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(result);
        SAML11ResponseWriter writer = new SAML11ResponseWriter(xmlWriter);
        writer.write(response);
        return doc;
    }

    private static Document wrapSoap(Node node) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();

        Element envelope = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soap:Envelope");
        envelope.appendChild(doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soap:Header"));
        Element body = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soap:Body");

        Node imported = doc.importNode(node, true);

        body.appendChild(imported);
        doc.appendChild(body);
        envelope.appendChild(body);
        doc.appendChild(envelope);
        return doc;
    }

    public static String toString(Document document) throws TransformerException {
        // Output the Document
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        t.transform(source, result);
        return writer.toString();
    }
}
