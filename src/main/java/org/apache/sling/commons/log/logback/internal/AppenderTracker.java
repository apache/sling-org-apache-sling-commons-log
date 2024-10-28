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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.converter.Converters;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Service tracker that listens for Appender services and
 * applies them to the logging configuration
 */
public class AppenderTracker extends ServiceTracker<Appender<ILoggingEvent>, Appender<ILoggingEvent>> 
        implements LogbackResetListener {

    static final String PROP_LOGGER = "loggers";

    private final LogConfigManager logConfigManager;

    private final Map<ServiceReference<Appender<ILoggingEvent>>, AppenderInfo> appenders = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param context the bundle context
     * @param logConfigManager the LogConfigManger to apply the configuration to
     * @throws InvalidSyntaxException if {@link #createFilter()} returns something invalid
     */
    public AppenderTracker(@NotNull final BundleContext context,
            @NotNull final LogConfigManager logConfigManager) throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.logConfigManager = logConfigManager;
    }

    /**
     * Callback when an Appender service has been added
     *
     * @param reference the service reference that was added
     * @return the Appender service object
     */
    @Override
    public @NotNull Appender<ILoggingEvent> addingService(@NotNull ServiceReference<Appender<ILoggingEvent>> reference) {
        final Appender<ILoggingEvent> appender = super.addingService(reference);

        final AppenderInfo ai = new AppenderInfo(reference, appender);
        appenders.put(reference, ai);
        attachAppender(ai);

        return appender;
    }

    /**
     * Callback when an Appender service has been modified
     * 
     * @param reference the service reference that was modified
     * @param service the service object that was being tracked
     */
    @Override
    public void modifiedService(@NotNull ServiceReference<Appender<ILoggingEvent>> reference,
            @NotNull Appender<ILoggingEvent> service) {
        detachAppender(appenders.remove(reference));
        final AppenderInfo nai = new AppenderInfo(reference, service);
        appenders.put(reference, nai);
        attachAppender(nai);
    }

    /**
     * Callback when an Appender service has been removed
     * 
     * @param reference the service reference that was removed
     * @param service the service object that was being tracked
     */
    @Override
    public void removedService(@NotNull ServiceReference<Appender<ILoggingEvent>> reference,
            @NotNull Appender<ILoggingEvent> service) {
        final AppenderInfo ai = appenders.remove(reference);
        this.detachAppender(ai);
        super.removedService(reference, service);
    }

    /**
     * Return the current set of appender information
     * 
     * @return collection of appender information
     */
    public @NotNull Collection<AppenderInfo> getAppenderInfos() {
        return Collections.unmodifiableCollection(appenders.values());
    }

    /**
     * Creates the filter that this tracker will match against
     * 
     * @return the filter
     */
    static @NotNull Filter createFilter() throws InvalidSyntaxException {
        String filter = String.format("(&(objectClass=%s)(%s=*))", Appender.class.getName(), PROP_LOGGER);
        return FrameworkUtil.createFilter(filter);
    }

    //~-----------------------------------LogbackResetListener

    /**
     * Callback after the reset is completed
     * 
     * @param context the logger context being reset
     */
    @Override
    public void onResetComplete(@NotNull LoggerContext context) {
        // Re-attach all the known appenders
        for (AppenderInfo ai : appenders.values()) {
            attachAppender(ai);
        }
    }

    //~-----------------------------------Internal Methods

    /**
     * Detach the appender from the loggers
     *
     * @param ai the appender information
     */
    private void detachAppender(@NotNull final AppenderInfo ai) {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        for (final String name : ai.getLoggers()) {
            final Logger logger = loggerContext.getLogger(name);
            logConfigManager.maybeDetachAppender(AppenderOrigin.TRACKER, ai.name, logger);
        }
        ai.appender.stop();
    }

    /**
     * Attach the appender to the expected loggers
     * 
     * @param ai the appender information
     */
    private void attachAppender(@NotNull final AppenderInfo ai) {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        ai.appender.setContext(loggerContext);
        ai.appender.start();

        logConfigManager.addOrUpdateAppender(AppenderOrigin.TRACKER, ai.name, ai.appender, ai.getLoggers());
    }

    /**
     * Contains the details of the appender that was added
     */
    class AppenderInfo {
        private final Set<String> loggers;
        final Appender<ILoggingEvent> appender;
        final String pid;
        private final String name;

        /**
         * Constructor
         *
         * @param ref the service reference
         * @param appender the appender that was added
         */
        public AppenderInfo(@NotNull final ServiceReference<Appender<ILoggingEvent>> ref,
                    @NotNull Appender<ILoggingEvent> appender) {
            this.appender = appender;
            this.pid = ref.getProperty(Constants.SERVICE_ID).toString();

            @SuppressWarnings("unchecked")
            Set<String> loggerNames = Converters.standardConverter()
                .convert(ref.getProperty(PROP_LOGGER))
                .defaultValue(Collections.emptySet())
                .to(Set.class);
            this.name = appender.getName();
            this.loggers = loggerNames;

            logConfigManager.addOrUpdateAppender(AppenderOrigin.TRACKER, this.name, appender, loggerNames);
        }

        /**
         * Return the set of loggers that the appender is for
         *
         * @return set of logger names
         */
        public @NotNull Set<String> getLoggers(){
            Set<String> result = new HashSet<>(loggers);

            Set<String> loggersFromConfig = logConfigManager.getLoggerNamesForKnownAppender(AppenderOrigin.JORAN_OSGI, name);
            result.addAll(loggersFromConfig);

            return Collections.unmodifiableSet(result);
        }
    }

}
