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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LogbackServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactoryFriend;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Track the arrival of new SLF4JServiceProvider components to know when to retry
 * the LogConfigManager initialization
 */
final class SLF4JServiceProviderTracker extends ServiceTracker<SLF4JServiceProvider, SLF4JServiceProvider> {

    /**
     * The current log config manager (if started)
     */
    protected @Nullable LogConfigManager logConfigManager;

    /**
     * Constructor
     *
     * @param bundleContext the bundle context the tracker is for
     */
    SLF4JServiceProviderTracker(@NotNull BundleContext bundleContext) {
        super(bundleContext, SLF4JServiceProvider.class, null);
    }

    /**
     * Callback when the specified service is added
     *
     * @param reference the service reference for the removed service
     * @return the service instance being added
     */
    @Override
    public @NotNull SLF4JServiceProvider addingService(@NotNull ServiceReference<SLF4JServiceProvider> reference) {
        SLF4JServiceProvider service = super.addingService(reference);
        if (service instanceof LogbackServiceProvider) {
            if (!isSlf4jInitialized()) {
                // WARNING - This "friend" class is declared as reserved for internal use, but
                //   there doesn't appear to be any better alternative to re-calculate the factory
                LoggerFactoryFriend.reset();

                // check the factory impl after the reset
                ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
                if (iLoggerFactory instanceof LoggerContext) {
                    // got the Logback impl after the reset, so we should be good to go
                    LoggerFactory.getLogger(getClass())
                            .info("Slf4j is initialized with the logback impl after the reset");
                } else {
                    // still no good
                    LoggerFactory.getLogger(getClass())
                            .error("Slf4j is not initialized with the logback impl after the reset");
                }
            }

            if (!isSlf4jInitialized()) {
                LoggerFactory.getLogger(getClass())
                        .info("Slf4j is not initialized with the logback impl so skipping customization");
            } else {
                // looks like we have the right impl now, so start our custom LogConfigManager
                logConfigManager = new LogConfigManager(context);
                logConfigManager.start();
            }
        } else {
            LoggerFactory.getLogger(getClass())
                    .debug(
                            "addingService is not the LogbackServiceProvider: {}",
                            service.getClass().getName());
        }

        return service;
    }

    /**
     * Callback when the specified service is removed
     *
     * @param reference the service reference for the removed service
     * @param the service instance being removed
     */
    @Override
    public void removedService(
            @NotNull ServiceReference<SLF4JServiceProvider> reference, @NotNull SLF4JServiceProvider service) {
        if (service instanceof LogbackServiceProvider) {
            if (logConfigManager != null) {
                logConfigManager.stop();
                logConfigManager = null;
            }
        } else {
            LoggerFactory.getLogger(getClass())
                    .debug(
                            "removedService is not the LogbackServiceProvider: {}",
                            service.getClass().getName());
        }
    }

    /**
     * Returns if the current SFL4J LoggerFactory is the logback implementation
     *
     * @return true if the SFL4J LoggerFactory is the logback implementation
     */
    private boolean isSlf4jInitialized() {
        return LoggerFactory.getILoggerFactory() instanceof LoggerContext;
    }
}
