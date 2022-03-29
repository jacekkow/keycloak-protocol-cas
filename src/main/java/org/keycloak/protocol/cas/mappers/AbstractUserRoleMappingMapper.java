/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.cas.mappers;

import org.keycloak.models.ProtocolMapperModel;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for mapping of user role mappings to an ID and Access Token claim.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
abstract class AbstractUserRoleMappingMapper extends AbstractUserAttributeMapper {

    /**
     * Retrieves all roles of the current user based on direct roles set to the user, its groups and their parent groups.
     * Then it recursively expands all composite roles, and restricts according to the given predicate {@code restriction}.
     * If the current client sessions is restricted (i.e. no client found in active user session has full scope allowed),
     * the final list of roles is also restricted by the client scope. Finally, the list is mapped to the token into
     * a claim.
     */
    protected void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, Set<String> rolesToAdd,
                                  String prefix) {
        Set<String> realmRoleNames;
        if (prefix != null && !prefix.isEmpty()) {
            realmRoleNames = rolesToAdd.stream()
                    .map(roleName -> prefix + roleName)
                    .collect(Collectors.toSet());
        } else {
            realmRoleNames = rolesToAdd;
        }

        setPlainAttribute(attributes, mappingModel, realmRoleNames);
    }
}
