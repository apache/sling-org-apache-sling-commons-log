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
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class LogConfiguratorTest {
    protected final OsgiContext context = new OsgiContext();

    private LogConfigurator configurator = new LogConfigurator();
    private LogConfigManager mgr = new LogConfigManager(context.bundleContext());

    @AfterEach
    protected void afterEach() {
        if (mgr != null) {
            mgr.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LogConfigurator#setLogConfigManager(org.apache.sling.commons.log.logback.internal.LogConfigManager)}.
     */
    @Test
    void testSetLogConfigManager() {
        configurator.setLogConfigManager(mgr);
        assertEquals(mgr, configurator.getLogConfigManager());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LogConfigurator#getLogConfigManager()}.
     */
    @Test
    void testGetLogConfigManager() {
        assertNull(configurator.getLogConfigManager());
        configurator.setLogConfigManager(mgr);
        assertEquals(mgr, configurator.getLogConfigManager());
    }
}
