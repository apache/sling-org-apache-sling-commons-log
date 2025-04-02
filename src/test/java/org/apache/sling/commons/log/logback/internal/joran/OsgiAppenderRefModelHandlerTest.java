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
package org.apache.sling.commons.log.logback.internal.joran;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.commons.log.logback.internal.AppenderOrigin;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.AppenderAttachable;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class OsgiAppenderRefModelHandlerTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private LogConfigManager logConfigManager;
    private OsgiAppenderRefModelHandler handler;

    private Context logbackContext;

    @BeforeEach
    private void beforeEach() {
        BundleContext bundleContext = context.bundleContext();
        logConfigManager = new LogConfigManager(bundleContext);

        logbackContext = new ContextBase();

        handler = new OsgiAppenderRefModelHandler(logbackContext, logConfigManager);
    }

    @AfterEach
    protected void afterEach() {
        if (logConfigManager != null) {
            logConfigManager.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModelHandler#getSupportedModelClass()}.
     */
    @Test
    void testGetSupportedModelClass() {
        assertEquals(OsgiAppenderRefModel.class, handler.getSupportedModelClass());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModelHandler#handle(ch.qos.logback.core.model.processor.ModelInterpretationContext, ch.qos.logback.core.model.Model)}.
     */
    @Test
    void testHandle() {
        ModelInterpretationContext mic = new ModelInterpretationContext(logbackContext);

        @SuppressWarnings("unchecked")
        AppenderAttachable<ILoggingEvent> logger = (AppenderAttachable<ILoggingEvent>)LoggerFactory.getLogger(getClass());
        mic.pushObject(logger);

        OsgiAppenderRefModel model = new OsgiAppenderRefModel();
        model.setTag("appender-ref-osgi");
        model.setRef("appender1");
        assertDoesNotThrow(() -> handler.handle(mic, model));
        Set<String> loggerNames = logConfigManager.getLoggerNamesForKnownAppender(AppenderOrigin.JORAN_OSGI, "appender1");
        assertTrue(loggerNames.contains(getClass().getName()));
    }

    @Test
    void testHandleWithWrongTopOfStackObject() {
        ModelInterpretationContext mic = new ModelInterpretationContext(logbackContext);

        Appender<ILoggingEvent> appender1 = new ListAppender<>();
        Map<String, Appender<ILoggingEvent>> appenderBag = new HashMap<>();
        appenderBag.put("appender1", appender1);
        mic.getObjectMap().put(JoranConstants.APPENDER_BAG, appenderBag);

        mic.pushObject("invalid");

        OsgiAppenderRefModel model = new OsgiAppenderRefModel();
        model.setTag("appender-ref-osgi");
        model.setRef("appender1");
        assertDoesNotThrow(() -> handler.handle(mic, model));
        // verify the error status was reported
        assertTrue(logbackContext.getStatusManager()
                .getCopyOfStatusList()
                .stream()
                .anyMatch(s -> "Could not find an AppenderAttachable at the top of execution stack. Near <appender-ref-osgi> at line 0".equals(s.getMessage())),
        "Expected error status msg");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModelHandler#attachOsgiReferencedAppenders(ch.qos.logback.core.model.processor.ModelInterpretationContext, org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel, ch.qos.logback.core.spi.AppenderAttachable)}.
     */
    @Test
    void testAttachOsgiReferencedAppenders() {
        ModelInterpretationContext mic = new ModelInterpretationContext(logbackContext);

        @SuppressWarnings("unchecked")
        AppenderAttachable<ILoggingEvent> logger = (AppenderAttachable<ILoggingEvent>)LoggerFactory.getLogger(getClass());
        mic.pushObject(logger);

        OsgiAppenderRefModel model = new OsgiAppenderRefModel();
        model.setTag("appender-ref-osgi");
        model.setRef("appender1");

        handler.attachOsgiReferencedAppenders(mic, model, logger);
        Set<String> loggerNames = logConfigManager.getLoggerNamesForKnownAppender(AppenderOrigin.JORAN_OSGI, "appender1");
        assertTrue(loggerNames.contains(getClass().getName()));
    }

    @Test
    void testAttachOsgiReferencedAppendersWithNotLoggerAttachable() {
        ModelInterpretationContext mic = new ModelInterpretationContext(logbackContext);

        @SuppressWarnings("unchecked")
        AppenderAttachable<ILoggingEvent> logger = Mockito.mock(AppenderAttachable.class);
        mic.pushObject(logger);

        OsgiAppenderRefModel model = new OsgiAppenderRefModel();
        model.setTag("appender-ref-osgi");
        model.setRef("appender1");

        handler.attachOsgiReferencedAppenders(mic, model, logger);
        // verify the error status was reported
        assertTrue(logbackContext.getStatusManager()
                .getCopyOfStatusList()
                .stream()
                .anyMatch(s -> "Failed to add osgi appender named [appender1] as the attachable is not a Logger.".equals(s.getMessage())),
        "Expected error status msg");
    }

}
