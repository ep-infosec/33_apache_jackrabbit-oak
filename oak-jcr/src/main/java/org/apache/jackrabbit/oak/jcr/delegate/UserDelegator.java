/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.jackrabbit.oak.jcr.delegate;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.jcr.session.operation.SessionOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This implementation of {@code User} delegates back to a
 * delegatee wrapping each call into a {@link SessionOperation} closure.
 *
 * @see SessionDelegate#perform(SessionOperation)
 */
final class UserDelegator extends AuthorizableDelegator implements User {

    private UserDelegator(SessionDelegate sessionDelegate, User userDelegate) {
        super(sessionDelegate, userDelegate);
    }

    @NotNull
    static User wrap(@NotNull SessionDelegate sessionDelegate, @NotNull User user) {
        return new UserDelegator(sessionDelegate, user);
    }

    @NotNull
    static User unwrap(@NotNull User user) {
        if (user instanceof UserDelegator) {
            return ((UserDelegator) user).getDelegate();
        } else {
            return user;
        }
    }

    @NotNull
    private User getDelegate() {
        return (User) delegate;
    }

    //---------------------------------------------------------------< User >---
    @Override
    public boolean isAdmin() {
        return sessionDelegate.safePerform(new SessionOperation<Boolean>("isAdmin") {
            @NotNull
            @Override
            public Boolean perform() {
                return getDelegate().isAdmin();
            }
        });
    }

    @Override
    public boolean isSystemUser() {
        return sessionDelegate.safePerform(new SessionOperation<Boolean>("isSystemUser") {
            @NotNull
            @Override
            public Boolean perform() {
                return getDelegate().isSystemUser();
            }
        });
    }

    @NotNull
    @Override
    public Credentials getCredentials() {
        return sessionDelegate.safePerform(new SessionOperation<Credentials>("getCredentials") {
            @NotNull
            @Override
            public Credentials perform() throws RepositoryException {
                return getDelegate().getCredentials();
            }
        });
    }

    @NotNull
    @Override
    public Impersonation getImpersonation() {
        return sessionDelegate.safePerform(new SessionOperation<Impersonation>("getImpersonation") {
            @NotNull
            @Override
            public Impersonation perform() throws RepositoryException {
                Impersonation impersonation = getDelegate().getImpersonation();
                return ImpersonationDelegator.wrap(sessionDelegate, impersonation);
            }
        });
    }

    @Override
    public void changePassword(@Nullable final String password) throws RepositoryException {
        sessionDelegate.performVoid(new SessionOperation<Void>("changePassword", true) {
            @Override
            public void performVoid() throws RepositoryException {
                getDelegate().changePassword(password);
            }
        });
    }

    @Override
    public void changePassword(@Nullable final String password, @NotNull final String oldPassword) throws RepositoryException {
        sessionDelegate.performVoid(new SessionOperation<Void>("changePassword", true) {
            @Override
            public void performVoid() throws RepositoryException {
                getDelegate().changePassword(password, oldPassword);
            }
        });
    }

    @Override
    public void disable(@Nullable final String reason) throws RepositoryException {
        sessionDelegate.performVoid(new SessionOperation<Void>("disable", true) {
            @Override
            public void performVoid() throws RepositoryException {
                getDelegate().disable(reason);
            }
        });
    }

    @Override
    public boolean isDisabled() throws RepositoryException {
        return sessionDelegate.perform(new SessionOperation<Boolean>("isDisabled") {
            @NotNull
            @Override
            public Boolean perform() throws RepositoryException {
                return getDelegate().isDisabled();
            }
        });
    }

    @Nullable
    @Override
    public String getDisabledReason() throws RepositoryException {
        return sessionDelegate.performNullable(new SessionOperation<String>("getDisabledReason") {
            @Override
            public String performNullable() throws RepositoryException {
                return getDelegate().getDisabledReason();
            }
        });
    }
}
