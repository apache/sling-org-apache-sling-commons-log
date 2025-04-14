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

import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import static org.mockito.Mockito.times;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class ConfigResetRequestHandlerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private LogConfigManager logConfigManager;
    private ConfigResetRequestHandler handler;

    @BeforeEach
    protected void beforeEach() throws Exception {
        logConfigManager = Mockito.mock(LogConfigManager.class);

        handler = Mockito.spy(new ConfigResetRequestHandler(logConfigManager));
        handler = (ConfigResetRequestHandler) context.registerService(EventHandler.class, handler);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.ConfigResetRequestHandler#handleEvent(org.osgi.service.event.Event)}.
     */
    @Test
    void testHandleEvent() {
        // mock firing of event
        EventAdmin eventAdmin = context.getService(EventAdmin.class);
        Event mockEvent = Mockito.mock(Event.class);
        eventAdmin.sendEvent(mockEvent);

        // verify the handler was called
        Mockito.verify(handler, times(1)).handleEvent(mockEvent);
        Mockito.verify(logConfigManager, times(1)).configChanged();
    }
}
