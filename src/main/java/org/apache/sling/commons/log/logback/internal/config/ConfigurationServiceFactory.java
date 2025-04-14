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
package org.apache.sling.commons.log.logback.internal.config;

import java.util.function.Supplier;

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Factory of logging configuration services
 *
 * @param <S> the type of LogConfigurator provided by the factory
 */
public class ConfigurationServiceFactory<S extends LogConfigurator> implements ServiceFactory<S> {

    private final LogConfigManager logConfigManager;
    private final Supplier<S> svcSupplier;
    private int useCount;
    private S service;

    /**
     * Constructor
     *
     * @param mgr the log configuration manager
     * @param svcSupplier the supplier that creates the service instance
     */
    public ConfigurationServiceFactory(@NotNull LogConfigManager mgr, @NotNull Supplier<S> svcSupplier) {
        this.logConfigManager = mgr;
        this.svcSupplier = svcSupplier;
    }

    /**
     * Get or create the service for the supplied registration. The useCount is
     * incremented for each call.
     *
     * @return the service for the registration
     */
    @Override
    public @NotNull S getService(@NotNull Bundle bundle, @NotNull ServiceRegistration<S> registration) {
        if (service == null) {
            useCount = 1;
            service = svcSupplier.get();
            service.setLogConfigManager(logConfigManager);
        } else {
            useCount++;
        }
        return service;
    }

    /**
     * Unget the service for the supplied registration. This decrements the useCount
     * and if that reaches zero then the service object is disposed
     */
    @Override
    public void ungetService(@NotNull Bundle bundle, @NotNull ServiceRegistration<S> registration, @NotNull S svc) {
        useCount--;
        if (useCount <= 0) {
            service = null;
            // reset in case the value has gone below zero
            useCount = 0;
        }
    }
}
