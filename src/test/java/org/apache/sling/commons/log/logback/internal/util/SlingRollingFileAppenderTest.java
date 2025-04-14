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
package org.apache.sling.commons.log.logback.internal.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.sling.commons.log.logback.internal.LogWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 */
class SlingRollingFileAppenderTest {

    private SlingRollingFileAppender<ILoggingEvent> appender;
    private LogWriter mockLogWriter;

    @BeforeEach
    protected void beforeEach() {
        appender = new SlingRollingFileAppender<>();
        mockLogWriter = Mockito.mock(LogWriter.class);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender#getLogWriter()}.
     */
    @Test
    void testGetLogWriter() {
        assertNull(appender.getLogWriter());
        appender.setLogWriter(mockLogWriter);
        assertEquals(mockLogWriter, appender.getLogWriter());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender#setLogWriter(org.apache.sling.commons.log.logback.internal.LogWriter)}.
     */
    @Test
    void testSetLogWriter() {
        appender.setLogWriter(mockLogWriter);
        assertEquals(mockLogWriter, appender.getLogWriter());
    }
}
