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
package org.apache.sling.commons.log.logback.store;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot of a log entry.
 *
 * <p>Stores only the lightweight, stable parts of a log event so the log store
 * does not retain full logging event object graphs.</p>
 */
public final class LogEntry {

    private final long timeMillis;
    private final LogLevel level;
    private final String loggerName;
    private final String threadName;
    private final String formattedMessage;
    private final String throwableText;
    private final Map<String, String> mdc;

    public LogEntry(
            long timeMillis,
            LogLevel level,
            String loggerName,
            String threadName,
            String formattedMessage,
            String throwableText,
            Map<String, String> mdc) {
        this.timeMillis = timeMillis;
        this.level = level;
        this.loggerName = loggerName;
        this.threadName = threadName;
        this.formattedMessage = formattedMessage;
        this.throwableText = throwableText;
        this.mdc = mdc.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(mdc));
    }

    public long timeMillis() {
        return timeMillis;
    }

    public LogLevel level() {
        return level;
    }

    public String loggerName() {
        return loggerName;
    }

    public String threadName() {
        return threadName;
    }

    public String formattedMessage() {
        return formattedMessage;
    }

    public String throwableText() {
        return throwableText;
    }

    public Map<String, String> mdc() {
        return mdc;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LogEntry)) {
            return false;
        }
        LogEntry that = (LogEntry) other;
        return timeMillis == that.timeMillis
                && Objects.equals(level, that.level)
                && Objects.equals(loggerName, that.loggerName)
                && Objects.equals(threadName, that.threadName)
                && Objects.equals(formattedMessage, that.formattedMessage)
                && Objects.equals(throwableText, that.throwableText)
                && Objects.equals(mdc, that.mdc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeMillis, level, loggerName, threadName, formattedMessage, throwableText, mdc);
    }

    @Override
    public String toString() {
        return "LogEntry{" + "timeMillis=" + timeMillis + ", level='" + level + '\'' + ", loggerName='"
                + loggerName + '\'' + ", threadName='" + threadName + '\'' + ", formattedMessage='"
                + formattedMessage + '\'' + ", throwableText='" + throwableText + '\'' + ", mdc=" + mdc + '}';
    }
}
