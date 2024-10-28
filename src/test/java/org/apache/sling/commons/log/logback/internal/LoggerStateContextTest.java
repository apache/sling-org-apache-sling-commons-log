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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class LoggerStateContextTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();
    private LogConfigManager logConfigManager;
    private LoggerStateContext loggerState;
    private TurboFilter turboFilter;
    private ServiceReference<TurboFilter> turboFilterRef;

    @BeforeEach
    protected void beforeEach() {
        BundleContext bundleContext = context.bundleContext();

        try {
            System.setProperty(LogConstants.SLING_HOME, new File("target").getAbsolutePath());
            System.setProperty(LogConstants.LOG_FILE, "logs/hello.log");

            logConfigManager = new LogConfigManager(bundleContext);
            logConfigManager.start();
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.SLING_HOME);
        }

        ListAppender<ILoggingEvent> dynappender1 = new ListAppender<>();
        dynappender1.setName("dynappender1");
        context.registerService(Appender.class, dynappender1, Map.of(
                    AppenderTracker.PROP_LOGGER, new String[] {"log.logger1"}
                ));

        turboFilter = Mockito.spy(new TurboFilter() {
            @Override
            public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params,
                    Throwable t) {
                return FilterReply.NEUTRAL;
            }
        });
        ServiceRegistration<TurboFilter> serviceReg = bundleContext.registerService(TurboFilter.class, turboFilter, new Hashtable<>());
        turboFilterRef = serviceReg.getReference();

        loggerState = logConfigManager.determineLoggerState();
    }

    @AfterEach
    protected void afterEach() {
        logConfigManager.stop();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#getNumberOfLoggers()}.
     */
    @Test
    void testGetNumberOfLoggers() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        assertEquals(loggerContext.getLoggerList().size(),
                loggerState.getNumberOfLoggers());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#getNumOfDynamicAppenders()}.
     */
    @Test
    void testGetNumOfDynamicAppenders() {
        assertEquals(1, loggerState.getNumOfDynamicAppenders());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#getNumOfAppenders()}.
     */
    @Test
    void testGetNumOfAppenders() {
        assertEquals(2, loggerState.getNumOfAppenders());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#isDynamicAppender(ch.qos.logback.core.Appender)}.
     */
    @Test
    void testIsDynamicAppender() {
        Appender<ILoggingEvent> appender = loggerState.getAppenderMap().get("dynappender1");
        assertTrue(loggerState.isDynamicAppender(appender));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#getTurboFilterRef(ch.qos.logback.classic.turbo.TurboFilter)}.
     */
    @Test
    void testGetTurboFilterRef() {
        // with a match
        assertEquals(turboFilterRef, loggerState.getTurboFilterRef(turboFilter));

        // no match
        TurboFilter mockTF = Mockito.mock(TurboFilter.class);
        assertNull(loggerState.getTurboFilterRef(mockTF));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#getAllAppenders()}.
     */
    @Test
    void testGetAllAppenders() {
        Collection<Appender<ILoggingEvent>> allAppenders = loggerState.getAllAppenders();
        assertNotNull(allAppenders);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext#getAppenderMap()}.
     */
    @Test
    void testGetAppenderMap() {
        Map<String, Appender<ILoggingEvent>> appenderMap = loggerState.getAppenderMap();
        assertNotNull(appenderMap);
    }

}
