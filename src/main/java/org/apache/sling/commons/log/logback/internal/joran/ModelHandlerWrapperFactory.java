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

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.jetbrains.annotations.NotNull;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.processor.AppenderRefModelHandler;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;

/**
 * A factory helper to create model handlers for the custom logback xml
 * configuration loading
 */
class ModelHandlerWrapperFactory {

    private LogConfigManager logConfigManager;

    /**
     * Constructor
     *
     * @param manager the LogConfigManager that created the factory
     */
    public ModelHandlerWrapperFactory(@NotNull LogConfigManager manager) {
        this.logConfigManager = manager;
    }

    /**
     * Creates a custom AppenderRefModelHandler wrapper that will track which
     * appenders are referenced by which logger
     *  
     * @param context the logback context
     * @param mic the interpretation context
     * @return the model handler for appender refs
     */
    public @NotNull AppenderRefModelHandler makeAppenderRefModelHandlerInstance(@NotNull Context context,
            @NotNull ModelInterpretationContext mic) {
        return new AppenderRefModelHandlerWrapper(context, logConfigManager);
    }

    /**
     * Creates a custom OsgiAppenderRefModelHandler that will track which
     * osgi appenders are referenced by which logger
     *  
     * @param context the logback context
     * @param mic the interpretation context
     * @return the model handler for osgi appender refs
     */
    public @NotNull OsgiAppenderRefModelHandler makeOsgiAppenderRefModelHandlerInstance(@NotNull Context context,
            @NotNull ModelInterpretationContext mic) {
        return new OsgiAppenderRefModelHandler(context, logConfigManager);
    }

}
