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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import org.apache.sling.commons.log.logback.store.LogEntry;
import org.apache.sling.commons.log.logback.store.LogLevel;

public class LogStoreAppender extends AppenderBase<ILoggingEvent> {

    public static final String APPENDER_NAME = "structured-log-store";

    private final LogStoreImpl store;

    public LogStoreAppender(LogStoreImpl store) {
        this.store = store;
        setName(APPENDER_NAME);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null) {
            return;
        }

        LogLevel logLevel = getLogLevel(eventObject);
        if (logLevel == null) {
            return;
        }

        store.append(new LogEntry(
                eventObject.getTimeStamp(),
                logLevel,
                eventObject.getLoggerName(),
                eventObject.getThreadName(),
                eventObject.getFormattedMessage(),
                getThrowableText(eventObject),
                eventObject.getMDCPropertyMap()));
    }

    private LogLevel getLogLevel(ILoggingEvent eventObject) {
        switch (eventObject.getLevel().levelInt) {
            case ch.qos.logback.classic.Level.TRACE_INT:
                return LogLevel.TRACE;
            case ch.qos.logback.classic.Level.DEBUG_INT:
                return LogLevel.DEBUG;
            case ch.qos.logback.classic.Level.INFO_INT:
                return LogLevel.INFO;
            case ch.qos.logback.classic.Level.WARN_INT:
                return LogLevel.WARN;
            case ch.qos.logback.classic.Level.ERROR_INT:
                return LogLevel.ERROR;
            default:
                return null;
        }
    }

    private String getThrowableText(ILoggingEvent eventObject) {
        IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
        if (throwableProxy == null) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        appendThrowable(text, throwableProxy, null);
        return text.toString();
    }

    private void appendThrowable(StringBuilder text, IThrowableProxy throwableProxy, String prefix) {
        if (prefix != null) {
            text.append(prefix);
        }
        text.append(getThrowableHeader(throwableProxy)).append('\n');

        StackTraceElementProxy[] stackTrace = throwableProxy.getStackTraceElementProxyArray();
        if (stackTrace != null) {
            int framesToRender = Math.max(0, stackTrace.length - Math.max(0, throwableProxy.getCommonFrames()));
            for (int i = 0; i < framesToRender; i++) {
                text.append('\t').append(stackTrace[i]).append('\n');
            }
            if (throwableProxy.getCommonFrames() > 0) {
                text.append("\t... ")
                        .append(throwableProxy.getCommonFrames())
                        .append(" common frames omitted")
                        .append('\n');
            }
        }

        IThrowableProxy[] suppressed = throwableProxy.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy suppressedThrowable : suppressed) {
                appendThrowable(text, suppressedThrowable, "Suppressed: ");
            }
        }

        IThrowableProxy cause = throwableProxy.getCause();
        if (cause != null) {
            appendThrowable(text, cause, "Caused by: ");
        }
    }

    private String getThrowableHeader(IThrowableProxy throwableProxy) {
        String overridingMessage = throwableProxy.getOverridingMessage();
        if (overridingMessage != null && !overridingMessage.isEmpty()) {
            return overridingMessage;
        }

        StringBuilder header = new StringBuilder(throwableProxy.getClassName());
        String message = throwableProxy.getMessage();
        if (message != null && !message.isEmpty()) {
            header.append(": ").append(message);
        }
        return header.toString();
    }
}
