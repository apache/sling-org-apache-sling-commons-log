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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.util.Hashtable;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class AppenderTrackerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private LogConfigManager manager;
    private AppenderTracker tracker;

    private ServiceReference<?> serviceRef;

    private ListAppender<ILoggingEvent> appender;

    private String appenderName;

    @BeforeEach
    protected void beforeEach() throws Exception {
        BundleContext bundleContext = context.bundleContext();
        manager = new LogConfigManager(bundleContext);
        manager.start();

        tracker = new AppenderTracker(bundleContext, manager);

        appenderName = "test.appender1";
        appender = new ListAppender<>();
        appender.setName(appenderName);

        @SuppressWarnings("rawtypes")
        ServiceRegistration<Appender> serviceReg = bundleContext.registerService(Appender.class,
                appender, new Hashtable<>(Map.of(
                        AppenderTracker.PROP_LOGGER, new String[] {"log.logger1", "log.logger2"}
                        )));
        serviceRef = serviceReg.getReference();
    }

    @AfterEach
    protected void afterEach() {
        manager.stop();
    }

    private boolean anyMatch(Predicate<ILoggingEvent> p) {
        return appender.list.stream().anyMatch(p);
    }

    private void assertContains(Level atLevel, String ... substrings) {
        Stream.of(substrings).forEach(substring -> {
            if (!anyMatch(event -> event.getLevel() == atLevel && event.getFormattedMessage().contains(substring))) {
                fail(String.format("No log message contains [%s]", substring));
            }
        });
    }
    private void assertNotContains(Level atLevel, String ... substrings) {
        Stream.of(substrings).forEach(substring -> {
            if (anyMatch(event -> event.getLevel() == atLevel && event.getFormattedMessage().contains(substring))) {
                fail(String.format("Unexpacted log message contains [%s]", substring));
            }
        });
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.AppenderTracker#addingService(org.osgi.framework.ServiceReference)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testAddingService() {
        tracker.addingService((ServiceReference<Appender<ILoggingEvent>>)serviceRef);

        assertAppenderContents();
    }

    protected void assertAppenderContents() {
        Logger logger1 = LoggerFactory.getLogger("log.logger1");
        assertTrue(((ch.qos.logback.classic.Logger)logger1).isAttached(appender));
        logger1.info("Log Message from log.logger1");

        Logger logger2 = LoggerFactory.getLogger("log.logger2");
        assertTrue(((ch.qos.logback.classic.Logger)logger2).isAttached(appender));
        logger2.info("Log Message from log.logger2");

        Logger logger3 = LoggerFactory.getLogger("log.logger3");
        assertFalse(((ch.qos.logback.classic.Logger)logger3).isAttached(appender));
        logger3.info("Log Message from log.logger3");

        assertContains(Level.INFO, "Log Message from log.logger1");
        assertContains(Level.INFO, "Log Message from log.logger2");
        assertNotContains(Level.INFO, "Log Message from log.logger3");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.AppenderTracker#modifiedService(org.osgi.framework.ServiceReference, ch.qos.logback.core.Appender)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testModifiedService() {
        serviceRef = Mockito.spy(serviceRef);
        tracker.addingService((ServiceReference<Appender<ILoggingEvent>>)serviceRef);
        assertAppenderContents();
        appender.list.clear();

        // mock modify the service props
        Mockito.doReturn(new String[] {"log.logger3"})
            .when(serviceRef).getProperty(AppenderTracker.PROP_LOGGER);

        tracker.modifiedService((ServiceReference<Appender<ILoggingEvent>>)serviceRef, appender);

        Logger logger1 = LoggerFactory.getLogger("log.logger1");
        assertFalse(((ch.qos.logback.classic.Logger)logger1).isAttached(appender));
        logger1.info("Log Message from log.logger1");

        Logger logger2 = LoggerFactory.getLogger("log.logger2");
        assertFalse(((ch.qos.logback.classic.Logger)logger2).isAttached(appender));
        logger2.info("Log Message from log.logger2");

        Logger logger3 = LoggerFactory.getLogger("log.logger3");
        assertTrue(((ch.qos.logback.classic.Logger)logger3).isAttached(appender));
        logger3.info("Log Message from log.logger3");

        assertNotContains(Level.INFO, "Log Message from log.logger1");
        assertNotContains(Level.INFO, "Log Message from log.logger2");
        assertContains(Level.INFO, "Log Message from log.logger3");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.AppenderTracker#removedService(org.osgi.framework.ServiceReference, ch.qos.logback.core.Appender)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRemovedService() {
        tracker.addingService((ServiceReference<Appender<ILoggingEvent>>)serviceRef);

        tracker.removedService((ServiceReference<Appender<ILoggingEvent>>)serviceRef, appender);

        Logger logger1 = LoggerFactory.getLogger("log.logger1");
        assertFalse(((ch.qos.logback.classic.Logger)logger1).isAttached(appender));
        logger1.info("Log Message from log.logger1");

        Logger logger2 = LoggerFactory.getLogger("log.logger2");
        assertFalse(((ch.qos.logback.classic.Logger)logger2).isAttached(appender));
        logger2.info("Log Message from log.logger2");

        assertNotContains(Level.INFO, "Log Message from log.logger1");
        assertNotContains(Level.INFO, "Log Message from log.logger2");
    }

    @Test
    void testCreateFilter() throws InvalidSyntaxException {
        assertNotNull(AppenderTracker.createFilter());
    }

    @Test
    void testCreateFilterWithCaughtException() throws Exception {
        // verify the exception handling
        try (MockedStatic<FrameworkUtil> frameworkUtil = Mockito.mockStatic(FrameworkUtil.class, CALLS_REAL_METHODS);) {
            frameworkUtil.when(() -> FrameworkUtil.createFilter(anyString()))
                .thenThrow(InvalidSyntaxException.class);

            BundleContext bundleContext = context.bundleContext();
            assertThrows(InvalidSyntaxException.class, () -> new AppenderTracker(bundleContext, manager));
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.AppenderTracker#getAppenderInfos()}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testGetAppenderInfos() {
        // initially empty
        assertTrue(tracker.getAppenderInfos().isEmpty());

        // add an appender service
        tracker.addingService((ServiceReference<Appender<ILoggingEvent>>)serviceRef);
        // now not empty
        assertFalse(tracker.getAppenderInfos().isEmpty());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.AppenderTracker#onResetComplete(ch.qos.logback.classic.LoggerContext)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testOnResetComplete() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        // add an appender service
        tracker.addingService((ServiceReference<Appender<ILoggingEvent>>)serviceRef);
        ch.qos.logback.classic.Logger logger1 = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("log.logger1");
        assertTrue(logger1.isAttached(appender));

        // reset start should clear out the appender
        loggerContext.reset();
        assertNull(logger1.getAppender(appenderName));

        // reset complete should restore the appender
        tracker.onResetComplete(loggerContext);
        assertNotNull(logger1.getAppender(appenderName));
    }

}
