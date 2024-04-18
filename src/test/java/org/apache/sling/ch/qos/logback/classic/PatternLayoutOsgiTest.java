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
package org.apache.sling.ch.qos.logback.classic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayoutOsgi;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Context;

/**
 *
 */
class PatternLayoutOsgiTest {

    private PatternLayoutOsgi patternLayout;

    @BeforeEach
    private void beforeEach() {
        patternLayout = new PatternLayoutOsgi();
        Context context = new LoggerContext();
        patternLayout.setContext(context);
        patternLayout.setPattern(LogConstants.LOG_PATTERN_DEFAULT);
    }

    /**
     * Test method for {@link ch.qos.logback.classic.PatternLayoutOsgi#getDefaultConverterMap()}.
     */
    @Test
    void testGetDefaultConverterMap() {
        assertNotNull(patternLayout.getDefaultConverterMap());
    }

    protected ILoggingEvent mockLoggingEvent(String message, String loggerName) {
        String fqcn = getClass().getName();
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        Level level = Level.INFO;
        Throwable t = null;
        Object[] argArray = null;
        ILoggingEvent levent = new LoggingEvent(fqcn, logger, level, message, t, argArray);
        return levent;
    }
    /**
     * Test method for {@link ch.qos.logback.classic.PatternLayoutOsgi#doLayout(ch.qos.logback.classic.spi.ILoggingEvent)}.
     */
    @Test
    void testDoLayoutWhenNotStarted() {
        ILoggingEvent event = mockLoggingEvent("Something went wrong", getClass().getName());
        assertEquals("", patternLayout.doLayout(event));
    }
    @Test
    void testDoLayoutWhenStarted() {
        patternLayout.start();
        ILoggingEvent event = mockLoggingEvent("Something went wrong", getClass().getName());
        String output = patternLayout.doLayout(event);
        assertTrue(output.contains(" *INFO* [main] org.apache.sling.ch.qos.logback.classic.PatternLayoutOsgiTest Something went wrong"));
    }

    /**
     * Test method for {@link ch.qos.logback.classic.PatternLayoutOsgi#getPresentationHeaderPrefix()}.
     */
    @Test
    void testGetPresentationHeaderPrefix() {
        patternLayout.setOutputPatternAsHeader(true);
        assertEquals(String.format("#logback.classic pattern: %s", patternLayout.getPattern()),
                patternLayout.getPresentationHeader());
    }

}
