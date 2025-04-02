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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.slf4j.spi.SLF4JServiceProvider;

import ch.qos.logback.classic.spi.LogbackServiceProvider;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
abstract class BaseTryLoggingTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();
    protected Activator activator = new Activator();
    protected String logFilePath;

    @BeforeEach
    protected void beforeEach() {
        try {
            System.setProperty(LogConstants.SLING_HOME, "target");

            logFilePath = String.format("logs/%s/output.log", getClass().getName());
            System.setProperty(LogConstants.LOG_FILE, logFilePath);

            BundleContext bundleContext = context.bundleContext();
            assertDoesNotThrow(() -> activator.start(bundleContext));
            // trigger the service tracker that initializes the LogConfigManager
            context.registerService(SLF4JServiceProvider.class, new LogbackServiceProvider());
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.SLING_HOME);
        }
    }

    @AfterEach
    protected void afterEach() {
        BundleContext bundleContext = context.bundleContext();
        assertDoesNotThrow(() -> activator.stop(bundleContext));
    }

}
