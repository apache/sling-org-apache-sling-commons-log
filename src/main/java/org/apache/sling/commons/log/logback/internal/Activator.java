/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.log.logback.internal;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Activator for initializing the LogConfigManager after the (Logback) SLF4J LoggerFactory arrives
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    private ServiceTracker<SLF4JServiceProvider, SLF4JServiceProvider> slf4jServiceProviderTracker;

    /**
     * Initializes custom logback configuration when this bundle is started
     */
    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        // listen for the arrival of new SLF4JServiceProvider components
        slf4jServiceProviderTracker = new SLF4JServiceProviderTracker(context);
        slf4jServiceProviderTracker.open(true);
    }

    /**
     * Shutdown and undo our custom logback configuration
     */
    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        if (slf4jServiceProviderTracker != null) {
            slf4jServiceProviderTracker.close();
            slf4jServiceProviderTracker = null;
        }
    }

}
