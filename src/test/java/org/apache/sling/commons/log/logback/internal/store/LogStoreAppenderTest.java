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
package org.apache.sling.commons.log.logback.internal.store;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.apache.sling.commons.log.logback.store.LogEntry;
import org.apache.sling.commons.log.logback.store.LogLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogStoreAppenderTest {

    private static final StackTraceElement SHARED_FRAME_ONE =
            new StackTraceElement("example.Shared", "sharedOne", "Shared.java", 10);
    private static final StackTraceElement SHARED_FRAME_TWO =
            new StackTraceElement("example.Shared", "sharedTwo", "Shared.java", 20);

    @Test
    void appenderSnapshotsFormattedMessageAndThrowable() {
        LogStoreImpl store = new LogStoreImpl(5);
        LogStoreAppender appender = new LogStoreAppender(store);

        LoggerContext context = new LoggerContext();
        appender.setContext(context);
        Logger logger = context.getLogger("test.logger");
        RuntimeException failure = new RuntimeException("error");
        LoggingEvent event = new LoggingEvent(getClass().getName(), logger, Level.ERROR, "message", failure, null);
        event.setMDCPropertyMap(Map.of("requestId", "123"));
        event.setThreadName("worker-1");

        appender.append(event);

        List<LogEntry> logs = store.getRecent(null, LogLevel.TRACE, 10);
        assertEquals(1, logs.size());
        assertEquals("message", logs.get(0).formattedMessage());
        assertEquals("worker-1", logs.get(0).threadName());
        assertEquals(LogLevel.ERROR, logs.get(0).level());
        assertEquals(Map.of("requestId", "123"), logs.get(0).mdc());
        assertNotNull(logs.get(0).throwableText());
    }

    @Test
    void appenderFormatsSuppressedCauseAndCommonFrames() {
        LogStoreImpl store = new LogStoreImpl(5);
        LogStoreAppender appender = new LogStoreAppender(store);
        String expectedThrowableText = """
                java.lang.RuntimeException: top level
                	at example.Top.run(Top.java:50)
                	at example.Shared.sharedOne(Shared.java:10)
                	at example.Shared.sharedTwo(Shared.java:20)
                Suppressed: java.lang.IllegalStateException: suppressed
                	at example.Suppressed.hide(Suppressed.java:40)
                	... 2 common frames omitted
                Caused by: java.lang.IllegalArgumentException: root cause
                	at example.Cause.explode(Cause.java:30)
                	... 2 common frames omitted
                """;

        LoggerContext context = new LoggerContext();
        appender.setContext(context);
        Logger logger = context.getLogger("test.logger");

        IllegalArgumentException cause = exceptionWithSharedFrames(
                new IllegalArgumentException("root cause"), "example.Cause", "explode", "Cause.java", 30);

        IllegalStateException suppressed = exceptionWithSharedFrames(
                new IllegalStateException("suppressed"), "example.Suppressed", "hide", "Suppressed.java", 40);

        RuntimeException failure = new RuntimeException("top level", cause);
        exceptionWithSharedFrames(failure, "example.Top", "run", "Top.java", 50);
        failure.addSuppressed(suppressed);

        LoggingEvent event = new LoggingEvent(getClass().getName(), logger, Level.ERROR, "message", failure, null);
        event.setMDCPropertyMap(Map.of());
        event.setThreadName("worker-1");

        appender.append(event);

        List<LogEntry> logs = store.getRecent(null, LogLevel.TRACE, 10);
        assertEquals(1, logs.size());
        assertEquals(expectedThrowableText, logs.get(0).throwableText());
    }

    private <T extends Throwable> T exceptionWithSharedFrames(
            T throwable, String className, String methodName, String fileName, int lineNumber) {
        throwable.setStackTrace(new StackTraceElement[] {
            new StackTraceElement(className, methodName, fileName, lineNumber), SHARED_FRAME_ONE, SHARED_FRAME_TWO
        });
        return throwable;
    }
}
