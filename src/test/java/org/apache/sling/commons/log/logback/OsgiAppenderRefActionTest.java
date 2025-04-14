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
package org.apache.sling.commons.log.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class OsgiAppenderRefActionTest {

    private OsgiAppenderRefAction action;

    @BeforeEach
    protected void beforeEach() {
        action = new OsgiAppenderRefAction();
        action.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.OsgiAppenderRefAction#validPreconditions(ch.qos.logback.core.joran.spi.SaxEventInterpretationContext, java.lang.String, org.xml.sax.Attributes)}.
     */
    @Test
    void testValidPreconditions() {
        SaxEventInterpretationContext intercon = Mockito.mock(SaxEventInterpretationContext.class);
        String name = "appender-osgi-ref";

        // empty attributes
        Attributes attributes = new AttributesImpl();
        assertFalse(action.validPreconditions(intercon, name, attributes));

        // valid attributes
        attributes = Mockito.mock(Attributes.class);
        Mockito.doReturn("appender1").when(attributes).getValue(JoranConstants.REF_ATTRIBUTE);
        assertTrue(action.validPreconditions(intercon, name, attributes));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.OsgiAppenderRefAction#buildCurrentModel(ch.qos.logback.core.joran.spi.SaxEventInterpretationContext, java.lang.String, org.xml.sax.Attributes)}.
     */
    @Test
    void testBuildCurrentModel() {
        SaxEventInterpretationContext intercon = Mockito.mock(SaxEventInterpretationContext.class);
        String name = "appender-osgi-ref";
        Attributes attributes = Mockito.mock(Attributes.class);
        Mockito.doReturn("appender1").when(attributes).getValue(JoranConstants.REF_ATTRIBUTE);
        Model buildCurrentModel = action.buildCurrentModel(intercon, name, attributes);
        assertTrue(buildCurrentModel instanceof OsgiAppenderRefModel);
    }
}
