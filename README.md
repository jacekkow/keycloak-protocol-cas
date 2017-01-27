# keycloak-protocol-cas
This plugin for Keycloak Identity and Access Management (http://www.keycloak.org) adds the CAS 3.0 SSO protocol as an available client protocol to the Keycloak system. It implements the required Service Provider Interfaces (SPIs) for a Login Protocol and will be picked up and made available by Keycloak automatically once installed.

## Features
The following CAS features are currently implemented:
* CAS 1.0/2.0/3.0 compliant Login/Logout and Service Ticket Validation
* Filtering of provided `service` against configured redirect URIs
* JSON and XML response types
* Mapping of custom user attributes to CAS assertion attributes

The following features are **curently missing**:
* Proxy ticket service and proxy ticket validation [CAS 2.0]
* Long-Term Tickets - Remember-Me [CAS 3.0 - optional]
* SAML request/response [CAS 3.0 - optional]

## Installation
1. Clone or download this repository (pre-compiled releases will follow!)
2. Run `mvn package` to build the plugin JAR
3. Copy the JAR file generated in the `target` folder into the `providers` directory in your Keycloak server's root
4. Restart Keycloak

## Configuration
To use the new protocol, you have to create a client within Keycloak as usual.  
**Important: Due to [KEYCLOAK-4270](https://issues.jboss.org/browse/KEYCLOAK-4270), you have to select the `openid-connect` protocol when creating the client and change it after saving.**  
As the CAS protocol does not transmit a client ID, the client will be identified by the redirect URIs (mapped to CAS service). No further configuration is necessary.

Enter `https://your.keycloak.host/auth/realms/master/protocol/cas` as the CAS URL into your SP.

## Disclaimer
This plugin was implemented from scratch to comply to the official CAS protocol specification, and is based heavily on the OpenID Connect implementation in Keycloak.  
It is licensed under the Apache License 2.0.

## References
[1] http://www.keycloak.org  
[2] https://issues.jboss.org/browse/KEYCLOAK-1047 (Support CAS 2.0 SSO protocol)  
[3] https://apereo.github.io/cas/4.2.x/protocol/CAS-Protocol-Specification.html  
[4] https://keycloak.gitbooks.io/server-developer-guide/content/topics/providers.html
