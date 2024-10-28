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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingPatternLayoutEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayoutOsgi;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;

/**
 *
 */
class MaskingMessageUtilTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil#setMessageConverter(ch.qos.logback.classic.PatternLayoutOsgi)}.
     */
    @Test
    void testSetMessageConverter() {
        PatternLayoutOsgi pl = new PatternLayoutOsgi();
        MaskingMessageUtil.setMessageConverter(pl);
        assertEquals(11, pl.getInstanceConverterSupplierMap().size());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil#getDefaultEncoder(ch.qos.logback.core.Context)}.
     */
    @Test
    void testGetDefaultEncoder() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Encoder<ILoggingEvent> encoder = MaskingMessageUtil.getDefaultEncoder(loggerContext);
        assertTrue(encoder instanceof MaskingPatternLayoutEncoder);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil#mask(java.lang.String)}.
     */
    @Test
    void testMask() {
        assertNull(MaskingMessageUtil.mask(null));
        assertEquals("hello__world", MaskingMessageUtil.mask("hello\r\nworld"));
    }

}
