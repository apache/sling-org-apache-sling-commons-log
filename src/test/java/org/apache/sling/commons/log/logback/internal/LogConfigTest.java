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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.PostCompileProcessor;
import org.apache.sling.commons.log.helpers.LogCapture;
import org.apache.sling.commons.log.logback.internal.LogConfig.LogWriterProvider;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

/**
 *
 */
class LogConfigTest {
    private LogConfig logConfig;
    private LogWriterProvider logWriterProvider;

    @BeforeEach
    protected void beforeEach() {
        logWriterProvider = Mockito.mock(LogWriterProvider.class);
        logConfig = new LogConfig(
                logWriterProvider,
                LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.logger1"),
                Level.WARN,
                "logwriter1",
                true,
                LogConstants.PID,
                false);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#getConfigPid()}.
     */
    @Test
    void testGetConfigPid() {
        assertEquals(LogConstants.PID, logConfig.getConfigPid());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#getCategories()}.
     */
    @Test
    void testGetCategories() {
        assertEquals(Set.of("log.logger1"), logConfig.getCategories());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#getLogLevel()}.
     */
    @Test
    void testGetLogLevel() {
        assertEquals(Level.WARN, logConfig.getLogLevel());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#getLogWriterName()}.
     */
    @Test
    void testGetLogWriterName() {
        assertEquals("logwriter1", logConfig.getLogWriterName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#isAppenderDefined()}.
     */
    @Test
    void testIsAppenderDefined() {
        assertTrue(logConfig.isAppenderDefined());

        // try with null logWriterName
        logConfig = new LogConfig(
                logWriterProvider,
                LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.logger1"),
                Level.WARN,
                null,
                true,
                LogConstants.PID,
                false);
        assertFalse(logConfig.isAppenderDefined());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#isAdditive()}.
     */
    @Test
    void testIsAdditive() {
        assertTrue(logConfig.isAdditive());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#getLogWriter()}.
     */
    @Test
    void testGetLogWriter() {
        LogWriter logWriter = logConfig.getLogWriter();
        assertNull(logWriter);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#isResetToDefault()}.
     */
    @Test
    void testIsResetToDefault() {
        assertFalse(logConfig.isResetToDefault());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#createLayout()}.
     */
    @Test
    void testCreateLayout() {
        PatternLayout pl = logConfig.createLayout((LoggerContext) LoggerFactory.getILoggerFactory());
        assertNotNull(pl);
    }

    @Test
    void testCreateLayoutWithLegacyPattern() {
        String pattern = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}";

        logConfig = new LogConfig(
                logWriterProvider,
                pattern,
                Set.of("log.logger1"),
                Level.WARN,
                "logwriter1",
                true,
                LogConstants.PID,
                false);

        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        PatternLayout pl = logConfig.createLayout((LoggerContext) LoggerFactory.getILoggerFactory());
        assertEquals(convertedPattern, pl.getPattern());
    }

    @Test
    void testCreateLayoutWithLegacyPatternAndCaughtIllegalArgumentException() throws Exception {
        String pattern = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5} {W}";

        logConfig = new LogConfig(
                logWriterProvider,
                pattern,
                Set.of("log.logger1"),
                Level.WARN,
                "logwriter1",
                true,
                LogConstants.PID,
                false);

        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(logConfig.getClass().getName(), true)) {
            PatternLayout pl = TestUtils.doWorkWithoutRootConsoleAppender(() -> {
                return logConfig.createLayout((LoggerContext) LoggerFactory.getILoggerFactory());
            });

            assertEquals(LogConstants.LOG_PATTERN_DEFAULT, pl.getPattern());

            // verify the msg was logged
            capture.assertContains(Level.WARN, "Invalid message format provided");
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#toString()}.
     */
    @Test
    void testToString() {
        assertNotNull(logConfig.toString());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfig#setPostProcessor(ch.qos.logback.core.pattern.PostCompileProcessor)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testSetPostProcessor() {
        PostCompileProcessor<ILoggingEvent> postProcessor = Mockito.mock(PostCompileProcessor.class);
        logConfig.setPostProcessor(postProcessor);

        logConfig.createLayout((LoggerContext) LoggerFactory.getILoggerFactory());
        Mockito.verify(postProcessor, times(1)).process(any(Context.class), any(Converter.class));
    }

    @Test
    void testLayout() {
        String pattern = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}";
        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        LogConfig logConfig = createConfig(pattern);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertEquals(convertedPattern, logConfig.createLayout(loggerContext).getPattern());
    }

    @Test
    void testLayoutWithNewPattern() {
        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        LogConfig logConfig = createConfig(convertedPattern);
        // Test that valid LogBack pattern are not tampered
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertEquals(convertedPattern, logConfig.createLayout(loggerContext).getPattern());
    }

    @Test
    void testMessageEscaping() {
        final LogConfig logConfig = createConfig("%message %m %msg");
        final ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getFormattedMessage()).thenReturn("foo\r\nbar");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final String result = logConfig.createLayout(loggerContext).doLayout(event);
        assertEquals("foo__bar foo__bar foo__bar", result);
    }

    @Test
    void testExceptionMessageEscaping() {
        final String[] patterns = new String[] {
            "%ex", "%exception", "%rEx", "%rootException", "%throwable", "%xEx", "%xException", "%xThrowable", "%m"
        };
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (final String p : patterns) {
            final LogConfig logConfig = createConfig(p);
            final PatternLayout layout = logConfig.createLayout(loggerContext);

            final ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
            Mockito.when(event.getFormattedMessage()).thenReturn("message");

            // single exception
            Mockito.when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(new RuntimeException("foo\r\nbar")));
            String result = layout.doLayout(event);
            assertTrue(result.contains("foo__bar"), "pattern " + p + " : " + result);

            // nested exception
            Mockito.when(event.getThrowableProxy())
                    .thenReturn(new ThrowableProxy(new RuntimeException("foo\r\nbar", new IOException("a\r\nb"))));
            result = layout.doLayout(event);
            assertTrue(result.contains("foo__bar"), "pattern " + p + " : " + result);
            assertTrue(result.contains("a__b"), "pattern " + p + " : " + result);
        }
    }

    private LogConfig createConfig(String pattern) {
        return new LogConfig(
                new DummyLogWriterProvider(),
                pattern,
                Collections.<String>emptySet(),
                Level.DEBUG,
                "test",
                false,
                null,
                false);
    }

    private static class DummyLogWriterProvider implements LogConfig.LogWriterProvider {

        @Override
        public LogWriter getLogWriter(String writerName) {
            return null;
        }
    }
}
