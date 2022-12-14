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
package org.apache.jackrabbit.oak.spi.security.authorization.cug.impl;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.nodetype.TypePredicate;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.DefaultValidator;
import org.apache.jackrabbit.oak.spi.commit.Validator;
import org.apache.jackrabbit.oak.spi.commit.ValidatorProvider;
import org.apache.jackrabbit.oak.spi.commit.VisibleValidator;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStateUtils;
import org.jetbrains.annotations.NotNull;

import static org.apache.jackrabbit.JcrConstants.JCR_SYSTEM;
import static org.apache.jackrabbit.oak.api.CommitFailedException.ACCESS_CONTROL;
import static org.apache.jackrabbit.oak.spi.nodetype.NodeTypeConstants.JCR_NODE_TYPES;

class CugValidatorProvider extends ValidatorProvider implements CugConstants {

    private TypePredicate isMixCug;

    @Override
    protected Validator getRootValidator(NodeState before, NodeState after, CommitInfo info) {
        this.isMixCug = new TypePredicate(after, MIX_REP_CUG_MIXIN);
        return new CugValidator("", after, false);
    }

    private static CommitFailedException accessViolation(int code, String message) {
        return new CommitFailedException(ACCESS_CONTROL, code, message);
    }

    private static boolean isNodetypeTree(CugValidator parentValidator, String name) {
        if (parentValidator.isNodetypeTree) {
            return true;
        } else {
            return JCR_NODE_TYPES.equals(name) && JCR_SYSTEM.equals(parentValidator.parentName);
        }
    }

    private final class CugValidator extends DefaultValidator {

        private final NodeState parentAfter;
        private final String parentName;
        private final boolean isNodetypeTree;

        private CugValidator(@NotNull String parentName, @NotNull NodeState parentAfter, boolean isNodetypeTree) {
            this.parentAfter = parentAfter;
            this.parentName = parentName;
            this.isNodetypeTree = isNodetypeTree;
        }

        //------------------------------------------------------< Validator >---
        @Override
        public void propertyAdded(PropertyState after) throws CommitFailedException {
            String name = after.getName();
            if (JcrConstants.JCR_PRIMARYTYPE.equals(name)) {
                if (NT_REP_CUG_POLICY.equals(after.getValue(Type.STRING)) && !REP_CUG_POLICY.equals(parentName)) {
                    throw accessViolation(23, "Attempt create Cug node with different name than 'rep:cugPolicy'.");
                }
            }
        }

        @Override
        public void propertyChanged(PropertyState before, PropertyState after) throws CommitFailedException {
            String name = after.getName();
            if (JcrConstants.JCR_PRIMARYTYPE.equals(name)) {
                if (NT_REP_CUG_POLICY.equals(before.getValue(Type.STRING)) || NT_REP_CUG_POLICY.equals(after.getValue(Type.STRING))) {
                    throw accessViolation(20, "Attempt to change primary type of/to CUG policy.");
                }
            }
        }

        @Override
        public Validator childNodeAdded(String name, NodeState after) throws CommitFailedException {
            if (!isNodetypeTree && REP_CUG_POLICY.equals(name)) {
                validateCugNode(parentAfter, after);
            }
            return new VisibleValidator(new CugValidator(name, after, isNodetypeTree(this, name)), true, true);
        }

        @Override
        public Validator childNodeChanged(String name, NodeState before, NodeState after) throws CommitFailedException {
            if (!isNodetypeTree && after.hasChildNode(REP_CUG_POLICY)) {
                validateCugNode(after, after.getChildNode(REP_CUG_POLICY));
            }
            return new VisibleValidator(new CugValidator(name, after, isNodetypeTree(this, name)), true, true);
        }

        private void validateCugNode(@NotNull NodeState parent, @NotNull NodeState nodeState) throws CommitFailedException {
            if (!NT_REP_CUG_POLICY.equals(NodeStateUtils.getPrimaryTypeName(nodeState))) {
                throw accessViolation(21, "Reserved name 'rep:cugPolicy' must only be used for nodes of type 'rep:CugPolicy'.");
            }
            if (!isMixCug.test(parent)) {
                throw accessViolation(22, "Parent node not of mixin type 'rep:CugMixin'.");
            }
        }
    }
}
