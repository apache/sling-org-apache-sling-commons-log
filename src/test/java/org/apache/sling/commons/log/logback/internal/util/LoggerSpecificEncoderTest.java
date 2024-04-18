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
package org.apache.sling.commons.log.logback.internal.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.sling.commons.log.logback.internal.LogConfig;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayoutOsgi;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 *
 */
class LoggerSpecificEncoderTest {

    private LoggerSpecificEncoder encoder;

    @BeforeEach
    protected void beforeEach() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        PatternLayoutOsgi defaultLayout = new PatternLayoutOsgi();
        defaultLayout.setPattern(LogConstants.LOG_PATTERN_DEFAULT);
        defaultLayout.setContext(loggerContext);
        defaultLayout.start();

        encoder = new LoggerSpecificEncoder(defaultLayout);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.LoggerSpecificEncoder#encode(ch.qos.logback.classic.spi.ILoggingEvent)}.
     */
    @Test
    void testEncodeILoggingEvent() {
        ILoggingEvent levent = mockLoggingEvent();
        byte[] bytes = encoder.encode(levent);
        assertNotNull(bytes);
        assertTrue(new String(bytes).contains(" *INFO* [main] org.apache.sling.commons.log.logback.internal.util.LoggerSpecificEncoderTest Something happened"));
    }

    /**
     * @return
     */
    protected ILoggingEvent mockLoggingEvent() {
        return mockLoggingEvent("Something happened", getClass().getName());
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
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.LoggerSpecificEncoder#addLogConfig(org.apache.sling.commons.log.logback.internal.LogConfig)}.
     */
    @Test
    void testAddLogConfig() {
        LogConfig logConfig = Mockito.mock(LogConfig.class);
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        PatternLayoutOsgi layout = new PatternLayoutOsgi();
        layout.setPattern("%msg");
        layout.setContext(loggerContext);
        layout.start();
        Mockito.doReturn(layout).when(logConfig).createLayout(loggerContext);
        Mockito.doReturn(Set.of(getClass().getName())).when(logConfig).getCategories();

        encoder.addLogConfig(logConfig);

        // now the logging event should use the PatternLayout from the LogConfig
        //  instead of the default one
        ILoggingEvent levent = mockLoggingEvent();
        byte[] bytes = encoder.encode(levent);
        assertNotNull(bytes);
        assertEquals("Something happened", new String(bytes));
    }

    protected static Stream<Arguments> addLogConfigForBestMatchArgs() {
        return Stream.of(
                Arguments.of(new LinkedHashSet<>(List.of(LoggerSpecificEncoderTest.class.getName())), true),
                    Arguments.of(new LinkedHashSet<>(List.of("org.apache.sling.commons.log")), true),
                    Arguments.of(new LinkedHashSet<>(List.of("org.apache.sling.commons.log.logback2")), false),
                    Arguments.of(new LinkedHashSet<>(List.of("org.apache.sling.commons.log.logback.internal.util.LoggerSpecif")), false),
                    Arguments.of(new LinkedHashSet<>(List.of("org.apache.sling.commons.log", "org.apache.sling.commons.log.logback.internal.util")), true),
                    Arguments.of(new LinkedHashSet<>(List.of("org.apache.sling.commons.log", "org.apache.sling.commons.log.logback.internal.util", "org.apache.sling.commons.log.logback.internal")), true)
                );
    }
    @ParameterizedTest
    @MethodSource("addLogConfigForBestMatchArgs")
    void testAddLogConfig(Set<String> categories, boolean expected) {
        encoder.setCharset(StandardCharsets.UTF_8);

        LogConfig logConfig = Mockito.mock(LogConfig.class);
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        PatternLayoutOsgi layout = new PatternLayoutOsgi();
        layout.setPattern("%msg");
        layout.setContext(loggerContext);
        layout.start();
        Mockito.doReturn(layout).when(logConfig).createLayout(loggerContext);
        Mockito.doReturn(categories)
            .when(logConfig).getCategories();

        encoder.addLogConfig(logConfig);

        // now the logging event should use the PatternLayout from the LogConfig
        //  instead of the default one
        ILoggingEvent levent = mockLoggingEvent();
        byte[] bytes = encoder.encode(levent);
        assertNotNull(bytes);
        assertEquals(expected, "Something happened".equals(new String(bytes)));
    }


    @Test
    void testShouldReturnWithDefaultLayoutForNoConfigs() {
        LoggerSpecificEncoder tested = new LoggerSpecificEncoder(new PrefixTestLayout("DEFAULT:"));
        ILoggingEvent mockTestEvent = mockLoggingEvent("test message", "org.apache.sling.testing.FooBar");

        assertThat(tested.encode(mockTestEvent), is(equalTo("DEFAULT:test message".getBytes())));
    }

    @Test
    void testShouldIgnoreNonmatchingLayoutCategories() {
        LoggerSpecificEncoder tested = new LoggerSpecificEncoder(new PrefixTestLayout("DEFAULT:"));
        ILoggingEvent mockTestEvent = mockLoggingEvent("test message", "org.apache.sling.testing.FooBar");

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(Arrays.asList("org.apache.commons", "com.initech.sling")));
        when(logConfigMock.createLayout(loggerContext)).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("DEFAULT:test message".getBytes())));
    }

    @Test
    void testShouldIgnorePartialMatchingPackageName() {
        LoggerSpecificEncoder tested = new LoggerSpecificEncoder(new PrefixTestLayout("DEFAULT:"));
        ILoggingEvent mockTestEvent = mockLoggingEvent("test message", "org.apache.sling.testing.FooBar");

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(Arrays.asList("org.apache.sling.test")));
        when(logConfigMock.createLayout(loggerContext)).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("DEFAULT:test message".getBytes())));
    }

    @Test
    void testShouldUseExactMatchingCategoryLayout() {
        LoggerSpecificEncoder tested = new LoggerSpecificEncoder(new PrefixTestLayout("DEFAULT:"));
        ILoggingEvent mockTestEvent = mockLoggingEvent("test message", "org.apache.sling.testing.FooBar");

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(Arrays.asList("org.apache.sling.testing.FooBar")));
        when(logConfigMock.createLayout(loggerContext)).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("INITECH:test message".getBytes())));
    }

    @Test
    void testShouldUseInheritedCategoryLayout() {
        LoggerSpecificEncoder tested = new LoggerSpecificEncoder(new PrefixTestLayout("DEFAULT:"));
        ILoggingEvent mockTestEvent = mockLoggingEvent("test message", "org.apache.sling.testing.FooBar");

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(Arrays.asList("org.apache")));
        when(logConfigMock.createLayout(loggerContext)).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("INITECH:test message".getBytes())));
    }

    /**
     * Simple partial implementation of {@link PatternLayout} that redirects all method calls that are not explicitly extended to an
     * underlying mock (available as a protected {@link PrefixTestLayout#wrapped} field).
     */
    private static class PrefixTestLayout extends AbstractPatternLayoutWrapper {

        private final String prefix;

        private PrefixTestLayout(String prefix) {
            super(mock(PatternLayoutOsgi.class));
            this.prefix = prefix;
        }

        @Override
        public String doLayout(ILoggingEvent event) {
            return prefix + event.getMessage();
        }
    }

}
