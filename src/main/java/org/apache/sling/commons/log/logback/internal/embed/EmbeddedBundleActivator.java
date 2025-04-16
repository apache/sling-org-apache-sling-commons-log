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
package org.apache.sling.commons.log.logback.internal.embed;

import java.util.ServiceLoader;

import ch.qos.logback.classic.spi.Configurator;
import org.apache.sling.commons.log.logback.internal.Activator;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.slf4j.spi.SLF4JServiceProvider;

public class EmbeddedBundleActivator extends Activator {

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        ServiceLoader.load(SLF4JServiceProvider.class, this.getClass().getClassLoader())
                .forEach(serviceProvider -> {
                    // Register the service provider with the OSGi context
                    context.registerService(SLF4JServiceProvider.class, serviceProvider, null);
                });
        ServiceLoader.load(Configurator.class, this.getClass().getClassLoader()).forEach(serviceProvider -> {
            // Register the configurator with the OSGi context
            context.registerService(Configurator.class, serviceProvider, null);
        });
        super.start(context);
    }
}
