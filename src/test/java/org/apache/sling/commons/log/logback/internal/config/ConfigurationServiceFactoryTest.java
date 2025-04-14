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
package org.apache.sling.commons.log.logback.internal.config;

import org.apache.sling.commons.log.helpers.ReflectionTools;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class ConfigurationServiceFactoryTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private LogConfigManager mgr;
    private ConfigurationServiceFactory<LogConfigurator> factory;

    @BeforeEach
    protected void beforeEach() {
        mgr = new LogConfigManager(context.bundleContext());
        factory = new ConfigurationServiceFactory<>(mgr, LogConfigurator::new);
    }

    @AfterEach
    protected void afterEach() {
        if (mgr != null) {
            mgr.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigurationServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)}.
     */
    @Test
    void testGetService() {
        @SuppressWarnings("unchecked")
        ServiceRegistration<LogConfigurator> mockSvcReg = Mockito.mock(ServiceRegistration.class);
        Bundle bundle = context.bundleContext().getBundle();
        LogConfigurator service = factory.getService(bundle, mockSvcReg);
        assertNotNull(service);
        assertEquals(1, ReflectionTools.getFieldWithReflection(factory, "useCount", Integer.class));
        assertNotNull(ReflectionTools.getFieldWithReflection(factory, "service", LogConfigurator.class));

        LogConfigurator service2 = factory.getService(bundle, mockSvcReg);
        assertSame(service2, service);
        assertEquals(2, ReflectionTools.getFieldWithReflection(factory, "useCount", Integer.class));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigurationServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, org.apache.sling.commons.log.logback.internal.config.LogConfigurator)}.
     */
    @Test
    void testUngetService() {
        @SuppressWarnings("unchecked")
        ServiceRegistration<LogConfigurator> mockSvcReg = Mockito.mock(ServiceRegistration.class);
        Bundle bundle = context.bundleContext().getBundle();
        LogConfigurator service = factory.getService(bundle, mockSvcReg);
        assertNotNull(service);
        LogConfigurator service2 = factory.getService(bundle, mockSvcReg);
        assertSame(service2, service);

        assertEquals(2, ReflectionTools.getFieldWithReflection(factory, "useCount", Integer.class));
        assertNotNull(ReflectionTools.getFieldWithReflection(factory, "service", LogConfigurator.class));
        factory.ungetService(context.bundleContext().getBundle(), mockSvcReg, service);
        assertEquals(1, ReflectionTools.getFieldWithReflection(factory, "useCount", Integer.class));
        assertNotNull(ReflectionTools.getFieldWithReflection(factory, "service", LogConfigurator.class));
        factory.ungetService(context.bundleContext().getBundle(), mockSvcReg, service);
        assertEquals(0, ReflectionTools.getFieldWithReflection(factory, "useCount", Integer.class));
        assertNull(ReflectionTools.getFieldWithReflection(factory, "service", LogConfigurator.class));
    }
}
