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
package org.apache.sling.commons.log.logback.internal.stacktrace;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingExtendedThrowableProxyConverter;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingMessageConverter;
import org.apache.sling.commons.log.logback.internal.stacktrace.OSGiAwareExceptionHandling.OSGiAwareConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 */
class OSGiAwareExceptionHandlingTest {

    private OSGiAwareExceptionHandling exceptionHandling;
    private PackageInfoCollector collector;

    @BeforeEach
    protected void beforeEach() {
        collector = new PackageInfoCollector();
        exceptionHandling = new OSGiAwareExceptionHandling(collector);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.OSGiAwareExceptionHandling#process(ch.qos.logback.core.Context, ch.qos.logback.core.pattern.Converter)}.
     */
    @Test
    void testProcess() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        Converter<ILoggingEvent> converter = new MaskingMessageConverter();
        exceptionHandling.process(loggerContext, converter);

        Converter<ILoggingEvent> tail = ConverterUtil.findTail(converter);
        assertEquals(OSGiAwareConverter.class.getName(), tail.getClass().getName());
    }

    @Test
    void testProcessForNullConverter() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertThrows(IllegalArgumentException.class, () -> exceptionHandling.process(loggerContext, null));
    }

    @Test
    void testProcessWithConverterThatHandlesThrowable() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        Converter<ILoggingEvent> converter = new MaskingExtendedThrowableProxyConverter();
        exceptionHandling.process(loggerContext, converter);
        Converter<ILoggingEvent> tail = ConverterUtil.findTail(converter);
        assertEquals(converter, tail);
    }
}
