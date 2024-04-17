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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.slf4j.spi.SLF4JServiceProvider;

import ch.qos.logback.classic.spi.LogbackServiceProvider;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class ActivatorTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private Activator activator = new Activator();

    @AfterEach
    protected void afterEach() throws Exception {
        BundleContext bundleContext = context.bundleContext();
        activator.stop(bundleContext);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.Activator#start(org.osgi.framework.BundleContext)}.
     */
    @Test
    void testStart() {
        BundleContext bundleContext = context.bundleContext();
        assertDoesNotThrow(() -> activator.start(bundleContext));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.Activator#stop(org.osgi.framework.BundleContext)}.
     */
    @Test
    void testStopWithoutStart() {
        BundleContext bundleContext = context.bundleContext();
        // never started, so should just do nothing
        assertDoesNotThrow(() -> activator.stop(bundleContext));
    }

    @Test
    void testStop() {
        BundleContext bundleContext = context.bundleContext();
        assertDoesNotThrow(() -> activator.start(bundleContext));
        // trigger the service tracker that initializes the LogConfigManager
        context.registerService(SLF4JServiceProvider.class, new LogbackServiceProvider());
        // was started, so should just stop everything that was previously started
        assertDoesNotThrow(() -> activator.stop(bundleContext));
    }

}
