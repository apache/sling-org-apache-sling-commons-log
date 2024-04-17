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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.commons.log.helpers.LogCapture;
import org.apache.sling.commons.log.logback.internal.mock.MockLoggerFactory;
import org.apache.sling.commons.log.logback.internal.mock.MockSLF4JServiceProvider;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactoryFriend;
import org.slf4j.spi.SLF4JServiceProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LogbackServiceProvider;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class SLF4JServiceProviderTrackerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private SLF4JServiceProviderTracker tracker;

    private ServiceReference<SLF4JServiceProvider> serviceRef;

    @BeforeEach
    protected void beforeEach() {
        System.setProperty(LogConstants.SLING_HOME, new File("target").getPath());

        tracker = new SLF4JServiceProviderTracker(context.bundleContext());
        ServiceRegistration<SLF4JServiceProvider> serviceReg = context.bundleContext().registerService(SLF4JServiceProvider.class, new LogbackServiceProvider(), new Hashtable<>());
        serviceRef = serviceReg.getReference();
    }

    @AfterEach
    protected void afterEach() {
        // cleanup to not interfere with other tests
        System.clearProperty(LogConstants.SLING_HOME);

        // reset it so we get a fresh copy for each test
        LoggerFactoryFriend.reset();
    }

    /**
     * Register and return a mock SLF4JServiceProvider service
     */
    protected ServiceReference<SLF4JServiceProvider> mockSLF4JServiceProvider() {
        return mockSLF4JServiceProvider(new Hashtable<>());
    }
    protected ServiceReference<SLF4JServiceProvider> mockSLF4JServiceProvider(Dictionary<String, ?> properties) {
        SLF4JServiceProvider mockServiceProvider = new MockSLF4JServiceProvider();
        ServiceRegistration<SLF4JServiceProvider> serviceReg = context.bundleContext().registerService(SLF4JServiceProvider.class, mockServiceProvider, properties);
        return serviceReg.getReference();
    }

    /**
     * Switch SLF4J to the mock SLF4JServiceProvider impl
     */
    protected void mockLoggerFactory() {
        try {
            System.setProperty(LoggerFactory.PROVIDER_PROPERTY_KEY, MockSLF4JServiceProvider.class.getName());
            LoggerFactoryFriend.reset();
            ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
            assertTrue(iLoggerFactory instanceof MockLoggerFactory);
        } finally {
            System.clearProperty(LoggerFactory.PROVIDER_PROPERTY_KEY);
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SLF4JServiceProviderTracker#addingService(org.osgi.framework.ServiceReference)}.
     */
    @Test
    void testAddingServiceWithMockImpl() throws Exception {
        ServiceReference<SLF4JServiceProvider> serviceRef2 = mockSLF4JServiceProvider();

        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(tracker.getClass().getName(), true)) {
            tracker.addingService(serviceRef2);

            // verify the msg was logged
            capture.assertContains(Level.DEBUG,
                    "addingService is not the LogbackServiceProvider: org.apache.sling.commons.log.logback.internal.mock.MockSLF4JServiceProvider");
        }
    }

    @Test
    void testAddingServiceToSwitchFromMockImplToLogbackImpl() {
        mockLoggerFactory();

        tracker.addingService(serviceRef);

        // verify switch to the logback impl
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        assertTrue(iLoggerFactory instanceof LoggerContext);
    }

    @Test
    void testAddingServiceToSwitchFromMockImplToStillMockImpl() {
        mockLoggerFactory();

        try {
            System.setProperty(LoggerFactory.PROVIDER_PROPERTY_KEY, MockSLF4JServiceProvider.class.getName());
            tracker.addingService(serviceRef);

            // verify still the mock impl
            ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
            assertTrue(iLoggerFactory instanceof MockLoggerFactory);
        } finally {
            System.clearProperty(LoggerFactory.PROVIDER_PROPERTY_KEY);
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SLF4JServiceProviderTracker#removedService(org.osgi.framework.ServiceReference, org.slf4j.spi.SLF4JServiceProvider)}.
     */
    @Test
    void testRemovedServiceWithMockImpl() throws Exception {
        ServiceReference<SLF4JServiceProvider> serviceRef2 = mockSLF4JServiceProvider();
        SLF4JServiceProvider service = context.bundleContext().getService(serviceRef2);

        tracker.addingService(serviceRef);
        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(tracker.getClass().getName(), true)) {
            tracker.removedService(serviceRef2, service);

            // verify the msg was logged
            capture.assertContains(Level.DEBUG,
                    "removedService is not the LogbackServiceProvider: org.apache.sling.commons.log.logback.internal.mock.MockSLF4JServiceProvider");
        }
    }
    @Test
    void testRemovedService() throws Exception {
        tracker.addingService(serviceRef);
        SLF4JServiceProvider service = context.bundleContext().getService(serviceRef);

        // verify that the old console appender was detached
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.clear();
        tracker.removedService(serviceRef, service);
        List<Status> copyOfStatusList = statusManager.getCopyOfStatusList();
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> s.getMessage().equals("detaching appender CONSOLE for ROOT")),
                "Expected detaching appender message");

        // call again should do nothing
        statusManager.clear();
        tracker.removedService(serviceRef, service);
        copyOfStatusList = statusManager.getCopyOfStatusList();
        assertTrue(copyOfStatusList.isEmpty());
    }

}
