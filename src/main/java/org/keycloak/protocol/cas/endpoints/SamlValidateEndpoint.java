package org.keycloak.protocol.cas.endpoints;

import org.keycloak.dom.saml.v1.protocol.SAML11ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.representations.SamlResponseHelper;
import org.keycloak.protocol.cas.utils.CASValidationException;
import org.keycloak.protocol.cas.utils.UsernameMapperHelper;
import org.keycloak.services.Urls;
import org.xml.sax.InputSource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.*;

import static org.keycloak.protocol.cas.CASLoginProtocol.TARGET_PARAM;

public class SamlValidateEndpoint extends AbstractValidateEndpoint {
    public SamlValidateEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event.event(EventType.CODE_TO_TOKEN));
    }

    @POST
    @Consumes("text/xml;charset=utf-8")
    @Produces("text/xml;charset=utf-8")
    public Response validate(String input) {
        MultivaluedMap<String, String> queryParams = request.getUri().getQueryParameters();
        try {
            String soapAction = Optional.ofNullable(request.getHttpHeaders().getHeaderString("SOAPAction")).map(s -> s.trim().replace("\"", "")).orElse("");
            if (!soapAction.equals("http://www.oasis-open.org/committees/security")) {
                throw new CASValidationException(CASErrorCode.INTERNAL_ERROR, "Not a validation request", Response.Status.BAD_REQUEST);
            }

            String service = queryParams.getFirst(TARGET_PARAM);
            boolean renew = queryParams.containsKey(CASLoginProtocol.RENEW_PARAM);

            checkRealm();
            checkSsl();
            checkClient(service);
            String issuer = Urls.realmIssuer(request.getUri().getBaseUri(), realm.getName());
            String ticket = getTicket(input);

            checkTicket(ticket, renew);
            UserModel user = clientSession.getUserSession().getUser();

            Map<String, Object> attributes = getUserAttributes();

            SAML11ResponseType response = SamlResponseHelper.successResponse(issuer, UsernameMapperHelper.getMappedUsername(session,clientSession), attributes);

            return Response.ok(SamlResponseHelper.soap(response)).build();

        } catch (CASValidationException ex) {
            logger.warnf("Invalid SAML1.1 token %s", ex.getErrorDescription());

            SAML11ResponseType response = SamlResponseHelper.errorResponse(ex);
            return Response.ok().entity(SamlResponseHelper.soap(response)).build();
        }
    }

    private String getTicket(String input) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new MapNamespaceContext(Collections.singletonMap("samlp", "urn:oasis:names:tc:SAML:1.0:protocol")));

            XPathExpression expression = xPath.compile("//samlp:AssertionArtifact/text()");

            return expression.evaluate(new InputSource(new StringReader(input)));
        } catch (XPathExpressionException ex) {
            throw new CASValidationException(CASErrorCode.INVALID_TICKET, ex.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private static class MapNamespaceContext implements NamespaceContext {
        Map<String, String> map;

        private MapNamespaceContext(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public String getNamespaceURI(String s) {
            return map.get(s);
        }

        @Override
        public String getPrefix(String s) {
            return map.entrySet().stream().filter(e -> e.getValue().equals(s)).findFirst().map(Map.Entry::getKey).orElse(null);
        }

        @Override
        public Iterator<String> getPrefixes(String s) {
            return map.keySet().iterator();
        }
    }
}
