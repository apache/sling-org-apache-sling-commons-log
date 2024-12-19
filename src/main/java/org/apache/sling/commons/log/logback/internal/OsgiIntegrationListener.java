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
package org.apache.sling.commons.log.logback.internal;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.util.ContextUtil;

/**
 * Apply the OSGi configuration when the logback context is reset
 */
class OsgiIntegrationListener implements LoggerContextListener {
    /**
     * Reference to the calling LogConfigManager
     */
    private final LogConfigManager logConfigManager;

    /**
     * Reference to helper for reporting status
     */
    private final ContextUtil contextUtil;

    /**
     * Constructor
     *
     * @param logConfigManager the log config manager this was created by
     */
    OsgiIntegrationListener(@NotNull LogConfigManager logConfigManager) {
        this.logConfigManager = logConfigManager;
        this.contextUtil = new ContextUtil((Context)LoggerFactory.getILoggerFactory());
    }

    @Override
    public boolean isResetResistant() {
        // The integration listener has to survive resets from other causes
        // like reset when Logback detects change in config file and reloads
        // on its own in ReconfigureOnChangeFilter
        return true;
    }

    @Override
    public void onStart(@NotNull LoggerContext context) {
        // no-op
    }

    @Override
    public void onReset(@NotNull LoggerContext context) {
        contextUtil.addInfo("OsgiIntegrationListener : context reset detected. Adding LogManager to context map and firing"
                + " listeners");

        context.setPackagingDataEnabled(false);
        context.setMaxCallerDataDepth(logConfigManager.getMaxCallerDataDepth());
        logConfigManager.registerPackageInfoCollector();

        // Attach a console appender to handle logging until we configure
        // one. This would be removed in RootLoggerListener.reset
        final Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(logConfigManager.getDefaultAppender());

        // Now record the time of reset with a default appender attached to
        // root logger. We also add a milli second extra to account for logs which would have
        // got fired in same duration
        logConfigManager.updateResetStartTime();
        contextUtil.addInfo("Registered a default console based logger");

        context.putObject(LogConfigManager.class.getName(), logConfigManager);
        logConfigManager.fireResetStartListeners();
    }

    @Override
    public void onStop(@NotNull LoggerContext context) {
        // no-op
    }

    @Override
    public void onLevelChange(@NotNull Logger logger, @NotNull Level level) {
        // no-op
    }

}
