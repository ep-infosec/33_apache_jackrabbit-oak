/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.jcr.security.user;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.security.internal.SecurityProviderBuilder;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.apache.jackrabbit.test.NotExecutableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;

import static org.apache.jackrabbit.oak.jcr.AbstractRepositoryTest.dispose;
import static org.junit.Assert.assertNotEquals;

/**
 * Base class for user import related tests.
 */
public abstract class AbstractImportTest {

    private static final String ADMINISTRATORS = "administrators";
    protected static final String USERPATH = UserConstants.DEFAULT_USER_PATH;
    protected static final String GROUPPATH = UserConstants.DEFAULT_GROUP_PATH;

    private Repository repo;

    protected SecurityProvider securityProvider;

    protected Session adminSession;
    protected UserManager userMgr;

    private Set<String> preTestAuthorizables = new HashSet<>();

    @Before
    public void before() throws Exception {
        ConfigurationParameters config = getConfigurationParameters();
        if (config != null) {
            securityProvider = SecurityProviderBuilder.newBuilder().with(config).build();
        } else {
            securityProvider = SecurityProviderBuilder.newBuilder().build();
        }
        QueryEngineSettings queryEngineSettings = new QueryEngineSettings();
        queryEngineSettings.setFailTraversal(true);
        Jcr jcr = new Jcr();
        jcr.with(securityProvider);
        jcr.with(queryEngineSettings);
        repo = jcr.createRepository();
        adminSession = repo.login(new SimpleCredentials(UserConstants.DEFAULT_ADMIN_ID, UserConstants.DEFAULT_ADMIN_ID.toCharArray()));

        if (!(adminSession instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        userMgr = ((JackrabbitSession) adminSession).getUserManager();

        preTestAuthorizables.clear();
        Iterator<Authorizable> iter = userMgr.findAuthorizables("rep:principalName", null);
        while (iter.hasNext()) {
            String id = iter.next().getID();
            preTestAuthorizables.add(id);
        }

        // make sure the target node for group-import exists
        Authorizable administrators = userMgr.getAuthorizable(ADMINISTRATORS);
        if (userMgr.getAuthorizable(ADMINISTRATORS) == null) {
            userMgr.createGroup(new PrincipalImpl(ADMINISTRATORS));
        } else if (!administrators.isGroup()) {
            throw new NotExecutableException("Expected " + administrators.getID() + " to be a group.");
        }
        adminSession.save();
    }

    @After
    public void after() throws Exception {
        try {
            adminSession.refresh(false);
            if (userMgr.isAutoSave()) {
                try {
                    userMgr.autoSave(false);
                } catch (Exception e) {
                    // ignore
                }
            }

            Iterator<Authorizable> iter = userMgr.findAuthorizables("rep:principalName", null);
            while (iter.hasNext()) {
                String id = iter.next().getID();
                if (!preTestAuthorizables.remove(id)) {
                    try {
                        userMgr.getAuthorizable(id).remove();
                    } catch (RepositoryException e) {
                        // ignore
                        System.out.println("error removing " + id + ":" + e);
                    }
                }
            }
            adminSession.save();
        } finally {
            adminSession.logout();
            repo = dispose(repo);
        }
    }

    @Nullable
    protected ConfigurationParameters getConfigurationParameters() {
        String importBehavior = getImportBehavior();
        if (importBehavior != null) {
            Map<String, String> userParams = new HashMap<>();
            userParams.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, importBehavior);
            return ConfigurationParameters.of(UserConfiguration.NAME, ConfigurationParameters.of(userParams));
        } else {
            return null;
        }
    }

    @Nullable
    protected abstract String getImportBehavior();

    @NotNull
    protected abstract String getTargetPath();

    @NotNull
    protected Session getImportSession() {
        return adminSession;
    }

    @NotNull
    protected UserManager getUserManager() throws RepositoryException {
        return ((JackrabbitSession) getImportSession()).getUserManager();
    }

    @NotNull
    protected Node getTargetNode() throws RepositoryException {
        return getImportSession().getNode(getTargetPath());
    }

    @NotNull
    protected String getExistingUUID() throws RepositoryException {
        Node n = adminSession.getRootNode();
        n.addMixin(JcrConstants.MIX_REFERENCEABLE);
        //noinspection deprecation
        return n.getUUID();
    }

    protected void doImport(@NotNull String parentPath, @NotNull String xml) throws Exception {
        doImport(parentPath, xml, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    protected void doImport(@NotNull String parentPath, @NotNull String xml, int importUUIDBehavior) throws Exception {
        doImport(getImportSession(), parentPath, xml, importUUIDBehavior);
    }

    protected void doImport(@NotNull Session importSession, @NotNull String parentPath, @NotNull String xml, int importUUIDBehavior) throws Exception {
        InputStream in;
        if (xml.charAt(0) == '<') {
            in = new ByteArrayInputStream(xml.getBytes());
            // uncomment to dump include XMLs
            // FileOutputStream out = new FileOutputStream(getTestXml());
            // out.write(xml.getBytes());
            // out.close();
        } else {
            in = getClass().getResourceAsStream(xml);
        }
        try {
            importSession.importXML(parentPath, in, importUUIDBehavior);
        } finally {
            in.close();
        }
    }

    protected static void assertNotDeclaredMember(@NotNull Group gr, @NotNull String potentialID, @NotNull Session session) throws RepositoryException {
        // declared members must not list the invalid entry.
        Iterator<Authorizable> it = gr.getDeclaredMembers();
        while (it.hasNext()) {
            Authorizable member = it.next();
            assertNotEquals(potentialID, session.getNode(member.getPath()).getIdentifier());
        }
    }
}
