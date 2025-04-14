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
package org.apache.sling.commons.log.logback;

import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.action.PreconditionValidator;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;
import org.xml.sax.Attributes;

/**
 * Joran action enabling integration between OSGi and Logback. It is based on
 * {@link ch.qos.logback.core.joran.action.IncludeAction}. It supports including
 * config fragments provided through OSGi ServiceRegistry
 *
 * The action class needs to be referred in external files hence adding a
 * class in public package.
 * <p>
 * This class is for configuration reference only. Consumers are not intended to
 * instantiate or extend from it.
 */
@ProviderType
public final class OsgiAppenderRefAction extends BaseModelAction {

    /**
     * Validate preconditions of this action.
     *
     * @param interpretationContext the Sax event interpretation context
     * @param name the element name
     * @param attributes the attributes for the element
     * @return true if valid, false otherwise
     */
    @Override
    protected boolean validPreconditions(
            @NotNull SaxEventInterpretationContext interpretationContext,
            @NotNull String name,
            @NotNull Attributes attributes) {
        PreconditionValidator pv = new PreconditionValidator(this, interpretationContext, name, attributes);
        pv.validateRefAttribute();
        return pv.isValid();
    }

    /**
     * Build the OsgiAppenderRefModel for the given inputs
     *
     * @param interpretationContext the Sax event interpretation context
     * @param name the element name
     * @param attributes the attributes for the element
     * @return the built model
     */
    @Override
    protected Model buildCurrentModel(
            @NotNull SaxEventInterpretationContext interpretationContext,
            @NotNull String name,
            @NotNull Attributes attributes) {
        OsgiAppenderRefModel arm = new OsgiAppenderRefModel();
        String ref = attributes.getValue(JoranConstants.REF_ATTRIBUTE);
        arm.setRef(ref);
        return arm;
    }
}
