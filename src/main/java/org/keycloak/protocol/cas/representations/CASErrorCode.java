package org.keycloak.protocol.cas.representations;

public enum CASErrorCode {
    /** not all of the required request parameters were present */
    INVALID_REQUEST,
    /** failure to meet the requirements of validation specification */
    INVALID_TICKET_SPEC,
    /** the service is not authorized to perform proxy authentication */
    UNAUTHORIZED_SERVICE_PROXY,
    /** The proxy callback specified is invalid. The credentials specified for proxy authentication do not meet the security requirements */
    INVALID_PROXY_CALLBACK,
    /** The proxy callback specified return with error*/
    PROXY_CALLBACK_ERROR,
    /** the ticket provided was not valid, or the ticket did not come from an initial login and renew was set on validation. */
    INVALID_TICKET,
    /** the ticket provided was valid, but the service specified did not match the service associated with the ticket. */
    INVALID_SERVICE,
    /** an internal error occurred during ticket validation */
    INTERNAL_ERROR
}
