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
package org.apache.sling.commons.log.logback.internal;

import java.util.List;
import java.util.function.Supplier;

import org.apache.sling.commons.log.logback.internal.util.SlingContextUtil;
import org.jetbrains.annotations.NotNull;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAware;

/**
 * It checks if LevelChangePropagator is installed or not. If not then
 * it installs the propagator when Slf4j Bridge Handler is installed
 */
class LevelChangePropagatorChecker implements LogbackResetListener {

    /**
     * supplier that informs if the bridge handler was installed
     */
    private Supplier<Boolean> isBridgeInstalledSupplier;

    /**
     * Constructor
     *
     * @param isBridgeInstalledSupplier supplier fn that returns whether
     *          the bride handler was installed
     */
    LevelChangePropagatorChecker(@NotNull Supplier<Boolean> isBridgeInstalledSupplier) {
        this.isBridgeInstalledSupplier = isBridgeInstalledSupplier;
    }

    /**
     * Callback after the reset is completed
     * 
     * @param context the logger context being reset
     */
    @Override
    public void onResetComplete(@NotNull LoggerContext context) {
        List<LoggerContextListener> listenerList = context.getCopyOfListenerList();
        boolean levelChangePropagatorInstalled = listenerList.stream()
            .anyMatch(LevelChangePropagator.class::isInstance);

        //http://logback.qos.ch/manual/configuration.html#LevelChangePropagator
        if (!levelChangePropagatorInstalled &&
                Boolean.TRUE.equals(isBridgeInstalledSupplier.get())) {
            LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
            levelChangePropagator.setContext(context);
            levelChangePropagator.start();
            context.addListener(levelChangePropagator);
            ContextAware c = new SlingContextUtil(context, this);
            c.addInfo("Slf4j bridge handler found to be enabled. Installing the LevelChangePropagator");
        }
    }

}
