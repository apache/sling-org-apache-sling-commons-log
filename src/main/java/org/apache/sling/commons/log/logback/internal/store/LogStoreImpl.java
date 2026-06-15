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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import org.apache.sling.commons.log.logback.store.LogEntry;
import org.apache.sling.commons.log.logback.store.LogEntryListener;
import org.apache.sling.commons.log.logback.store.LogLevel;
import org.apache.sling.commons.log.logback.store.LogStore;

public class LogStoreImpl implements LogStore {

    static final int DEFAULT_MAX_ENTRIES = 10000;

    private final Object lock = new Object();
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private final Set<LogEntryListener> listeners = new CopyOnWriteArraySet<>();
    private int maxEntriesKept;

    public LogStoreImpl(int maxEntriesKept) {
        this.maxEntriesKept = Math.max(1, maxEntriesKept);
    }

    public void append(LogEntry snapshot) {
        synchronized (lock) {
            entries.addLast(snapshot);
            trimToSize();
        }
        // Notify listeners outside the lock so a slow or re-entrant listener does
        // not stall other producers waiting to append.
        for (LogEntryListener listener : listeners) {
            listener.onEntry(snapshot);
        }
    }

    public void addListener(LogEntryListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(LogEntryListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public List<LogEntry> getRecent(Pattern pattern, LogLevel minLevel, int maxEntries) {
        LogLevel effectiveMinLevel = minLevel == null ? LogLevel.TRACE : minLevel;

        synchronized (lock) {
            List<LogEntry> matches = new ArrayList<>();
            int remaining = Math.max(1, maxEntries);

            for (Iterator<LogEntry> iterator = entries.descendingIterator(); iterator.hasNext() && remaining > 0; ) {
                LogEntry snapshot = iterator.next();
                if (!matches(snapshot, pattern, effectiveMinLevel)) {
                    continue;
                }
                matches.add(snapshot);
                remaining--;
            }

            return matches;
        }
    }

    public void setMaxEntries(int maxEntriesKept) {
        synchronized (lock) {
            this.maxEntriesKept = Math.max(1, maxEntriesKept);
            trimToSize();
        }
    }

    private boolean matches(LogEntry snapshot, Pattern pattern, LogLevel minLevel) {
        if (snapshot.level().ordinal() >= minLevel.ordinal()) {
            if (pattern == null) {
                return true;
            }
            return matchesField(pattern, snapshot.level().name())
                    || matchesField(pattern, snapshot.loggerName())
                    || matchesField(pattern, snapshot.threadName())
                    || matchesField(pattern, snapshot.formattedMessage())
                    || matchesField(pattern, snapshot.throwableText())
                    || matchesMdc(pattern, snapshot);
        }
        return false;
    }

    private boolean matchesMdc(Pattern pattern, LogEntry snapshot) {
        if (snapshot.mdc().isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : snapshot.mdc().entrySet()) {
            if (matchesField(pattern, entry.getKey()) || matchesField(pattern, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesField(Pattern pattern, String value) {
        return value != null && !value.isEmpty() && pattern.matcher(value).find();
    }

    private void trimToSize() {
        while (entries.size() > maxEntriesKept) {
            entries.removeFirst();
        }
    }
}
