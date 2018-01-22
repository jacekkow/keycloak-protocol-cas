# keycloak-protocol-cas
This plugin for Keycloak Identity and Access Management (http://www.keycloak.org) adds the CAS 3.0 SSO protocol as an available client protocol to the Keycloak system. It implements the required Service Provider Interfaces (SPIs) for a Login Protocol and will be picked up and made available by Keycloak automatically once installed.

[![Build Status](https://travis-ci.org/Doccrazy/keycloak-protocol-cas.svg?branch=master)](https://travis-ci.org/Doccrazy/keycloak-protocol-cas)

## Features
The following CAS features are currently implemented:
* CAS 1.0/2.0/3.0 compliant Login/Logout and Service Ticket Validation
* Single Logout (SLO)
* Filtering of provided `service` against configured redirect URIs
* JSON and XML response types
* Mapping of custom user attributes to CAS assertion attributes

The following features are **currently missing**:
* [#1](/../../issues/1): SAML request/response [CAS 3.0 - optional]
* [#2](/../../issues/2): Proxy ticket service and proxy ticket validation [CAS 2.0]

The following features are out of scope:
* Long-Term Tickets - Remember-Me [CAS 3.0 - optional]

## Compatibility
The CAS plugin has been tested against the following Keycloak versions. Please ensure your version is compatible before deploying.  
Please report test results with other versions!

* For Keycloak **2.5.x, 3.0.x and 3.1.x** please use plugin version **1.0.0**
* For Keycloak **3.2.x, 3.3.x and 3.4.0** please use plugin version **2.1.0**
* Starting from Keycloak **3.4.3**, the plugin version should **match your Keycloak version**

## Installation
Installation of a compatible plugin version is simple and can be done without a Keycloak server restart.

1. Download the latest release compatible with your Keycloak version from the [releases page](https://github.com/Doccrazy/keycloak-protocol-cas/releases)
2. Copy the JAR file into the `standalone/deployments` directory in your Keycloak server's root
3. Restart Keycloak (optional, hot deployment should work)

## Configuration
To use the new protocol, you have to create a client within Keycloak as usual.  
**Important: Due to [KEYCLOAK-4270](https://issues.jboss.org/browse/KEYCLOAK-4270), you may have to select the `openid-connect` protocol when creating the client and change it after saving. This has been fixed in Keycloak 3.0.0.**  
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
