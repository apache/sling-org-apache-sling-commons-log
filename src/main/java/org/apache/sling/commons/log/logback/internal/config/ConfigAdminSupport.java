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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Helper to handle registration of the configurable
 * logging services
 *
 */
public class ConfigAdminSupport {

    private ServiceRegistration<?> loggingConfigurable;
    private ServiceRegistration<?> writerConfigurer;
    private ServiceRegistration<?> configConfigurer;

    /**
     * Register the configurable logging services
     *
     * @param context the bundle context
     * @param logConfigManager the log config manager
     */
    public void start(@NotNull BundleContext context, @NotNull LogConfigManager logConfigManager) {
        // prepare registration properties (will be reused)
        Dictionary<String, String> props = new Hashtable<>(); // NOSONAR
        props.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);

        // register for official (global) configuration now
        props.put(Constants.SERVICE_PID, LogConstants.PID);
        props.put(Constants.SERVICE_DESCRIPTION, "LogManager Configuration Admin support");
        loggingConfigurable = context.registerService(
                "org.osgi.service.cm.ManagedService",
                new ConfigurationServiceFactory<>(logConfigManager, GlobalConfigurator::new),
                props);

        // register for log writer configuration
        ConfigurationServiceFactory<LogWriterManagedServiceFactory> msf =
                new ConfigurationServiceFactory<>(logConfigManager, LogWriterManagedServiceFactory::new);
        props.put(Constants.SERVICE_PID, LogConstants.FACTORY_PID_WRITERS);
        props.put(Constants.SERVICE_DESCRIPTION, "LogWriter configurator");
        writerConfigurer = context.registerService("org.osgi.service.cm.ManagedServiceFactory", msf, props);

        // register for log configuration
        ConfigurationServiceFactory<LoggerManagedServiceFactory> msf2 =
                new ConfigurationServiceFactory<>(logConfigManager, LoggerManagedServiceFactory::new);
        props.put(Constants.SERVICE_PID, LogConstants.FACTORY_PID_CONFIGS);
        props.put(Constants.SERVICE_DESCRIPTION, "Logger configurator");
        configConfigurer = context.registerService("org.osgi.service.cm.ManagedServiceFactory", msf2, props);
    }

    /**
     * Unregister the configurable logging services
     */
    public void stop() {
        if (loggingConfigurable != null) {
            loggingConfigurable.unregister();
            loggingConfigurable = null;
        }

        if (writerConfigurer != null) {
            writerConfigurer.unregister();
            writerConfigurer = null;
        }

        if (configConfigurer != null) {
            configConfigurer.unregister();
            configConfigurer = null;
        }
    }
}
