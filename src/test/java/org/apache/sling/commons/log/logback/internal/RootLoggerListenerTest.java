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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.status.Status;

/**
 *
 */
class RootLoggerListenerTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.RootLoggerListener#onResetComplete(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnResetCompleteWhenNoConsoleAppenderIsAttached() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        RootLoggerListener listener = new RootLoggerListener();

        loggerContext.getStatusManager().clear();
        listener.onResetComplete(loggerContext);
        List<Status> copyOfStatusList = loggerContext
                .getStatusManager()
                .getCopyOfStatusList();
        // verify the error status was reported
        assertTrue(copyOfStatusList.stream()
                .anyMatch(s -> "No default console appender was attached to the root logger".equals(s.getMessage())),
            "Expected error status msg");
    }

    @Test
    void testOnResetCompleteWhenConsoleAppenderIsAttached() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();

        // simulate the appender already attached
        OutputStreamAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setName(LogConstants.DEFAULT_CONSOLE_APPENDER_NAME);
        appender.setContext(loggerContext);
        appender.setEncoder(MaskingMessageUtil.getDefaultEncoder(loggerContext));
        appender.start();
        final Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(appender);

        RootLoggerListener listener = new RootLoggerListener();

        loggerContext.getStatusManager().clear();
        listener.onResetComplete(loggerContext);
        List<Status> copyOfStatusList = loggerContext
                .getStatusManager()
                .getCopyOfStatusList();
        // verify the error status was reported
        assertTrue(copyOfStatusList.stream()
                .anyMatch(s -> "Detaching the default console appender that is attached to the root logger".equals(s.getMessage())),
            "Expected error status msg");
    }

}
