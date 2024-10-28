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
package org.apache.sling.commons.log.logback.internal.joran;

import org.apache.sling.commons.log.logback.OsgiAction;
import org.apache.sling.commons.log.logback.OsgiAppenderRefAction;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.jetbrains.annotations.NotNull;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;

/**
 * Wrap the original JoronConfigurator to provide tracking of
 * the appender-ref that happen during processing
 */
public class JoranConfiguratorWrapper extends JoranConfigurator {

    private LogConfigManager logConfigManager;

    /**
     * Constructor
     *
     * @param manager the LogConfigManager that created the wrapper
     */
    public JoranConfiguratorWrapper(@NotNull LogConfigManager manager) {
        this.logConfigManager = manager;
    }

    /**
     * Override to allow the LogConfigManager to contribute substitution properties
     */
    @Override
    public void buildModelInterpretationContext() {
        super.buildModelInterpretationContext();
        logConfigManager.addSubsitutionProperties(modelInterpretationContext);

    }

    /**
     * Subclass to provide a custom wrapper around the AppenderRef handling
     * so we can keep track of what appenders were added to which loggers
     */
    @Override
    protected void addModelHandlerAssociations(@NotNull DefaultProcessor defaultProcessor) {
        super.addModelHandlerAssociations(defaultProcessor);

        ModelHandlerWrapperFactory factory = new ModelHandlerWrapperFactory(logConfigManager);
        defaultProcessor.addHandler(AppenderRefModel.class, factory::makeAppenderRefModelHandlerInstance);
        defaultProcessor.addHandler(OsgiAppenderRefModel.class, factory::makeOsgiAppenderRefModelHandlerInstance);
        defaultProcessor.addHandler(OsgiModel.class, OsgiModelHandler::makeInstance);
    }

    @Override
    public void addElementSelectorAndActionAssociations(RuleStore rs) {
        super.addElementSelectorAndActionAssociations(rs);

        // TODO: add handler for the JMX handler to warn about it not being supported anymore?
        rs.addRule(new ElementSelector("*/configuration/osgi"), OsgiAction::new);
        rs.addRule(new ElementSelector("*/configuration/appender-ref-osgi"), OsgiAppenderRefAction::new);
    }

}
