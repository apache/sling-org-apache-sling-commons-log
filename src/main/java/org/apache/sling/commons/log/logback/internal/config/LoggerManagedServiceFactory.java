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

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.osgi.service.cm.ManagedServiceFactory;

class LoggerManagedServiceFactory extends LogConfigurator implements ManagedServiceFactory {

    public static final String LOG_FILE_DEFAULT = "logs/error.log";

    public String getName() {
        return "Logger configurator";
    }

    @SuppressWarnings("unchecked")
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary configuration)
            throws org.osgi.service.cm.ConfigurationException {
        try {
            Dictionary<String, Object> conf = configuration;
            if (configuration.get(LogConfigManager.LOG_FILE) == null) {
                List<String> keys = Collections.list(configuration.keys());
                Map<String, Object> confCopy = keys.stream()
                           .collect(Collectors.toMap(Function.identity(), configuration::get)); 
                confCopy.put(LogConfigManager.LOG_FILE, LOG_FILE_DEFAULT);
                conf = new Hashtable<>(confCopy);
            }
            getLogConfigManager().updateLoggerConfiguration(pid, conf, true);
        } catch (ConfigurationException ce) {
            throw new org.osgi.service.cm.ConfigurationException(ce.getProperty(), ce.getReason(), ce);
        }
    }

    public void deleted(String pid) {
        try {
            getLogConfigManager().updateLoggerConfiguration(pid, null, true);
        } catch (ConfigurationException ce) {
            // not expected
            getLogConfigManager().internalFailure("Unexpected Configuration Problem", ce);
        }
    }
}