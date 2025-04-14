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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

/**
 * Service tracker that listens for TurboFilter services and
 * applies them to the logging configuration
 */
public class TurboFilterTracker extends ServiceTracker<TurboFilter, TurboFilter> implements LogbackResetListener {

    private final Map<ServiceReference<TurboFilter>, TurboFilter> filters = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param context the bundle context
     */
    public TurboFilterTracker(@NotNull BundleContext context) {
        super(context, TurboFilter.class.getName(), null);
    }

    /**
     * Callback when a TurboFilter service has been added
     *
     * @param reference the service reference that was added
     * @return the TurboFilter service object
     */
    @Override
    public @NotNull TurboFilter addingService(@NotNull ServiceReference<TurboFilter> reference) {
        TurboFilter tf = super.addingService(reference);

        attachFilter(tf);
        filters.put(reference, tf);

        return tf;
    }

    /**
     * Callback when a TurboFilter service has been removed
     *
     * @param reference the service reference that was removed
     * @param service the service object that was being tracked
     */
    @Override
    public void removedService(@NotNull ServiceReference<TurboFilter> reference, @NotNull TurboFilter service) {
        filters.remove(reference);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getTurboFilterList().remove(service);
        service.stop();

        super.removedService(reference, service);
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
     * Return a view of the current turbo filters that have been tracked
     *
     * @return unmodifiable map of filters where the key is the service reference
     *          and the value is the service object
     */
    public @NotNull Map<ServiceReference<TurboFilter>, TurboFilter> getFilters() {
        return Collections.unmodifiableMap(filters);
    }

    // ~-----------------------------------LogbackResetListener

    /**
     * Callback before the reset is started
     *
     * @param context the logger context being reset
     */
    @Override
    public void onResetStart(@NotNull LoggerContext context) {
        for (TurboFilter tf : filters.values()) {
            attachFilter(tf);
        }
    }

    // ~-----------------------------------Internal Methods

    /**
     * Attach the turbo filter to the logger context if it is not already there
     *
     * @param loggerContext the logger context
     * @param tf the turbo filter to attach
     */
    private void attachFilter(@NotNull TurboFilter tf) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (!loggerContext.getTurboFilterList().contains(tf)) {
            tf.setContext(loggerContext);
            tf.start();

            loggerContext.addTurboFilter(tf);
        }
    }
}
