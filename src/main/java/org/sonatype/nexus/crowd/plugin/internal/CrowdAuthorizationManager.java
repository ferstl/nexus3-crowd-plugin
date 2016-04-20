/*
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.crowd.plugin.internal;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.authz.AbstractReadOnlyAuthorizationManager;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;

/**
 * @author justin
 * @author Issa Gorissen
 */
@Singleton
@Typed(AuthorizationManager.class)
@Named("Crowd")
public class CrowdAuthorizationManager extends AbstractReadOnlyAuthorizationManager {

    private DefaultCrowdClientHolder crowdClientHolder;

    private static final Logger logger = LoggerFactory.getLogger(CrowdAuthorizationManager.class);

    @Inject
    public CrowdAuthorizationManager(DefaultCrowdClientHolder holder) {
        logger.info("CrowdAuthorizationManager is starting...");
        crowdClientHolder = holder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public Privilege getPrivilege(String privilegeId) throws NoSuchPrivilegeException {
        throw new NoSuchPrivilegeException("Crowd plugin doesn't support privileges");
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public Role getRole(String roleId) throws NoSuchRoleException {
        if (crowdClientHolder.isConfigured()) {
            try {
                Role role = crowdClientHolder.getRestClient().getGroup(roleId);
                role.setSource(getSource());
                return role;
            } catch (Exception e) {
                throw new NoSuchRoleException("Failed to get role " + roleId + " from Crowd.", e);
            }
        } else {
            throw new NoSuchRoleException("Crowd plugin is not configured.");
        }
    }

    @Override
	public String getSource() {
        return CrowdUserManager.SOURCE;
    }

    @Override
	public Set<Privilege> listPrivileges() {
        return Collections.emptySet();
    }

    @Override
	public Set<Role> listRoles() {
        if (crowdClientHolder.isConfigured()) {
            try {
            	Set<Role> roles = crowdClientHolder.getRestClient().getAllGroups();
            	for (Role role : roles) {
            		role.setSource(getSource());
            	}
                return roles;
            } catch (OperationFailedException e) {
            	logger.error("Unable to load roles", e);
                return null;
			} catch (InvalidAuthenticationException e) {
				logger.error("Unable to load roles", e);
                return null;
			} catch (ApplicationPermissionException e) {
				logger.error("Unable to load roles", e);
                return null;
			}
        }
        UnconfiguredNotifier.unconfigured();
        return Collections.emptySet();
    }

}
