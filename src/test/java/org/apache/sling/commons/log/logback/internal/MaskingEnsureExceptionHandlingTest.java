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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingEnsureExceptionHandling;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingExtendedThrowableProxyConverter;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingMessageConverter;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxyConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class MaskingEnsureExceptionHandlingTest {

    private MaskingEnsureExceptionHandling eh;
    private LoggerContext loggerContext;

    @BeforeEach
    protected void beforeEach() {
        eh = new MaskingEnsureExceptionHandling();
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingEnsureExceptionHandling#process(ch.qos.logback.core.Context, ch.qos.logback.core.pattern.Converter)}.
     */
    @Test
    void testProcessWithNullConverter() {
        assertThrows(IllegalArgumentException.class, () -> eh.process(loggerContext, null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProcessWithConverterThatDoesNotHandleThrowable(boolean packagingDataEnabled) {
        boolean original = loggerContext.isPackagingDataEnabled();
        try {
            loggerContext.setPackagingDataEnabled(packagingDataEnabled);

            Converter<ILoggingEvent> converter = new MaskingMessageConverter();
            eh.process(loggerContext, converter);

            Converter<ILoggingEvent> tail = ConverterUtil.findTail(converter);
            if (packagingDataEnabled) {
                assertTrue(tail instanceof MaskingExtendedThrowableProxyConverter);
            } else {
                assertTrue(tail instanceof MaskingThrowableProxyConverter);
            }
        } finally {
            loggerContext.setPackagingDataEnabled(original);
        }
    }

    @Test
    void testProcessWithConverterThatHandlesThrowable() {
        Converter<ILoggingEvent> converter = new MaskingExtendedThrowableProxyConverter();
        eh.process(loggerContext, converter);
        Converter<ILoggingEvent> tail = ConverterUtil.findTail(converter);
        assertEquals(converter, tail);
    }
}
