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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.model.processor.AppenderRefModelHandler;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class ModelHandlerWrapperFactoryTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.ModelHandlerWrapperFactory#makeAppenderRefModelHandlerInstance(ch.qos.logback.core.Context, ch.qos.logback.core.model.processor.ModelInterpretationContext)}.
     */
    @Test
    void testMakeAppenderRefModelHandlerInstance() {
        LogConfigManager logConfigManager = Mockito.mock(LogConfigManager.class);
        ModelHandlerWrapperFactory factory = new ModelHandlerWrapperFactory(logConfigManager);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        AppenderRefModelHandler instance = factory.makeAppenderRefModelHandlerInstance(loggerContext, mic);
        assertNotNull(instance);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.ModelHandlerWrapperFactory#makeOsgiAppenderRefModelHandlerInstance(ch.qos.logback.core.Context, ch.qos.logback.core.model.processor.ModelInterpretationContext)}.
     */
    @Test
    void testMakeOsgiAppenderRefModelHandlerInstance() {
        LogConfigManager logConfigManager = Mockito.mock(LogConfigManager.class);
        ModelHandlerWrapperFactory factory = new ModelHandlerWrapperFactory(logConfigManager);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        OsgiAppenderRefModelHandler instance = factory.makeOsgiAppenderRefModelHandlerInstance(loggerContext, mic);
        assertNotNull(instance);
    }
}
