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
package org.apache.jackrabbit.oak.segment.standby;

import java.io.File;

import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.test.TemporaryBlobStore;
import org.apache.jackrabbit.oak.segment.test.TemporaryFileStore;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public class ExternalPrivateStoreIT extends DataStoreTestBase {

    private TemporaryFolder folder = new TemporaryFolder(new File("target"));

    private TemporaryBlobStore serverBlobStore = new TemporaryBlobStore(folder, "server-blob-store");

    private TemporaryFileStore serverFileStore = new TemporaryFileStore(folder, serverBlobStore, false, "server-file-store");

    private TemporaryBlobStore clientBlobStore = new TemporaryBlobStore(folder, "client-blob-store");

    private TemporaryFileStore clientFileStore = new TemporaryFileStore(folder, clientBlobStore, true, "client-file-store");

    @Rule
    public RuleChain chain = RuleChain.outerRule(folder)
            .around(serverBlobStore)
            .around(serverFileStore)
            .around(clientBlobStore)
            .around(clientFileStore);

    @Override
    FileStore getPrimary() {
        return serverFileStore.fileStore();
    }

    @Override
    FileStore getSecondary() {
        return clientFileStore.fileStore();
    }

    @Override
    boolean storesShouldBeDifferent() {
        return true;
    }

}
