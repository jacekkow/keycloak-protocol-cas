# keycloak-protocol-cas

This plugin for Keycloak Identity and Access Management (http://www.keycloak.org) adds the CAS 3.0 SSO protocol
as an available client protocol to the Keycloak system. It implements the required Service Provider Interfaces (SPIs)
for a Login Protocol and will be picked up and made available by Keycloak automatically once installed.

![Build status](https://github.com/jacekkow/keycloak-protocol-cas/workflows/Release/badge.svg)

## Features

The following CAS features are currently implemented:
* CAS 1.0/2.0/3.0 compliant Login/Logout and Service Ticket Validation
* Single Logout (SLO)
* Filtering of provided `service` against configured redirect URIs
* JSON and XML response types
* Mapping of custom user attributes to CAS assertion attributes

The following features are **missing**:
* SAML request/response [CAS 3.0 - optional]
* Proxy ticket service and proxy ticket validation [CAS 2.0]

The following features are out of scope:
* Long-Term Tickets - Remember-Me [CAS 3.0 - optional]

## Compatibility

The CAS plugin has been tested against the same Keycloak version as the plugin version.

As a rule of thumb plugin version should **match your Keycloak version**.

## Installation

Installation of a compatible plugin version is simple and can be done without a Keycloak server restart.

1. Download the latest release compatible with your Keycloak version from the [releases page](https://github.com/jacekkow/keycloak-protocol-cas/releases).
2. Copy the JAR file into the `standalone/deployments` directory in your Keycloak server's root.

Remember to update plugin artifact with each Keycloak server upgrade!

## Configuration

To use the new protocol, you have to create a client within Keycloak as usual, selecting `cas` as protocol.
As there is no client ID indication in protocol, the client will be identified by the redirect URIs
configured in Keycloak.

Enter `https://your.keycloak.host/auth/realms/master/protocol/cas` as the CAS URL into your SP.

## Disclaimer

This plugin was implemented from scratch to comply to the official CAS protocol specification,
and is based heavily on the OpenID Connect implementation in Keycloak.
It is licensed under the Apache License 2.0.

This repo is a fork of https://github.com/Doccrazy/keycloak-protocol-cas
and includes changes for Keycloak 8 and newer that were not merged by the owner for half a year.

## References
[1] http://www.keycloak.org
[2] https://issues.jboss.org/browse/KEYCLOAK-1047 (Support CAS 2.0 SSO protocol)
[3] https://apereo.github.io/cas/4.2.x/protocol/CAS-Protocol-Specification.html
[4] https://keycloak.gitbooks.io/server-developer-guide/content/topics/providers.html
