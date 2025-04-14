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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * Factory for non-global logging configuration services
 */
public class LoggerManagedServiceFactory extends LogConfigurator implements ManagedServiceFactory {

    /**
     * The default file name when a value is not supplied in the configuration
     */
    public static final String LOG_FILE_DEFAULT = "logs/error.log";

    /**
     * Descriptive name of this factory
     *
     * @return the name for the factory
     */
    public @NotNull String getName() {
        return "Logger configurator";
    }

    /**
     * Update the logger configuration for the supplied configuration service
     *
     * @param pid the service identifier for the service
     * @param configuration the configuration properties to apply
     */
    public void updated(@NotNull String pid, @NotNull Dictionary<String, ?> configuration)
            throws org.osgi.service.cm.ConfigurationException {
        try {
            Dictionary<String, ?> conf = configuration;

            // calculate a filename if one is not supplied
            if (configuration.get(LogConstants.LOG_FILE) == null) {
                List<String> keys = Collections.list(configuration.keys());
                Map<String, Object> confCopy =
                        keys.stream().collect(Collectors.toMap(Function.identity(), configuration::get));
                confCopy.put(LogConstants.LOG_FILE, LOG_FILE_DEFAULT);
                conf = new Hashtable<>(confCopy);
            }

            getLogConfigManager().updateLoggerConfiguration(pid, conf, true);
        } catch (ConfigurationException ce) {
            throw new org.osgi.service.cm.ConfigurationException(ce.getProperty(), ce.getReason(), ce);
        }
    }

    /**
     * Remove the logger configuration for the supplied configuration service
     *
     * @param pid the service identifier for the service
     */
    public void deleted(@NotNull String pid) {
        try {
            getLogConfigManager().updateLoggerConfiguration(pid, null, true);
        } catch (ConfigurationException ce) {
            // not expected
            getLogConfigManager().internalFailure("Unexpected Configuration Problem", ce);
        }
    }
}
