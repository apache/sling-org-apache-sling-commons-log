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
package org.apache.sling.commons.log.logback.internal.webconsole;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.sling.commons.log.logback.webconsole.LoggerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
class LoggerConfigTest {

    private LoggerConfig loggerConfig;

    @BeforeEach
    protected void beforeEach() {
        String pid = "pid1";
        String logLevel = "WARN";
        String[] loggers = new String[] {"log.loggerConfig1"};
        String logFile = "logs/loggerConfig.log";
        boolean additive = false;
        loggerConfig = new LoggerConfig(pid, logLevel, loggers, logFile, additive);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.LoggerConfig#getPid()}.
     */
    @Test
    void testGetPid() {
        assertEquals("pid1", loggerConfig.getPid());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.LoggerConfig#getLogLevel()}.
     */
    @Test
    void testGetLogLevel() {
        assertEquals("WARN", loggerConfig.getLogLevel());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.LoggerConfig#getLoggers()}.
     */
    @Test
    void testGetLoggers() {
        assertArrayEquals(new String[] {"log.loggerConfig1"},
                loggerConfig.getLoggers());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.LoggerConfig#getLogFile()}.
     */
    @Test
    void testGetLogFile() {
        assertEquals("logs/loggerConfig.log", loggerConfig.getLogFile());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.LoggerConfig#isAdditive()}.
     */
    @Test
    void testIsAdditive() {
        assertFalse(loggerConfig.isAdditive());
    }

}
