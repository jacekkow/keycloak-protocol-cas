#!/bin/bash
set -e

keycloak_cas_url='http://localhost:8080/auth/realms/master/protocol/cas'
action_pattern='action="([^"]+)"'
ticket_pattern='Location: .*\?ticket=(ST-[-A-Za-z0-9_.=]+)'

get_ticket() {
    local cookie_options="-b /tmp/cookies"
    if [ "$1" == "save_cookies" ]; then
      cookie_options="${cookie_options} -c /tmp/cookies"
    fi

    local login_response=$(curl --fail --silent -c /tmp/cookies "${keycloak_cas_url}/login?service=http://localhost")
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
ticket=$(get_ticket)
curl --fail --silent "${keycloak_cas_url}/validate?service=http://localhost&ticket=$ticket"
echo

# CAS 2.0
ticket=$(get_ticket)
curl --fail --silent "${keycloak_cas_url}/serviceValidate?service=http://localhost&format=XML&ticket=$ticket"
echo

ticket=$(get_ticket)
curl --fail --silent "${keycloak_cas_url}/serviceValidate?service=http://localhost&format=JSON&ticket=$ticket"
echo

# CAS 3.0
ticket=$(get_ticket save_cookies)
curl --fail --silent "${keycloak_cas_url}/p3/serviceValidate?service=http://localhost&format=JSON&ticket=$ticket"
echo

# CAS, gateway option
get_ticket save_cookies
login_response=$(curl --fail --silent -D - -b /tmp/cookies "${keycloak_cas_url}/login?service=http://localhost&gateway=true")
if echo "${login_response}" | grep '^Location: http://localhost\?ticket='; then
    echo "Gateway option did not redirect back to service with ticket"
    echo "${login_response}"
    exit 1
fi

login_response=$(curl --fail --silent -D - "${keycloak_cas_url}/login?service=http://localhost&gateway=true")
if echo "${login_response}" | grep '^Location: http://localhost$'; then
    echo "Gateway option did not redirect back to service without ticket"
    echo "${login_response}"
    exit 1
fi
