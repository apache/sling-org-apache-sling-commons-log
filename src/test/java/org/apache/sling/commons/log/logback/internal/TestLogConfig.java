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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

public class TestLogConfig {

    @Test
    public void testLayout() {
        String pattern = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}";
        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        LogConfig logConfig = createConfig(pattern);
        assertEquals(convertedPattern, logConfig.createLayout().getPattern());
    }

    @Test
    public void testLayoutWithNewPattern() {
        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        LogConfig logConfig = createConfig(convertedPattern);
        // Test that valid LogBack pattern are not tampered
        assertEquals(convertedPattern, logConfig.createLayout().getPattern());
    }

    @Test
    public void testMessageEscaping() {
        final LogConfig logConfig = createConfig("%message %m %msg");
        final ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getFormattedMessage()).thenReturn("foo\r\nbar");
        final String result = logConfig.createLayout().doLayout(event);
        assertEquals("foo__bar foo__bar foo__bar", result);
    }

    @Test
    public void testExceptionMessageEscaping() {
        final String[] patterns = new String[] {"%ex", "%exception", "%rEx", "%rootException", "%throwable", "%xEx", "%xException", "%xThrowable", "%m"};
        for(final String p : patterns) {
            final LogConfig logConfig = createConfig(p);
            final PatternLayout layout = logConfig.createLayout();

            final ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
            Mockito.when(event.getFormattedMessage()).thenReturn("message");

            // single exception
            Mockito.when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(new RuntimeException("foo\r\nbar")));
            String result = layout.doLayout(event);
            assertTrue("pattern " + p + " : " + result, result.contains("foo__bar"));

            // nested exception
            Mockito.when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(new RuntimeException("foo\r\nbar", new IOException("a\r\nb"))));
            result = layout.doLayout(event);
            assertTrue("pattern " + p + " : " + result, result.contains("foo__bar"));
            assertTrue("pattern " + p + " : " + result, result.contains("a__b"));
        }
    }

    private LogConfig createConfig(String pattern) {
        return new LogConfig(new DummyLogWriterProvider(), pattern, Collections.<String> emptySet(), Level.DEBUG,
            "test", false, null, (LoggerContext) LoggerFactory.getILoggerFactory(), false);
    }

    private static class DummyLogWriterProvider implements LogConfig.LogWriterProvider {

        @Override
        public LogWriter getLogWriter(String writerName) {
            return null;
        }
    }
}
