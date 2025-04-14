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

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
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
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.times;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class FilterTrackerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private LogConfigManager manager;
    private FilterTracker tracker;
    private Filter<ILoggingEvent> filter1;
    private ServiceReference<?> serviceRef1;

    private String appenderName;

    @BeforeEach
    protected void beforeEach() throws InvalidSyntaxException {
        BundleContext bundleContext = context.bundleContext();
        System.setProperty(LogConstants.SLING_HOME, new File("target").getPath());
        manager = new LogConfigManager(bundleContext);

        appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        mockAddedAppender(appenderName);

        tracker = new FilterTracker(bundleContext, manager);

        filter1 = Mockito.spy(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        });

        ServiceRegistration<?> serviceReg1 = bundleContext.registerService(
                Filter.class,
                filter1,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName, "invalid"})));
        serviceRef1 = serviceReg1.getReference();
    }

    @AfterEach
    protected void afterEach() {
        // cleanup to not interfere with other tests
        System.clearProperty(LogConstants.SLING_HOME);

        if (tracker != null) {
            tracker.close();
            tracker = null;
        }

        if (manager != null) {
            manager.stop();
        }
    }

    protected void mockAddedAppender(String name) {
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testAddOrUpdateAppender"},
                LogConstants.LOG_LEVEL, "debug",
                LogConstants.LOG_FILE, "logs/testAddOrUpdateAppender.log"));
        manager.addOrUpdateAppender(AppenderOrigin.JORAN, name, config);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#close()}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testClose() {
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        assertFalse(tracker.getFilters().isEmpty());

        tracker.close();
        assertTrue(tracker.getFilters().isEmpty());
        tracker = null;
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#addingService(org.osgi.framework.ServiceReference)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testAddingService() {
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Mockito.verify(filter1, times(1)).start();

        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testAddingServiceWithAllAppenders() {
        BundleContext bundleContext = context.bundleContext();
        ServiceRegistration<?> serviceReg = bundleContext.registerService(
                Filter.class,
                filter1,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {FilterTracker.ALL_APPENDERS})));
        serviceRef1 = serviceReg.getReference();

        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Mockito.verify(filter1, times(1)).start();

        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testAddingServiceWithFilterThatIsAlreadyAttached() {
        // pre-add so the filter already exists on the appender
        Appender<ILoggingEvent> appender =
                manager.getKnownAppenders(AppenderOrigin.JORAN).get(appenderName);
        assertNotNull(appender);
        appender.addFilter(filter1);
        List<Filter<ILoggingEvent>> beforeFilters = appender.getCopyOfAttachedFiltersList();
        assertTrue(beforeFilters.contains(filter1));

        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        appender = manager.getKnownAppenders(AppenderOrigin.JORAN).get(appenderName);
        // should be the same
        assertEquals(beforeFilters, appender.getCopyOfAttachedFiltersList());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#modifiedService(org.osgi.framework.ServiceReference, ch.qos.logback.core.filter.Filter)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testModifiedService() {
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Mockito.verify(filter1, times(1)).start();

        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));

        tracker.modifiedService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1, filter1);
        appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#removedService(org.osgi.framework.ServiceReference, ch.qos.logback.core.filter.Filter)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRemovedService() {
        Filter<ILoggingEvent> filter2 = Mockito.spy(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        });
        BundleContext bundleContext = context.bundleContext();
        ServiceRegistration<?> serviceReg2 = bundleContext.registerService(
                Filter.class,
                filter2,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName, "invalid"})));
        ServiceReference<?> serviceRef2 = serviceReg2.getReference();
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef2);

        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));

        // remove the first one
        tracker.removedService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1, filter1);
        appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertFalse(appender.getCopyOfAttachedFiltersList().contains(filter1));

        // remove the second one
        tracker.removedService((ServiceReference<Filter<ILoggingEvent>>) serviceRef2, filter2);
        assertFalse(appender.getCopyOfAttachedFiltersList().contains(filter2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRemovedServiceForAlreadyStoppedFilter() {
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));

        filter1.stop();

        Mockito.reset(filter1);
        tracker.removedService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1, filter1);
        // no stop call when already stopped
        Mockito.verify(filter1, times(0)).stop();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRemovedServiceWithAllAppenders() {
        BundleContext bundleContext = context.bundleContext();
        ServiceRegistration<?> serviceReg = bundleContext.registerService(
                Filter.class,
                filter1,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {FilterTracker.ALL_APPENDERS})));
        serviceRef1 = serviceReg.getReference();

        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Mockito.verify(filter1, times(1)).start();
        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));

        tracker.removedService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1, filter1);
        appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertFalse(appender.getCopyOfAttachedFiltersList().contains(filter1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRemovedServiceWithFilterThatIsAlreadyDetatched() {
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Map<String, Appender<ILoggingEvent>> knownAppenders = manager.getKnownAppenders(AppenderOrigin.JORAN);
        Appender<ILoggingEvent> appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));

        // No method to directly remove filter. So clone -> remove -> add
        // Clone
        List<Filter<ILoggingEvent>> copyOfFilters = appender.getCopyOfAttachedFiltersList();
        // Clear
        appender.clearAllFilters();
        // Add
        for (Filter<ILoggingEvent> filter : copyOfFilters) {
            if (!filter.equals(filter)) {
                appender.addFilter(filter);
            }
        }

        List<Filter<ILoggingEvent>> beforeFilters = appender.getCopyOfAttachedFiltersList();
        tracker.removedService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1, filter1);
        appender = knownAppenders.get(appenderName);
        assertNotNull(appender);
        // should be the same
        assertEquals(beforeFilters, appender.getCopyOfAttachedFiltersList());
    }

    @Test
    void testCreateFilterWithCaughtException() throws Exception {
        // verify the exception handling
        try (MockedStatic<FrameworkUtil> frameworkUtil =
                Mockito.mockStatic(FrameworkUtil.class, CALLS_REAL_METHODS); ) {
            frameworkUtil.when(() -> FrameworkUtil.createFilter(anyString())).thenThrow(InvalidSyntaxException.class);

            BundleContext bundleContext = context.bundleContext();
            assertThrows(InvalidSyntaxException.class, () -> new FilterTracker(bundleContext, manager));
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#attachedAppender(ch.qos.logback.core.Appender)}.
     */
    @Test
    void testAttachedAppender() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        @SuppressWarnings("rawtypes")
        ServiceRegistration registerService = bundleContext.registerService(
                Filter.class, filter, new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName
                })));
        @SuppressWarnings("unchecked")
        ServiceReference<Filter<ILoggingEvent>> reference = registerService.getReference();
        tracker.addingService(reference);

        Appender<ILoggingEvent> appender = new ListAppender<>();
        appender.setName(appenderName);
        tracker.attachedAppender(appender);

        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#detachedAppender(ch.qos.logback.core.Appender)}.
     */
    @Test
    void testDetachedAppender() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        @SuppressWarnings("rawtypes")
        ServiceRegistration registerService = bundleContext.registerService(
                Filter.class, filter, new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName
                })));
        @SuppressWarnings("unchecked")
        ServiceReference<Filter<ILoggingEvent>> reference = registerService.getReference();
        tracker.addingService(reference);

        Appender<ILoggingEvent> appender = new ListAppender<>();
        appender.setName(appenderName);
        tracker.attachedAppender(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter));

        tracker.detachedAppender(appender);
        assertFalse(appender.getCopyOfAttachedFiltersList().contains(filter));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilterTracker#onResetComplete(ch.qos.logback.classic.LoggerContext)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testOnResetComplete() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = (Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppender");

        // first add a filter
        tracker.addingService((ServiceReference<Filter<ILoggingEvent>>) serviceRef1);
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        assertNotNull(appender);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));

        // reset start should clear out the appender the filter was attached to
        loggerContext.reset();
        assertNull(logger.getAppender(appenderName));

        // simulate appender re-added by some other part of the reset logic
        mockAddedAppender(appenderName);
        appender = logger.getAppender(appenderName);
        assertNotNull(appender);
        assertFalse(appender.getCopyOfAttachedFiltersList().contains(filter1));

        // reset complete should restore the added filter on the new appender
        tracker.onResetComplete(loggerContext);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter1));
    }
}
