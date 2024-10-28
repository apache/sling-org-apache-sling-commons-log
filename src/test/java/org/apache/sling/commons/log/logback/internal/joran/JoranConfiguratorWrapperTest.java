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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.log.helpers.ReflectionTools;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelHandlerFactoryMethod;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.PropertyContainer;

/**
 *
 */
class JoranConfiguratorWrapperTest {
    private LogConfigManager logConfigManager;
    private JoranConfiguratorWrapper wrapper;

    @BeforeEach
    protected void beforeEach() {
        logConfigManager = Mockito.mock(LogConfigManager.class);
        wrapper = new JoranConfiguratorWrapper(logConfigManager);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.JoranConfiguratorWrapper#buildModelInterpretationContext()}.
     */
    @Test
    void testBuildModelInterpretationContext() {
        LogConfigManager logConfigManager = Mockito.mock(LogConfigManager.class);
        JoranConfiguratorWrapper wrapper = new JoranConfiguratorWrapper(logConfigManager);

        // verify the LogConfigManager is called to possibly add substitution properties
        wrapper.buildModelInterpretationContext();
        Mockito.verify(logConfigManager, times(1)).addSubsitutionProperties(any(PropertyContainer.class));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.JoranConfiguratorWrapper#addModelHandlerAssociations(ch.qos.logback.core.model.processor.DefaultProcessor)}.
     */
    @Test
    void testAddModelHandlerAssociationsDefaultProcessor() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        DefaultProcessor defaultProcessor = new DefaultProcessor(loggerContext, mic);
        wrapper.addModelHandlerAssociations(defaultProcessor);

        // verify that the handler is now our wrapped variant
        @SuppressWarnings("unchecked")
        Map<Class<? extends Model>, ModelHandlerFactoryMethod> modelClassToHandlerMap =
            ReflectionTools.getFieldWithReflection(defaultProcessor, "modelClassToHandlerMap", HashMap.class);
        ModelHandlerFactoryMethod modelHandlerFactoryMethod = modelClassToHandlerMap.get(AppenderRefModel.class);
        ModelHandlerBase make = modelHandlerFactoryMethod.make(loggerContext, mic);
        assertTrue(make instanceof AppenderRefModelHandlerWrapper);
    }

}
