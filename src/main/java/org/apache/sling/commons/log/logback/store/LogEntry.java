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

/**
 * Snapshot of a log entry.
 *
 * <p>Stores only the lightweight, stable parts of a log event so the log store
 * does not retain full logging event object graphs.</p>
 */
public record LogEntry(
        long timeMillis,
        LogLevel level,
        String loggerName,
        String threadName,
        String formattedMessage,
        String throwableText,
        Map<String, String> mdc) {

    public LogEntry {
        mdc = mdc.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(mdc));
    }
}
