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
package org.apache.sling.commons.log.logback.internal;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.io.FileUtils;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicy;

/**
 *
 */
class LogWriterTest {

    private LogWriter logWriter1;
    private LogWriter logWriter2;

    @BeforeEach
    protected void beforeEach() {
        logWriter1 = new LogWriter("appenderName1", "target/logs/fileName1.log", 1, LogConstants.LOG_FILE_SIZE_DEFAULT);
        logWriter2 = new LogWriter("pid2", "appenderName2", 2, LogConstants.LOG_FILE_SIZE_DEFAULT, "target/logs/fileName2.log", true);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testConstructorWithNullFileName(String fileName) {
        LogWriter logWriter3 = new LogWriter("appenderName1", fileName, 1, LogConstants.LOG_FILE_SIZE_DEFAULT);
        assertEquals(LogConstants.FILE_NAME_CONSOLE, logWriter3.getFileName());
    }
    @Test
    void testConstructorWithNegativeLogNumber() {
        LogWriter logWriter3 = new LogWriter("appenderName1", "target/logs/fileName3.log", -1, LogConstants.LOG_FILE_SIZE_DEFAULT);
        assertEquals(LogConstants.LOG_FILE_NUMBER_DEFAULT, logWriter3.getLogNumber());
    }
    @ParameterizedTest
    @NullAndEmptySource
    void testConstructorWithNullLogRotation(String logRotation) {
        LogWriter logWriter3 = new LogWriter("appenderName1", "target/logs/fileName3.log", 1, logRotation);
        assertEquals(LogConstants.LOG_FILE_SIZE_DEFAULT, logWriter3.getLogRotation());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#getConfigurationPID()}.
     */
    @Test
    void testGetConfigurationPID() {
        assertNull(logWriter1.getConfigurationPID());

        assertEquals("pid2", logWriter2.getConfigurationPID());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#getImplicitConfigPID()}.
     */
    @Test
    void testGetImplicitConfigPID() {
        assertEquals(LogConstants.PID, logWriter1.getImplicitConfigPID());

        assertEquals(LogConstants.PID, logWriter2.getImplicitConfigPID());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#getFileName()}.
     */
    @Test
    void testGetFileName() {
        assertEquals("target/logs/fileName1.log", logWriter1.getFileName());

        assertEquals("target/logs/fileName2.log", logWriter2.getFileName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#getAppenderName()}.
     */
    @Test
    void testGetAppenderName() {
        assertEquals("appenderName1", logWriter1.getAppenderName());

        assertEquals("appenderName2", logWriter2.getAppenderName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#getLogNumber()}.
     */
    @Test
    void testGetLogNumber() {
        assertEquals(1, logWriter1.getLogNumber());

        assertEquals(2, logWriter2.getLogNumber());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#getLogRotation()}.
     */
    @Test
    void testGetLogRotation() {
        assertEquals(LogConstants.LOG_FILE_SIZE_DEFAULT, logWriter1.getLogRotation());

        assertEquals(LogConstants.LOG_FILE_SIZE_DEFAULT, logWriter2.getLogRotation());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#isImplicit()}.
     */
    @Test
    void testIsImplicit() {
        assertTrue(logWriter1.isImplicit());

        assertFalse(logWriter2.isImplicit());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#createAppender(ch.qos.logback.core.Context, ch.qos.logback.core.encoder.Encoder)}.
     */
    @Test
    void testCreateAppenderForConsole() {
        LogWriter logWriter3 = new LogWriter("appenderName1", null, 1, LogConstants.LOG_FILE_SIZE_DEFAULT);

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Encoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        Appender<ILoggingEvent> appender1 = logWriter3.createAppender(loggerContext, encoder);
        assertTrue(appender1 instanceof ConsoleAppender);
    }
    @Test
    void testCreateAppenderForTimeBasedRolling() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Encoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();

        Appender<ILoggingEvent> appender1 = logWriter1.createAppender(loggerContext, encoder);
        assertTrue(appender1 instanceof RollingFileAppender);
        RollingFileAppender<ILoggingEvent> rollingAppender1 = (RollingFileAppender<ILoggingEvent>)appender1;
        TriggeringPolicy<ILoggingEvent> triggeringPolicy1 = rollingAppender1.getTriggeringPolicy();
        assertTrue(triggeringPolicy1 instanceof TimeBasedRollingPolicy);

        Appender<ILoggingEvent> appender2 = logWriter2.createAppender(loggerContext, encoder);
        assertTrue(appender2 instanceof RollingFileAppender);
        RollingFileAppender<ILoggingEvent> rollingAppender2 = (RollingFileAppender<ILoggingEvent>)appender2;
        TriggeringPolicy<ILoggingEvent> triggeringPolicy2 = rollingAppender2.getTriggeringPolicy();
        assertTrue(triggeringPolicy2 instanceof TimeBasedRollingPolicy);
    }
    @Test
    void testCreateAppenderForTimeBasedRollingWithNotLayoutWrappingEncoder() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Encoder<ILoggingEvent> encoder = new EchoEncoder<>();

        Appender<ILoggingEvent> appender1 = logWriter1.createAppender(loggerContext, encoder);
        assertTrue(appender1 instanceof RollingFileAppender);
        RollingFileAppender<ILoggingEvent> rollingAppender1 = (RollingFileAppender<ILoggingEvent>)appender1;
        TriggeringPolicy<ILoggingEvent> triggeringPolicy1 = rollingAppender1.getTriggeringPolicy();
        assertTrue(triggeringPolicy1 instanceof TimeBasedRollingPolicy);

        Appender<ILoggingEvent> appender2 = logWriter2.createAppender(loggerContext, encoder);
        assertTrue(appender2 instanceof RollingFileAppender);
        RollingFileAppender<ILoggingEvent> rollingAppender2 = (RollingFileAppender<ILoggingEvent>)appender2;
        TriggeringPolicy<ILoggingEvent> triggeringPolicy2 = rollingAppender2.getTriggeringPolicy();
        assertTrue(triggeringPolicy2 instanceof TimeBasedRollingPolicy);
    }
    @ParameterizedTest
    @ValueSource(strings={"100",
            "1GB", "1G", "1g",
            "1MB", "1M", "1m",
            "1500KB", "1500K", "1500k"})
    void testCreateAppenderForSizeBasedRolling(String size) {
        logWriter1 = new LogWriter("appenderName1", "target/logs/fileName1.log", 1, size);

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Encoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        Appender<ILoggingEvent> appender1 = logWriter1.createAppender(loggerContext, encoder);
        assertTrue(appender1 instanceof RollingFileAppender);
        RollingFileAppender<ILoggingEvent> rollingAppender1 = (RollingFileAppender<ILoggingEvent>)appender1;
        TriggeringPolicy<ILoggingEvent> triggeringPolicy = rollingAppender1.getTriggeringPolicy();
        assertTrue(triggeringPolicy instanceof SizeBasedTriggeringPolicy);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#createFileNamePattern(java.lang.String, java.lang.String)}.
     */
    @Test
    void testCreateFileNamePattern() {
        assertEquals("logs/test.log.%d{yyyy-MM-dd}", LogWriter.createFileNamePattern("logs/test.log", 
                LogConstants.LOG_FILE_SIZE_DEFAULT));

        assertEquals("logs/test.log%d{yyyy-MM-dd}", LogWriter.createFileNamePattern("logs/test.log", 
                "yyyy-MM-dd"));

        assertEquals("logs/test.logtest1.%d{yyyy-MM-dd}", LogWriter.createFileNamePattern("logs/test.log", 
                "test1.%d{yyyy-MM-dd}"));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogWriter#toString()}.
     */
    @Test
    void testToString() {
        assertNotNull(logWriter1.toString());

        assertNotNull(logWriter2.toString());
    }


    @Test
    void specialHandlingForConsole() {
        LogWriter lw = new LogWriter(null,null, 5, null);
        assertTrue(createappender(lw) instanceof ConsoleAppender);

        lw = new LogWriter(LogWriter.FILE_NAME_CONSOLE,LogWriter.FILE_NAME_CONSOLE, 5, null);
        assertTrue(createappender(lw) instanceof ConsoleAppender);
    }

    @Test
    void testSizeBasedLegacyPattern() {
        LogWriter lw = new LogWriter("foo","target/foo", 5, "4k");
        Appender<ILoggingEvent> a = createappender(lw);

        assertInstanceOf(a, SlingRollingFileAppender.class);
        SlingRollingFileAppender<ILoggingEvent> sr = (SlingRollingFileAppender<ILoggingEvent>) a;

        assertInstanceOf(sr.getTriggeringPolicy(), SizeBasedTriggeringPolicy.class);
        assertInstanceOf(sr.getRollingPolicy(), FixedWindowRollingPolicy.class);

        SizeBasedTriggeringPolicy<ILoggingEvent> sbtp = (SizeBasedTriggeringPolicy<ILoggingEvent>) sr.getTriggeringPolicy();
        FixedWindowRollingPolicy fwRp = (FixedWindowRollingPolicy) sr.getRollingPolicy();
        assertEquals(5, fwRp.getMaxIndex());
        assertEquals(4 * FileUtils.ONE_KB, sbtp.getMaxFileSize().getSize());
    }

    @Test
    void testRotationBasedLegacyPattern() {
        LogWriter lw = new LogWriter("foo","target/foo", 5, "'.'yyyy-MM");
        Appender<ILoggingEvent> a = createappender(lw);

        assertInstanceOf(a, SlingRollingFileAppender.class);
        SlingRollingFileAppender<ILoggingEvent> sr = (SlingRollingFileAppender<ILoggingEvent>) a;

        assertInstanceOf(sr.getTriggeringPolicy(), TimeBasedRollingPolicy.class);

        TimeBasedRollingPolicy<ILoggingEvent> tbrp = (TimeBasedRollingPolicy<ILoggingEvent>) sr.getTriggeringPolicy();
        assertEquals(5, tbrp.getMaxHistory());
        assertEquals("target/foo.%d{yyyy-MM}", tbrp.getFileNamePattern());
    }

    @Test
    void allowMoreThanTwentyOneLogFiles() {
        LogWriter lw = new LogWriter("moreThanTwenty", "target/moreThanTwenty", 300, "4k");

        Appender<ILoggingEvent> a = createappender(lw);
        assertInstanceOf(a, SlingRollingFileAppender.class);
        SlingRollingFileAppender<ILoggingEvent> sr = (SlingRollingFileAppender<ILoggingEvent>) a;

        assertInstanceOf(sr.getRollingPolicy(), FixedWindowRollingPolicy.class);
        FixedWindowRollingPolicy rollingPolicy = (FixedWindowRollingPolicy) sr.getRollingPolicy();

        assertEquals(300, rollingPolicy.getMaxIndex());
    }

    private static Appender<ILoggingEvent> createappender(LogWriter lw) {
        Encoder<ILoggingEvent> encoder = new PatternLayoutEncoder();
        return lw.createAppender((Context) LoggerFactory.getILoggerFactory(), encoder);
    }

    private static void assertInstanceOf(Object o, Class<?> expected) {
        if (expected.isInstance(o)) {
            return;
        }
        fail(String.format("Object of type [%s] is not instanceof [%s]", o.getClass(), expected));
    }
}
