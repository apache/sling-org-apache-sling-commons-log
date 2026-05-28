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
package org.apache.sling.commons.log.logback.internal.store;

import java.util.Dictionary;
import java.util.Hashtable;

import ch.qos.logback.core.Appender;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.apache.sling.commons.log.logback.store.LogStore;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.converter.Converters;

public class LogStoreRegistrar {

    static final String PID = "org.apache.sling.commons.log.LogStore";
    static final String PROP_MAX_ENTRIES = "maxEntries";
    static final String PROP_LOGGERS = "loggers";
    static final String[] DEFAULT_LOGGERS = {"ROOT"};

    private ServiceRegistration<LogStore> storeRegistration;
    private ServiceRegistration<Appender> appenderRegistration;
    private ServiceRegistration<ManagedService> configRegistration;
    private BundleContext bundleContext;
    private LogStoreImpl store;
    private LogStoreAppender appender;
    private String[] activeLoggers;

    public void start(BundleContext context) {
        this.bundleContext = context;

        Dictionary<String, Object> configProps = new Hashtable<>();
        configProps.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        configProps.put(Constants.SERVICE_DESCRIPTION, "Log Store Configurator");
        configProps.put(Constants.SERVICE_PID, PID);
        configRegistration = context.registerService(ManagedService.class, this::updated, configProps);
    }

    public void stop() {
        deactivate();

        if (configRegistration != null) {
            configRegistration.unregister();
            configRegistration = null;
        }
        bundleContext = null;
    }

    void updated(@Nullable Dictionary<String, ?> properties) {
        if (properties == null) {
            deactivate();
            return;
        }

        int maxEntries = Converters.standardConverter()
                .convert(properties.get(PROP_MAX_ENTRIES))
                .defaultValue(LogStoreImpl.DEFAULT_MAX_ENTRIES)
                .to(Integer.class);

        String[] loggers = Converters.standardConverter()
                .convert(properties.get(PROP_LOGGERS))
                .defaultValue(DEFAULT_LOGGERS)
                .to(String[].class);
        if (loggers == null || loggers.length == 0) {
            loggers = DEFAULT_LOGGERS;
        }

        if (store == null) {
            activate(maxEntries, loggers);
        } else {
            store.setMaxEntries(maxEntries);
            applyLoggerConfig(loggers);
        }
    }

    private void activate(int maxEntries, String[] loggers) {
        if (bundleContext == null || store != null) {
            return;
        }

        store = new LogStoreImpl(maxEntries);
        appender = new LogStoreAppender(store);

        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        serviceProps.put(Constants.SERVICE_DESCRIPTION, "Log Store");
        storeRegistration = bundleContext.registerService(LogStore.class, store, serviceProps);

        Dictionary<String, Object> appenderProps = new Hashtable<>();
        appenderProps.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        appenderProps.put(Constants.SERVICE_DESCRIPTION, "Log Store Appender");
        appenderProps.put(PROP_LOGGERS, loggers);
        appenderRegistration = bundleContext.registerService(Appender.class, appender, appenderProps);
        activeLoggers = loggers;
    }

    private void applyLoggerConfig(String[] loggers) {
        if (appenderRegistration == null || equalLoggers(activeLoggers, loggers)) {
            return;
        }
        Dictionary<String, Object> appenderProps = new Hashtable<>();
        appenderProps.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        appenderProps.put(Constants.SERVICE_DESCRIPTION, "Log Store Appender");
        appenderProps.put(PROP_LOGGERS, loggers);
        appenderRegistration.setProperties(appenderProps);
        activeLoggers = loggers;
    }

    private boolean equalLoggers(String[] a, String[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

    private void deactivate() {
        if (appenderRegistration != null) {
            appenderRegistration.unregister();
            appenderRegistration = null;
        }
        if (storeRegistration != null) {
            storeRegistration.unregister();
            storeRegistration = null;
        }

        appender = null;
        store = null;
        activeLoggers = null;
    }
}
