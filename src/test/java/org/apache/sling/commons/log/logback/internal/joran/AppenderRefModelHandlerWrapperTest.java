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
package org.apache.sling.commons.log.logback.internal.joran;

import java.util.HashMap;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.apache.sling.commons.log.logback.internal.AppenderOrigin;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;

/**
 *
 */
class AppenderRefModelHandlerWrapperTest {

    private LoggerContext loggerContext;
    private LogConfigManager logConfigManager;
    private AppenderRefModelHandlerWrapper wrapper;

    @BeforeEach
    protected void beforeEach() {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        logConfigManager = Mockito.mock(LogConfigManager.class);

        wrapper = new AppenderRefModelHandlerWrapper(loggerContext, logConfigManager);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.AppenderRefModelHandlerWrapper#handle(ch.qos.logback.core.model.processor.ModelInterpretationContext, ch.qos.logback.core.model.Model)}.
     */
    @Test
    void testHandle() {
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        HashMap<String, Appender<ILoggingEvent>> appenderBag = new HashMap<>();
        appenderBag.put("testappender1", new ListAppender<>());
        mic.getObjectMap().put(JoranConstants.APPENDER_BAG, appenderBag);

        Logger logger = LoggerFactory.getLogger(getClass());
        mic.pushObject(logger);

        AppenderRefModel model = new AppenderRefModel();
        model.setRef("testappender1");

        assertDoesNotThrow(() -> wrapper.handle(mic, model));
        // verify the appender-ref info was tracked
        Mockito.verify(logConfigManager, times(1))
                .addedAppenderRef(
                        AppenderOrigin.JORAN, "testappender1", getClass().getName());
    }

    @Test
    void testHandleWithNullAppender() {
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        HashMap<String, Appender<ILoggingEvent>> appenderBag = new HashMap<>();
        mic.getObjectMap().put(JoranConstants.APPENDER_BAG, appenderBag);

        Logger logger = LoggerFactory.getLogger(getClass());
        mic.pushObject(logger);

        AppenderRefModel model = new AppenderRefModel();
        model.setRef("testappender1");

        assertDoesNotThrow(() -> wrapper.handle(mic, model));
        // verify the appender-ref info was tracked
        Mockito.verify(logConfigManager, times(0))
                .addedAppenderRef(
                        AppenderOrigin.JORAN, "testappender1", getClass().getName());
    }

    @Test
    void testHandleWithNotAppenderAttachableObject() {
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        HashMap<String, Appender<ILoggingEvent>> appenderBag = new HashMap<>();
        appenderBag.put("testappender1", new ListAppender<>());
        mic.getObjectMap().put(JoranConstants.APPENDER_BAG, appenderBag);

        mic.pushObject(new Object());

        AppenderRefModel model = new AppenderRefModel();
        model.setRef("testappender1");

        assertDoesNotThrow(() -> wrapper.handle(mic, model));
        // verify the appender-ref info was tracked
        Mockito.verify(logConfigManager, times(0))
                .addedAppenderRef(
                        AppenderOrigin.JORAN, "testappender1", getClass().getName());
    }

    @Test
    void testHandleWithNotLoggerObject() {
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        HashMap<String, Appender<ILoggingEvent>> appenderBag = new HashMap<>();
        appenderBag.put("testappender1", new ListAppender<>());
        mic.getObjectMap().put(JoranConstants.APPENDER_BAG, appenderBag);

        mic.pushObject(Mockito.mock(AppenderAttachable.class));

        AppenderRefModel model = new AppenderRefModel();
        model.setRef("testappender1");

        assertDoesNotThrow(() -> wrapper.handle(mic, model));
        // verify the appender-ref info was tracked
        Mockito.verify(logConfigManager, times(0))
                .addedAppenderRef(
                        AppenderOrigin.JORAN, "testappender1", getClass().getName());
    }
}
