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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.sling.commons.log.logback.ConfigProvider;
import org.apache.sling.commons.log.logback.internal.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import ch.qos.logback.classic.LoggerContext;

/**
 * Service tracker that listens for ConfigProvider services and
 * applies them to the logging configuration
 */
public class ConfigSourceTracker extends ServiceTracker<Object, ConfigProvider>
        implements LogbackResetListener {

    /**
     * Service property name indicating that String object is a Logback config
     * fragment
     */
    public static final String PROP_LOGBACK_CONFIG = "logbackConfig";

    /**
     * Reverse sorted map of ConfigSource based on ranking of ServiceReferences
     */
    private final Map<ServiceReference<Object>, ConfigSourceInfo> inputSources = new ConcurrentSkipListMap<>(
        Collections.reverseOrder());

    private final LogConfigManager logConfigManager;

    /**
     * Constructor
     *
     * @param context the bundle context
     * @param logConfigManager the LogConfigManger to apply the configuration to
     * @throws InvalidSyntaxException if {@link #createFilter()} returns something invalid
     */
    public ConfigSourceTracker(@NotNull BundleContext context, @NotNull LogConfigManager logConfigManager) throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.logConfigManager = logConfigManager;
    }

    /**
     * Return the current set of config source information
     * 
     * @return collection of config source information
     */
    public @NotNull Collection<ConfigSourceInfo> getSources() {
        return inputSources.values();
    }

    /**
     * Close the tracker and cleanup
     */
    @Override
    public synchronized void close() {
        super.close();
        inputSources.clear();
    }

    // ~--------------------------------- ServiceTracker

    /**
     * Callback when a ConfigProvider service has been added
     *
     * @param reference the service reference that was added
     * @return the Appender service object
     */
    @Override
    public @NotNull ConfigProvider addingService(@NotNull ServiceReference<Object> reference) {
        Object o = super.addingService(reference);
        ConfigProvider cp = getConfig(o);
        inputSources.put(reference, new ConfigSourceInfo(reference, cp));
        logConfigManager.configChanged();
        return cp;
    }

    /**
     * Callback when an Appender service has been modified
     * 
     * @param reference the service reference that was modified
     * @param service the service object that was being tracked
     */
    @Override
    public void modifiedService(@NotNull ServiceReference<Object> reference, @NotNull ConfigProvider service) {
        super.modifiedService(reference, service);
        // A ConfigProvider can modify its service registration properties
        // to indicate that config has changed and a reload is required
        logConfigManager.configChanged();
    }

    /**
     * Callback when an Appender service has been removed
     * 
     * @param reference the service reference that was removed
     * @param service the service object that was being tracked
     */
    @Override
    public void removedService(@NotNull ServiceReference<Object> reference, @NotNull ConfigProvider service) {
        inputSources.remove(reference);
        logConfigManager.configChanged();
    }

    //~-----------------------------------LogbackResetListener

    /**
     * Callback before the reset is started
     *
     * @param context the logger context being reset
     */
    @Override
    public void onResetStart(@NotNull LoggerContext context) {
        // export the tracker instance. It would later be used in
        // OSGiInternalAction
        context.putObject(ConfigSourceTracker.class.getName(), this);
    }

    // ~----------------------------------ConfigSourceInfo

    /**
     * Contains the details of the config source that was added
     */
    public static class ConfigSourceInfo {
        private final ServiceReference<Object> reference;

        private final ConfigProvider configProvider;

        public ConfigSourceInfo(@NotNull ServiceReference<Object> reference, @NotNull ConfigProvider configProvider) {
            this.reference = reference;
            this.configProvider = configProvider;
        }

        public @NotNull ConfigProvider getConfigProvider() {
            return configProvider;
        }

        public @NotNull ServiceReference<Object> getReference() {
            return reference;
        }

        public @NotNull String getSourceAsString() {
            return XmlUtil.prettyPrint(getConfigProvider().getConfigSource());
        }

        public @NotNull String getSourceAsEscapedString() {
            return XmlUtil.escapeXml(getSourceAsString());
        }

        public @NotNull String toString() {
            return String.format("Service ID %s", reference.getProperty(Constants.SERVICE_ID));
        }
    }

    private static @NotNull ConfigProvider getConfig(@NotNull Object o) {
        // If string then wrap it in StringSourceProvider
        if (o instanceof String) {
            return new StringConfigProvider((String) o);
        }
        return (ConfigProvider) o;
    }

    /**
     * Creates the filter that this tracker will match against
     * 
     * @return the filter
     */
    private static @NotNull Filter createFilter() throws InvalidSyntaxException {
        // Look for either ConfigProvider or String's with property
        // logbackConfig set
        String filter = String.format("(|(objectClass=%s)(&(objectClass=java.lang.String)(%s=*)))",
            ConfigProvider.class.getName(), PROP_LOGBACK_CONFIG);
        return FrameworkUtil.createFilter(filter);
    }

}
