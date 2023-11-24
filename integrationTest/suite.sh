#!/bin/bash
set -e

keycloak_cas_url='http://localhost:8080/realms/master/protocol/cas'
action_pattern='action="([^"]+)"'

get_ticket() {
    local cookie_options="-b /tmp/cookies"
    local ticket_pattern='Location: .*\?ticket=(ST-[-A-Za-z0-9_.=]+)'
    local client_url_param=service

    if [ "$1" == "save_cookies" ]; then
      cookie_options="${cookie_options} -c /tmp/cookies"
    elif [ "$1" == "SAML" ]; then
      ticket_pattern='Location: .*\?SAMLart=(ST-[-A-Za-z0-9_.=]+)'
      client_url_param=TARGET
    fi

    local login_response=$(curl --fail --silent -c /tmp/cookies "${keycloak_cas_url}/login?${client_url_param}=http://localhost")
    if [[ ! ($login_response =~ $action_pattern) ]] ; then
        echo "Could not parse login form in response"
        echo "${login_response}"
        exit 1
    fi

    local login_url=${BASH_REMATCH[1]//&amp;/&}
    local redirect_response=$(curl --fail --silent -D - $cookie_options --data 'username=admin&password=admin' "$login_url")
    if [[ ! ($redirect_response =~ $ticket_pattern) ]] ; then
        echo "No service ticket found in response"
        echo "${redirect_response}"
        exit 1
    fi

    echo "${BASH_REMATCH[1]}"
}

# CAS 1.0
echo "Testing CAS 1.0..."
ticket=$(get_ticket)
curl --fail --silent "${keycloak_cas_url}/validate?service=http://localhost&ticket=$ticket"
echo

# CAS 2.0
echo "Testing CAS 2.0 - XML..."
ticket=$(get_ticket)
curl --fail --silent "${keycloak_cas_url}/serviceValidate?service=http://localhost&format=XML&ticket=$ticket"
echo

echo "Testing CAS 2.0 - JSON..."
ticket=$(get_ticket)
curl --fail --silent "${keycloak_cas_url}/serviceValidate?service=http://localhost&format=JSON&ticket=$ticket"
echo

# CAS 3.0
echo "Testing CAS 3.0..."
ticket=$(get_ticket save_cookies)
curl --fail --silent "${keycloak_cas_url}/p3/serviceValidate?service=http://localhost&format=JSON&ticket=$ticket"
echo

# SAML 1.1
echo "Testing SAML 1.1..."
ticket=$(get_ticket SAML)
timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
saml_template=$(dirname "$0")/samlValidateTemplate.xml
sed -e "s/CAS_TICKET/$ticket/g" -e "s/TIMESTAMP/$timestamp/g" "$saml_template" \
  | curl --fail --silent -X POST -H "Content-Type: text/xml" \
      -H "SOAPAction: http://www.oasis-open.org/committees/security" \
      --data-binary @- "${keycloak_cas_url}/samlValidate?TARGET=http://localhost"
echo

# CAS - gateway option
echo "Testing CAS - gateway option, stage 1..."
get_ticket save_cookies
login_response=$(curl --fail --silent -D - -b /tmp/cookies "${keycloak_cas_url}/login?service=http://localhost&gateway=true")
if echo "${login_response}" | grep '^Location: http://localhost\?ticket='; then
    echo "Gateway option did not redirect back to service with ticket"
    echo "${login_response}"
    exit 1
fi

echo "Testing CAS - gateway option, stage 2..."
login_response=$(curl --fail --silent -D - "${keycloak_cas_url}/login?service=http://localhost&gateway=true")
if echo "${login_response}" | grep '^Location: http://localhost$'; then
    echo "Gateway option did not redirect back to service without ticket"
    echo "${login_response}"
    exit 1
fi
