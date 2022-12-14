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

package org.apache.jackrabbit.oak.plugins.document;

import javax.management.openmbean.CompositeData;

import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;

@SuppressWarnings("UnusedDeclaration")
public interface DocumentNodeStoreMBean {
    String TYPE = "DocumentNodeStore";

    @Deprecated
    String getRevisionComparatorState();

    String getHead();

    int getClusterId();

    int getUnmergedBranchCount();

    String[] getInactiveClusterNodes();

    String[] getActiveClusterNodes();

    String[] getLastKnownRevisions();

    String formatRevision(@Name("revision") String rev, @Name("UTC")boolean utc);

    @Description("Return the estimated time difference in milliseconds between\n" +
        "the local instance and the (typically common, shared) document server system.\n" +
        "The value can be zero if the times are estimated to be equal,\n" +
        "positive when the local instance is ahead of the remote server\n" +
        "and negative when the local instance is behind the remote server. An invocation is not cached\n" +
        "and typically requires a round-trip to the server (but that is not a requirement).")
    long determineServerTimeDifferenceMillis();

    CompositeData getMergeSuccessHistory();

    CompositeData getMergeFailureHistory();

    CompositeData getExternalChangeCountHistory();

    CompositeData getBackgroundUpdateCountHistory();

    CompositeData getBranchCommitHistory();

    CompositeData getMergeBranchCommitHistory();

    @Description("Triggers last revision recovery of nodes, below a given path and clusterId.\n" +
            "Returns number of records updated after performing recovery.\n" +
            "Note: Recovery can only be performed on inactive clusterIds. If the clusterNode is in ReadOnly mode,\n" +
            "it will return the no. of documents which needs update and won't perform recovery")
    int recover(
            @Description("The absolute path of a node.")
            @Name("path")
            String path, 
            @Description("The id of an inactive cluster node.")
            @Name("clusterId")
            int clusterId);

    @Description("Invalidates all the caches used by the DocumentNodeStore and DocumentStore: DiffCache, NodeCache, NodeChildrenCache and DocumentCache.")
    String cleanAllCaches();

    @Description("Invalidates the specified cache: DiffCache, NodeCache, NodeChildrenCache or DocumentCache.\n" +
            "Note: The values are case insensitive.")
    String cleanIndividualCache(
            @Description("Possible values are: DIFF, NODE, NODECHILDREN, DOCUMENT")
            @Name("name")
            String name);

    @Description("Creates a checkpoint with the given revision and lifetime.")
    String createCheckpoint(
            @Description("The revision of the checkpoint to create.")
            @Name("revision")
            String revision,
            @Description("The lifetime of the checkpoint in milliseconds.")
            @Name("lifetime")
            long lifetime,
            @Description("Force create the checkpoint even when the revision " +
                    "garbage collector may have cleaned up changes already " +
                    "that are more recent than the revision checkpoint.")
            @Name("force")
            boolean force);
}
