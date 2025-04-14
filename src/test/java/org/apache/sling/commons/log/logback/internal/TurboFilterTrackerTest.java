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

import java.util.Hashtable;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class TurboFilterTrackerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private TurboFilterTracker tracker;
    private TurboFilter turboFilter;
    private ServiceReference<TurboFilter> serviceRef;

    @BeforeEach
    protected void beforeEach() {
        BundleContext bundleContext = context.bundleContext();

        tracker = new TurboFilterTracker(bundleContext);

        turboFilter = Mockito.spy(new TurboFilter() {
            @Override
            public FilterReply decide(
                    Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
                return FilterReply.NEUTRAL;
            }
        });
        ServiceRegistration<TurboFilter> serviceReg =
                bundleContext.registerService(TurboFilter.class, turboFilter, new Hashtable<>());
        serviceRef = serviceReg.getReference();
    }

    @AfterEach
    protected void afterEach() {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.TurboFilterTracker#close()}.
     */
    @Test
    void testClose() {
        tracker.addingService(serviceRef);
        assertFalse(tracker.getFilters().isEmpty());

        tracker.close();
        assertTrue(tracker.getFilters().isEmpty());
        tracker = null;
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.TurboFilterTracker#addingService(org.osgi.framework.ServiceReference)}.
     */
    @Test
    void testAddingService() {
        tracker.addingService(serviceRef);
        Mockito.verify(turboFilter, times(1)).start();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertTrue(loggerContext.getTurboFilterList().contains(turboFilter));
    }

    @Test
    void testAddingServiceForAlreadyExistingFilter() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            loggerContext.getTurboFilterList().add(turboFilter);

            // adding again does nothing new
            Mockito.reset(turboFilter);
            tracker.addingService(serviceRef);
            Mockito.verify(turboFilter, times(0)).start();
        } finally {
            loggerContext.getTurboFilterList().remove(turboFilter);
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.TurboFilterTracker#removedService(org.osgi.framework.ServiceReference, ch.qos.logback.classic.turbo.TurboFilter)}.
     */
    @Test
    void testRemovedService() {
        tracker.addingService(serviceRef);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertTrue(loggerContext.getTurboFilterList().contains(turboFilter));

        tracker.removedService(serviceRef, turboFilter);
        assertFalse(loggerContext.getTurboFilterList().contains(turboFilter));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.TurboFilterTracker#getFilters()}.
     */
    @Test
    void testGetFilters() {
        tracker.addingService(serviceRef);
        Map<ServiceReference<TurboFilter>, TurboFilter> filters = tracker.getFilters();
        assertTrue(filters.containsKey(serviceRef));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.TurboFilterTracker#onResetStart(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnResetStart() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // first add a turbo filter
        tracker.addingService(serviceRef);
        assertTrue(loggerContext.getTurboFilterList().contains(turboFilter));

        // reset the logger context should clear out the added turbo filter
        loggerContext.reset();
        assertFalse(loggerContext.getTurboFilterList().contains(turboFilter));

        // reset callback should restore the added turbo filter
        tracker.onResetStart(loggerContext);
        assertTrue(loggerContext.getTurboFilterList().contains(turboFilter));
    }
}
