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

import java.io.StringReader;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import org.apache.sling.commons.log.logback.ConfigProvider;
import org.apache.sling.commons.log.logback.internal.ConfigSourceTracker.ConfigSourceInfo;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class ConfigSourceTrackerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private LogConfigManager manager;
    private ConfigSourceTracker tracker;
    private ServiceReference<?> serviceRef;
    private ServiceReference<?> serviceRef2;

    private ConfigProvider configProvider;
    private String configProvider2;

    @BeforeEach
    protected void beforeEach() throws InvalidSyntaxException {
        BundleContext bundleContext = context.bundleContext();
        manager = new LogConfigManager(bundleContext);

        tracker = new ConfigSourceTracker(bundleContext, manager);

        configProvider = Mockito.spy(new ConfigProvider() {
            @Override
            public @NotNull InputSource getConfigSource() {
                return new InputSource(new StringReader("<include></include>"));
            }
        });
        ServiceRegistration<?> serviceReg =
                bundleContext.registerService(ConfigProvider.class, configProvider, new Hashtable<>(Map.of()));
        serviceRef = serviceReg.getReference();

        configProvider2 = "<include></include>";
        ServiceRegistration<?> serviceReg2 = bundleContext.registerService(
                String.class, configProvider2, new Hashtable<>(Map.of(ConfigSourceTracker.PROP_LOGBACK_CONFIG, true)));
        serviceRef2 = serviceReg2.getReference();
    }

    @AfterEach
    protected void afterEach() {
        if (manager != null) {
            manager.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigSourceTracker#close()}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testClose() {
        tracker.addingService((ServiceReference<Object>) serviceRef);
        assertFalse(tracker.getSources().isEmpty());
        tracker.close();
        assertTrue(tracker.getSources().isEmpty());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigSourceTracker#getSources()}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testGetSources() {
        tracker.addingService((ServiceReference<Object>) serviceRef2);
        Collection<ConfigSourceInfo> sources = tracker.getSources();
        assertEquals(1, sources.size());

        // exercise the ConfigSourceInfo methods
        ConfigSourceInfo source = sources.iterator().next();
        assertNotNull(source.getConfigProvider());
        assertSame(serviceRef2, source.getReference());
        assertNotNull(source.getSourceAsEscapedString());
        assertNotNull(source.getSourceAsString());
        assertNotNull(source.toString());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigSourceTracker#addingService(org.osgi.framework.ServiceReference)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testAddingService() {
        // service as ConfigProvider
        assertNotNull(tracker.addingService((ServiceReference<Object>) serviceRef));
        assertEquals(1, tracker.getSources().size());

        // service as String
        assertNotNull(tracker.addingService((ServiceReference<Object>) serviceRef2));
        assertEquals(2, tracker.getSources().size());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigSourceTracker#modifiedService(org.osgi.framework.ServiceReference, org.apache.sling.commons.log.logback.ConfigProvider)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testModifiedService() {
        ConfigProvider svc = tracker.addingService((ServiceReference<Object>) serviceRef);
        assertDoesNotThrow(() -> tracker.modifiedService((ServiceReference<Object>) serviceRef, svc));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigSourceTracker#removedService(org.osgi.framework.ServiceReference, org.apache.sling.commons.log.logback.ConfigProvider)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testRemovedServiceServiceReferenceOfConfigProviderConfigProvider() {
        ConfigProvider svc = tracker.addingService((ServiceReference<Object>) serviceRef);
        assertFalse(tracker.getSources().isEmpty());

        tracker.removedService((ServiceReference<Object>) serviceRef, svc);
        assertTrue(tracker.getSources().isEmpty());

        // remove again does nothing
        tracker.removedService((ServiceReference<Object>) serviceRef, svc);
        assertTrue(tracker.getSources().isEmpty());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigSourceTracker#onResetStart(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnResetStart() {
        LoggerContext logbackContext = new LoggerContext();
        tracker.onResetStart(logbackContext);
        assertEquals(tracker, logbackContext.getObject(ConfigSourceTracker.class.getName()));
    }
}
