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

import java.util.List;
import java.util.regex.Pattern;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Queriable store of structured log entries.
 */
@ProviderType
public interface LogStore {

    /**
     * Returns a list of <tt>LogEntrie</tt>s matching the specified parameters
     *
     * @param pattern the pattern to match against all the text-based fields of the log entry. Ignored if <tt>null</tt>.
     * @param minLevel the minimum level of the log entries. Defaults to {@link LogLevel#TRACE} if <tt>null</tt>.
     * @param maxEntries the maximum entries to return. Clamped to 1 if needed.
     *
     * @return a list of entries matching the parameters. May be empty but not <tt>null</tt>
     */
    List<LogEntry> getRecent(Pattern pattern, LogLevel minLevel, int maxEntries);
}
