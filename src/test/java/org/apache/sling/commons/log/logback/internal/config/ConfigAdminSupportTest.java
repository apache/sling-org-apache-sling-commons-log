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

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class ConfigAdminSupportTest {
    protected final OsgiContext context = new OsgiContext();

    protected ConfigAdminSupport configAdminSupport;

    @BeforeEach
    protected void beforeEach() {
        configAdminSupport = new ConfigAdminSupport();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigAdminSupport#start(org.osgi.framework.BundleContext, org.apache.sling.commons.log.logback.internal.LogConfigManager)}.
     */
    @Test
    void testStart() throws InvalidSyntaxException {
        BundleContext bundleContext = context.bundleContext();
        LogConfigManager logConfigManager = new LogConfigManager(bundleContext);

        configAdminSupport.start(bundleContext, logConfigManager);

        // verify the services were registered
        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.PID);
        assertEquals(
                1,
                bundleContext.getServiceReferences(ManagedService.class, filter).size());

        filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.FACTORY_PID_WRITERS);
        assertEquals(
                1,
                bundleContext
                        .getServiceReferences(ManagedServiceFactory.class, filter)
                        .size());

        filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.FACTORY_PID_CONFIGS);
        assertEquals(
                1,
                bundleContext
                        .getServiceReferences(ManagedServiceFactory.class, filter)
                        .size());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigAdminSupport#stop()}.
     */
    @Test
    void testStop() throws InvalidSyntaxException {
        testStart();

        configAdminSupport.stop();

        // verify the services were unregistered
        BundleContext bundleContext = context.bundleContext();
        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.PID);
        assertEquals(
                0,
                bundleContext.getServiceReferences(ManagedService.class, filter).size());

        filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.FACTORY_PID_WRITERS);
        assertEquals(
                0,
                bundleContext
                        .getServiceReferences(ManagedServiceFactory.class, filter)
                        .size());

        filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.FACTORY_PID_CONFIGS);
        assertEquals(
                0,
                bundleContext
                        .getServiceReferences(ManagedServiceFactory.class, filter)
                        .size());
    }
}
