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
package org.apache.jackrabbit.oak.jcr.xml;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;


public class ImportTest extends AbstractJCRTest {

    private String uuid;
    private String path;
    private String siblingPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Node node = testRootNode.addNode(nodeName1);
        node.addMixin(mixReferenceable);

        Node sibling = testRootNode.addNode(nodeName2);

        uuid = node.getIdentifier();
        path = node.getPath();
        siblingPath = sibling.getPath();
    }

    private InputStream getImportStream() throws RepositoryException, IOException {
        return getImportStream(path);
    }

    private InputStream getImportStream(String path) throws RepositoryException, IOException {
        OutputStream out = new ByteArrayOutputStream();
        superuser.exportSystemView(path, out, true, false);
        return new ByteArrayInputStream(out.toString().getBytes());
    }

    public void testReplaceUUID() throws Exception {
        superuser.save();

        superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        superuser.save();

        // original node must have been replaced (but no child node added)
        assertTrue(testRootNode.hasNode(nodeName1));
        Node n2 = testRootNode.getNode(nodeName1);
        assertTrue(n2.isNodeType(mixReferenceable));
        assertEquals(uuid, n2.getIdentifier());

        Node sibling = superuser.getNode(siblingPath);
        assertFalse(sibling.hasNode(nodeName1));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-9541">OAK-9541</a>
     * @throws Exception
     *
     */
    public void testOverwriteDifferentUUIDSamePath() throws Exception {
        // export of node1/node1 (nt:folder in nt:folder, does not allow sns)
        superuser.removeItem(path);
        Node node = testRootNode.addNode(nodeName1, JcrConstants.NT_FOLDER);
        Node subNode = node.addNode(nodeName1, JcrConstants.NT_FOLDER);
        subNode.addMixin(mixReferenceable);
        superuser.save();
        uuid = subNode.getIdentifier();
        InputStream sysViewInputStream = getImportStream(subNode.getPath());
        sysViewInputStream.mark(Integer.MAX_VALUE);

        // replace uuid of node1/node1
        superuser.removeItem(subNode.getPath());
        subNode = node.addNode(nodeName1, JcrConstants.NT_FOLDER);
        subNode.addMixin(mixReferenceable);
        assertNotEquals(uuid, subNode.getIdentifier());
        superuser.save();

        // reimport node1/node1 must throw exception
        ItemExistsException e = assertThrows(ItemExistsException.class, () -> { superuser.importXML(node.getPath(), sysViewInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING); });
        assertFalse(e.getMessage().contains("UUID"));
        sysViewInputStream.reset();
        e = assertThrows(ItemExistsException.class, () -> { superuser.importXML(node.getPath(), sysViewInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING); });
        assertFalse(e.getMessage().contains("UUID"));
        sysViewInputStream.reset();
        e = assertThrows(ItemExistsException.class, () -> { superuser.importXML(node.getPath(), sysViewInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW); });
        assertFalse(e.getMessage().contains("UUID"));
        sysViewInputStream.reset();
        e = assertThrows(ItemExistsException.class, () -> { superuser.importXML(node.getPath(), sysViewInputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW); });
        assertFalse(e.getMessage().contains("UUID"));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2246">OAK-2246</a>
     */
    public void testTransientReplaceUUID() throws Exception {
        superuser.importXML(path, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        superuser.save();

        // original node must have been replaced (but no child node added)
        superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        superuser.save();

        // original node must have been replaced (but no child node added)
        assertTrue(testRootNode.hasNode(nodeName1));
        Node n2 = testRootNode.getNode(nodeName1);
        assertTrue(n2.isNodeType(mixReferenceable));
        assertEquals(uuid, n2.getIdentifier());

        Node sibling = superuser.getNode(siblingPath);
        assertFalse(sibling.hasNode(nodeName1));
    }

    public void testReplaceUUIDSameTree() throws Exception {
        superuser.save();

        superuser.importXML(path, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        superuser.save();

        // original node must have been replaced (but no child node added)
        assertTrue(testRootNode.hasNode(nodeName1));
        Node n2 = testRootNode.getNode(nodeName1);
        assertTrue(n2.isNodeType(mixReferenceable));
        assertEquals(uuid, n2.getIdentifier());
        assertFalse(n2.hasNode(nodeName1));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2246">OAK-2246</a>
     */
    public void testTransientReplaceUUIDSameTree() throws Exception {
        superuser.importXML(path, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        superuser.save();

        // original node must have been replaced (but no child node added)
        assertTrue(testRootNode.hasNode(nodeName1));
        Node n2 = testRootNode.getNode(nodeName1);
        assertTrue(n2.isNodeType(mixReferenceable));
        assertEquals(uuid, n2.getIdentifier());
        assertFalse(n2.hasNode(nodeName1));
    }

    public void testRemoveUUID() throws Exception {
        superuser.save();

        superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
        superuser.save();

        // original node must have been removed
        assertFalse(testRootNode.hasNode(nodeName1));

        Node sibling = superuser.getNode(siblingPath);
        assertTrue(sibling.hasNode(nodeName1));

        Node imported = sibling.getNode(nodeName1);
        assertTrue(imported.isNodeType(mixReferenceable));
        assertEquals(uuid, imported.getIdentifier());
    }

    public void testTransientRemoveUUID() throws Exception {
        superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
        superuser.save();

        // original node must have been removed
        assertFalse(testRootNode.hasNode(nodeName1));

        Node sibling = superuser.getNode(siblingPath);
        assertTrue(sibling.hasNode(nodeName1));

        Node imported = sibling.getNode(nodeName1);
        assertTrue(imported.isNodeType(mixReferenceable));
        assertEquals(uuid, imported.getIdentifier());
    }

    public void testRemoveUUIDSameTree() throws Exception {
        superuser.save();

        try {
            superuser.importXML(path, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
            fail("ConstraintViolationException expected");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    public void testTransientRemoveUUIDSameTree() throws Exception {
        try {
            superuser.importXML(path, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
            fail("ConstraintViolationException expected");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    public void testCreateNewUUID() throws Exception {
        superuser.save();

        superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        superuser.save();

        // original node must still exist
        assertTrue(testRootNode.hasNode(nodeName1));

        // verify the import produced the expected new node
        Node sibling = superuser.getNode(siblingPath);
        assertTrue(sibling.hasNode(nodeName1));

        Node imported = sibling.getNode(nodeName1);
        assertTrue(imported.isNodeType(mixReferenceable));
        assertFalse(uuid.equals(imported.getIdentifier()));
    }

    public void testTransientCreateNewUUID() throws Exception {
        superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        superuser.save();

        // original node must still exist
        assertTrue(testRootNode.hasNode(nodeName1));

        // verify the import produced the expected new node
        Node sibling = superuser.getNode(siblingPath);
        assertTrue(sibling.hasNode(nodeName1));

        Node imported = sibling.getNode(nodeName1);
        assertTrue(imported.isNodeType(mixReferenceable));
        assertFalse(uuid.equals(imported.getIdentifier()));
    }

    public void testThrow() throws Exception {
        superuser.save();

        try {
            superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            fail("ItemExistsException expected");
        } catch (ItemExistsException e) {
            // success
        }
    }

    public void testTransientThrow() throws Exception {
        try {
            superuser.importXML(siblingPath, getImportStream(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            fail("ItemExistsException expected");
        } catch (ItemExistsException e) {
            // success
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-8212">OAK-8212</a>
     */
    public void testNoMatchingPropertyDefinition() throws Exception {
        // jcr:data must be BINARY not BOOLEAN -> should fail
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<sv:node sv:name=\"resourceName\" xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:fn_old=\"http://www.w3.org/2004/10/xpath-functions\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:rep=\"internal\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\">" +
                    "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>oak:Resource</sv:value></sv:property>" +
                    "<sv:property sv:name=\"jcr:data\" sv:type=\"Boolean\"><sv:value>true</sv:value></sv:property>" +
                "</sv:node>";
        try {
            superuser.importXML(path, new ByteArrayInputStream(xml.getBytes()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            fail("ConstraintViolationException expected");
        } catch (ConstraintViolationException e) {
            // success
            assertEquals("No matching property definition found for jcr:data", e.getMessage());
        }
    }

    public void testNewNamespaceWithPrefixConflict() throws Exception {
        String ns1 = "urn:uuid:" + UUID.randomUUID().toString();
        String ns2 = "urn:uuid:" + UUID.randomUUID().toString();
        String xml = "<sv:node sv:name='resourceName' xmlns:sv='http://www.jcp.org/jcr/sv/1.0'>" + "<sv:property xmlns:foo='" + ns1
                + "' sv:name='foo:test' sv:type='String'><sv:value>a</sv:value></sv:property>" + "<sv:property xmlns:foo='" + ns2
                + "' sv:name='foo:test' sv:type='String'><sv:value>b</sv:value></sv:property>" + "</sv:node>";
        superuser.importXML(path, new ByteArrayInputStream(xml.getBytes()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        Node n = superuser.getNode(path);
        n = n.getNode("resourceName");
        Property p1 = n.getProperty("{" + ns1 + "}test");
        assertEquals("a", p1.getString());
        Property p2 = n.getProperty("{" + ns2 + "}test");
        assertEquals("b", p2.getString());
        assertNotEquals(p1.getName(), p2.getName());
    }
}