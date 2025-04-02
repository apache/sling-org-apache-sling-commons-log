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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class OsgiIntegrationListenerTest {
    protected final OsgiContext context = new OsgiContext();

    private LogConfigManager logConfigManager;
    private OsgiIntegrationListener listener;
    private LoggerContext loggerContext;


    @BeforeEach
    protected void beforeEach() {
        BundleContext bundleContext = context.bundleContext();
        logConfigManager = Mockito.spy(new LogConfigManager(bundleContext));
        listener = new OsgiIntegrationListener(logConfigManager);

        loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
    }

    @AfterEach
    protected void afterEach() {
        logConfigManager.stop();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.OsgiIntegrationListener#isResetResistant()}.
     */
    @Test
    void testIsResetResistant() {
        assertTrue(listener.isResetResistant());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.OsgiIntegrationListener#onStart(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnStart() {
        // should do nothing
        assertDoesNotThrow(() -> listener.onStart(loggerContext));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.OsgiIntegrationListener#onReset(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnReset() {
        listener.onReset(loggerContext);
        Mockito.verify(logConfigManager, times(1)).fireResetStartListeners();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.OsgiIntegrationListener#onStop(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnStop() {
        // should do nothing
        assertDoesNotThrow(() -> listener.onStop(loggerContext));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.OsgiIntegrationListener#onLevelChange(ch.qos.logback.classic.Logger, ch.qos.logback.classic.Level)}.
     */
    @Test
    void testOnLevelChange() {
        // should do nothing
        Logger logger = (Logger)LoggerFactory.getLogger(getClass());
        assertDoesNotThrow(() -> listener.onLevelChange(logger, Level.WARN));
    }

}
