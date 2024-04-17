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
package org.apache.sling.commons.log.logback.spi;

import java.net.URL;

import org.jetbrains.annotations.NotNull;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Implementation to block the original logback configuration handling
 */
@ConfiguratorRank(value = ConfiguratorRank.DEFAULT)
public class DefaultConfigurator extends ContextAwareBase implements Configurator {

    /**
     * Callback to configure the logger context
     *
     * @param context the logger context to configure
     */
    @Override
    public @NotNull ExecutionStatus configure(@NotNull LoggerContext context) {
        addInfo("Setting up default configuration.");

        // load the XML file
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            // start with basic console output configuration
            configurator.doConfigure(getConfigResource());
        } catch (JoranException e) {
            e.printStackTrace();
        }

        // don't fall through to the original handlers
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    /**
     * Return the URL for the configuration resource
     *
     * @return the URL of the configuration resource
     */
    protected @NotNull URL getConfigResource() {
        return getClass().getResource("/logback.xml");
    }

}
