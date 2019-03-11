#!/bin/bash
set -e

function docker_tag_exists() {
    REGISTRY_URL="https://quay.io/v2/$1/manifests/$2"
    curl -fsSLI "$REGISTRY_URL" > /dev/null
}

setup_git() {
    git config --global user.email "updater@travis-ci.org"
    git config --global user.name "Updater Bot"
    git remote add origin-auth https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git > /dev/null 2>&1
}

pull_request() {
    curl -fsSL -H "Authorization: token ${GH_TOKEN}" -X POST -d "{\"head\":\"$1\",\"base\":\"master\",\"title\":\"$2\",\"body\":\"$3\"}" "https://api.github.com/repos/${TRAVIS_REPO_SLUG}/pulls"
}


KEYCLOAK_VERSION=$(mvn versions:display-property-updates -DincludeProperties=keycloak.version | grep "keycloak.version" | sed -nr "s/.*->\s*([0-9]+\.[0-9]+\.[0-9])$/\1/p")
if [ -z "$KEYCLOAK_VERSION" ]; then
    echo "No Keycloak update found."
    exit
fi
echo "Keycloak version $KEYCLOAK_VERSION available; updating..."

BRANCH=feature/keycloak-update-$KEYCLOAK_VERSION
if git ls-remote -q --exit-code origin $BRANCH; then
    echo "Branch $BRANCH already exists."
    exit
fi

if ! docker_tag_exists keycloak/keycloak $KEYCLOAK_VERSION; then
    echo "Docker image for Keycloak $KEYCLOAK_VERSION not found, not updating."
    exit
fi
echo "Found updated docker image, proceeding"

mvn versions:set -DnewVersion=$KEYCLOAK_VERSION -DgenerateBackupPoms=false
sed -i "s/KEYCLOAK_VERSION=.*/KEYCLOAK_VERSION=$KEYCLOAK_VERSION/" .travis.yml

setup_git
git checkout -b $BRANCH
git add pom.xml .travis.yml
git commit -m "Update to Keycloak $KEYCLOAK_VERSION"
git push --quiet --set-upstream origin-auth $BRANCH

PR_TITLE="Update to Keycloak $KEYCLOAK_VERSION"
PR_BODY="Updates Keycloak dependency, CI test image and project version for Keycloak release $KEYCLOAK_VERSION\\n\\n*(automated pull request after upstream release)*"
pull_request $BRANCH "$PR_TITLE" "$PR_BODY"
echo "Created pull request '$PR_TITLE'"
