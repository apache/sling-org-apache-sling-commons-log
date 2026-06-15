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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.sling.commons.log.logback.store.LogEntry;
import org.apache.sling.commons.log.logback.store.LogEntryListener;
import org.apache.sling.commons.log.logback.store.LogLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogStoreImplTest {

    @Test
    void keepsOnlyNewestEntriesWithinCapacity() {
        LogStoreImpl store = new LogStoreImpl(2);

        store.append(logEntry(1L, LogLevel.INFO, "first"));
        store.append(logEntry(2L, LogLevel.INFO, "second"));
        store.append(logEntry(3L, LogLevel.INFO, "third"));

        List<LogEntry> logs = store.getRecent(null, LogLevel.TRACE, 10);
        assertEquals(
                List.of("third", "second"),
                logs.stream().map(LogEntry::formattedMessage).collect(Collectors.toList()));
    }

    @Test
    void filtersByLevelAndRegex() {
        LogStoreImpl store = new LogStoreImpl(10);

        store.append(logEntry(1L, LogLevel.DEBUG, "debug trace"));
        store.append(logEntry(2L, LogLevel.INFO, "first user ok"));
        store.append(logEntry(3L, LogLevel.ERROR, "first user failure"));

        List<LogEntry> logs = store.getRecent(Pattern.compile("first", Pattern.CASE_INSENSITIVE), LogLevel.INFO, 10);

        assertEquals(
                List.of("first user failure", "first user ok"),
                logs.stream().map(LogEntry::formattedMessage).collect(Collectors.toList()));
    }

    @Test
    void defaultsToTraceWhenMinLevelIsNull() {
        LogStoreImpl store = new LogStoreImpl(10);
        store.append(logEntry(1L, LogLevel.DEBUG, "debug trace"));

        List<LogEntry> logs = store.getRecent(null, null, 10);

        assertEquals(
                List.of("debug trace"),
                logs.stream().map(LogEntry::formattedMessage).collect(Collectors.toList()));
    }

    @Test
    void shrinksStoreWhenMaxEntriesIsLowered() {
        LogStoreImpl store = new LogStoreImpl(3);

        store.append(logEntry(1L, LogLevel.INFO, "first"));
        store.append(logEntry(2L, LogLevel.INFO, "second"));
        store.append(logEntry(3L, LogLevel.INFO, "third"));
        store.setMaxEntries(2);

        List<LogEntry> logs = store.getRecent(null, LogLevel.TRACE, 10);
        assertEquals(
                List.of("third", "second"),
                logs.stream().map(LogEntry::formattedMessage).collect(Collectors.toList()));
    }

    @Test
    void notifiesListenersInRegistrationOrder() {
        LogStoreImpl store = new LogStoreImpl(10);
        List<LogEntry> received = new ArrayList<>();
        LogEntryListener listener = received::add;

        store.addListener(listener);
        LogEntry entry1 = logEntry(1L, LogLevel.INFO, "first");
        LogEntry entry2 = logEntry(2L, LogLevel.INFO, "second");
        store.append(entry1);
        store.append(entry2);

        assertEquals(2, received.size());
        assertSame(entry1, received.get(0));
        assertSame(entry2, received.get(1));
    }

    @Test
    void stopsNotifyingAfterRemoveListener() {
        LogStoreImpl store = new LogStoreImpl(10);
        List<LogEntry> received = new ArrayList<>();
        LogEntryListener listener = received::add;

        store.addListener(listener);
        store.append(logEntry(1L, LogLevel.INFO, "before"));
        store.removeListener(listener);
        store.append(logEntry(2L, LogLevel.INFO, "after"));

        assertEquals(1, received.size());
        assertEquals("before", received.get(0).formattedMessage());
    }

    @Test
    void rejectsNullListener() {
        LogStoreImpl store = new LogStoreImpl(10);
        assertThrows(NullPointerException.class, () -> store.addListener(null));
        assertThrows(NullPointerException.class, () -> store.removeListener(null));
    }

    @Test
    void listenerExceptionDoesNotPreventStorage() {
        LogStoreImpl store = new LogStoreImpl(10);
        store.addListener(entry -> {
            throw new RuntimeException("listener boom");
        });

        try {
            store.append(logEntry(1L, LogLevel.INFO, "stored"));
        } catch (RuntimeException expected) {
            // listener propagated; storage already happened before the throw
        }
        List<LogEntry> stored = store.getRecent(null, LogLevel.TRACE, 10);
        assertEquals(1, stored.size());
        assertTrue("stored".equals(stored.get(0).formattedMessage()));
    }

    private LogEntry logEntry(long timeMillis, LogLevel level, String message) {
        return new LogEntry(timeMillis, level, "logger", "thread", message, null, null, null, Map.of());
    }
}
