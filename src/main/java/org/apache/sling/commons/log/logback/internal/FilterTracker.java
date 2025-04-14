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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.util.ContextUtil;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.converter.Converters;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

/**
 * Service tracker that listens for Filter services and
 * applies them to the logging configuration
 */
public class FilterTracker extends ServiceTracker<Filter<ILoggingEvent>, Filter<ILoggingEvent>>
        implements LogbackResetListener {
    static final String ALL_APPENDERS = "*";
    static final String PROP_APPENDER = "appenders";

    private final LogConfigManager logConfigManager;
    private Map<ServiceReference<Filter<ILoggingEvent>>, FilterInfo> filters = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param context the bundle context
     * @param logConfigManager the LogConfigManager to configure
     * @throws InvalidSyntaxException if {@link #createFilter()} returns something invalid
     */
    public FilterTracker(@NotNull BundleContext context, @NotNull LogConfigManager logConfigManager)
            throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.logConfigManager = logConfigManager;
    }

    /**
     * Callback when a Filter service has been added
     *
     * @param reference the service reference that was added
     * @return the Filter service object
     */
    @Override
    public @NotNull Filter<ILoggingEvent> addingService(@NotNull ServiceReference<Filter<ILoggingEvent>> reference) {
        Filter<ILoggingEvent> f = super.addingService(reference);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        f.setContext(loggerContext);
        f.start();

        FilterInfo fi = new FilterInfo(reference, f);
        filters.put(reference, fi);
        attachFilter(fi, getAppenderMap());

        return f;
    }

    /**
     * Callback when a Filter service has been modified
     *
     * @param reference the service reference that was modified
     * @param service the service object that was being tracked
     */
    @Override
    public void modifiedService(
            @NotNull ServiceReference<Filter<ILoggingEvent>> reference, @NotNull Filter<ILoggingEvent> service) {
        FilterInfo fi = filters.remove(reference);
        detachFilter(fi, getAppenderMap());
        filters.put(reference, new FilterInfo(reference, service));
        attachFilter(fi, getAppenderMap());
    }

    /**
     * Callback when a Filter service has been removed
     *
     * @param reference the service reference that was removed
     * @param service the service object that was being tracked
     */
    @Override
    public void removedService(
            @NotNull ServiceReference<Filter<ILoggingEvent>> reference, @NotNull Filter<ILoggingEvent> service) {
        FilterInfo fi = filters.remove(reference);
        fi.stop();

        detachFilter(fi, getAppenderMap());

        super.removedService(reference, service);
    }

    /**
     * Callback with a new appender has been attached to a Logger
     *
     * @param appender the appender that was attached
     */
    public void attachedAppender(Appender<ILoggingEvent> appender) {
        for (FilterInfo fi : filters.values()) {
            attachFilter(fi, Map.of(appender.getName(), appender), false);
        }
    }

    /**
     * Callback with an appender has been detached from a Logger
     *
     * @param appender the appender that was detatched
     */
    public void detachedAppender(Appender<ILoggingEvent> appender) {
        for (FilterInfo fi : filters.values()) {
            detachFilter(fi, Map.of(appender.getName(), appender));
        }
    }

    /**
     * Close the tracker and cleanup
     */
    @Override
    public synchronized void close() {
        super.close();
        filters.clear();
    }

    /**
     * Return a view of the current filters that have been tracked
     *
     * @return unmodifiable map of filters where the key is the service reference
     *          and the value is the service object
     */
    public @NotNull Map<ServiceReference<Filter<ILoggingEvent>>, FilterInfo> getFilters() {
        return Collections.unmodifiableMap(filters);
    }

    // ~-----------------------------------LogbackResetListener

    /**
     * Callback after the reset is completed
     *
     * @param context the logger context being reset
     */
    @Override
    public void onResetComplete(@NotNull LoggerContext context) {
        // The filters are attached at end when all appenders have been instantiated
        Map<String, Appender<ILoggingEvent>> appenderMap = getAppenderMap();
        for (FilterInfo fi : filters.values()) {
            attachFilter(fi, appenderMap);
        }
    }

    // ~-----------------------------------Internal Methods

    /**
     * Attach the filter to the appropriate appenders
     *
     * @param fi the info about the filter to attach
     * @param appenderMap the map of the current appenders
     */
    private void attachFilter(@NotNull FilterInfo fi, @NotNull Map<String, Appender<ILoggingEvent>> appenderMap) {
        attachFilter(fi, appenderMap, true);
    }

    /**
     * Attach the filter to the appropriate appenders
     *
     * @param fi the info about the filter to attach
     * @param appenderMap the map of the current appenders
     * @param warnOnNoMatch true to add a status warning when no filter is found that matches the appender
     */
    private void attachFilter(
            @NotNull FilterInfo fi, @NotNull Map<String, Appender<ILoggingEvent>> appenderMap, boolean warnOnNoMatch) {
        if (fi.registerAgainstAllAppenders) {
            for (Appender<ILoggingEvent> appender : appenderMap.values()) {
                attachFilter(appender, fi);
            }
            return;
        }
        for (String appenderName : fi.appenderNames) {
            Appender<ILoggingEvent> appender = appenderMap.get(appenderName);
            if (appender != null) {
                attachFilter(appender, fi);
            } else if (warnOnNoMatch) {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                new ContextUtil(loggerContext)
                        .addWarn("No appender with name [" + appenderName + "] found " + "to which " + fi.filter
                                + " can be attached");
            }
        }
    }

    /**
     * Detach the filter from the appropriate appenders
     *
     * @param fi the info about the filter to detach
     * @param appenderMap the map of the current appenders
     */
    private void detachFilter(@NotNull FilterInfo fi, @NotNull Map<String, Appender<ILoggingEvent>> appenderMap) {
        if (fi.registerAgainstAllAppenders) {
            for (Appender<ILoggingEvent> appender : appenderMap.values()) {
                detachFilter(appender, fi);
            }
            return;
        }

        for (String appenderName : fi.appenderNames) {
            Appender<ILoggingEvent> appender = appenderMap.get(appenderName);
            if (appender != null) {
                detachFilter(appender, fi);
            }
        }
    }

    /**
     * Attach the filter to the specified appender if it does not already
     * contain the filter
     *
     * @param appender the appender to add the filter to
     * @param fi the info about the filter to attach
     */
    private void attachFilter(@NotNull Appender<ILoggingEvent> appender, @NotNull FilterInfo fi) {
        // TOCHECK Should we add based on some ranking
        if (!appender.getCopyOfAttachedFiltersList().contains(fi.filter)) {
            appender.addFilter(fi.filter);
        }
    }

    /**
     * Detach the filter from the specified appender if it contains the filter
     *
     * @param appender the appender to add the filter to
     * @param fi the info about the filter to attach
     */
    private void detachFilter(@NotNull Appender<ILoggingEvent> appender, @NotNull FilterInfo fi) {
        // No method to directly remove filter. So clone -> remove -> add
        if (appender.getCopyOfAttachedFiltersList().contains(fi.filter)) {
            // Clone
            List<Filter<ILoggingEvent>> filtersCopy = appender.getCopyOfAttachedFiltersList();

            // Clear
            appender.clearAllFilters();

            // Add
            for (Filter<ILoggingEvent> filter : filtersCopy) {
                if (!fi.filter.equals(filter)) {
                    appender.addFilter(filter);
                }
            }
        }
    }

    /**
     * Get a map of the current appenders that are managed by our LogConfigManager
     * @return map of the known appenders where the key is the name and the value is the appender
     */
    private @NotNull Map<String, Appender<ILoggingEvent>> getAppenderMap() {
        return logConfigManager.getAllKnownAppenders();
    }

    /**
     * Creates the filter that this tracker will match against
     *
     * @return the filter
     */
    private static @NotNull org.osgi.framework.Filter createFilter() throws InvalidSyntaxException {
        String filter = String.format("(&(objectClass=%s)(%s=*))", Filter.class.getName(), PROP_APPENDER);
        return FrameworkUtil.createFilter(filter);
    }

    /**
     * Encapsulates the state of a filter that we are tracking
     */
    static class FilterInfo {
        final ServiceReference<Filter<ILoggingEvent>> reference;
        final Filter<ILoggingEvent> filter;
        final Set<String> appenderNames;
        final boolean registerAgainstAllAppenders;

        /**
         * Constructor
         *
         * @param reference the service reference of the filter
         * @param filter the service object
         */
        FilterInfo(@NotNull ServiceReference<Filter<ILoggingEvent>> reference, @NotNull Filter<ILoggingEvent> filter) {
            this.reference = reference;
            this.filter = filter;

            @SuppressWarnings("unchecked")
            Set<String> appenderSet = Converters.standardConverter()
                    .convert(reference.getProperty(PROP_APPENDER))
                    .defaultValue(Collections.emptySet())
                    .to(Set.class);
            this.appenderNames = Collections.unmodifiableSet(appenderSet);
            this.registerAgainstAllAppenders = appenderNames.contains(ALL_APPENDERS);
        }

        /**
         * Stop the filter if it has been started
         */
        public void stop() {
            if (filter.isStarted()) {
                filter.stop();
            }
        }
    }
}
