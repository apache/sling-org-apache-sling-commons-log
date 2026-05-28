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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Whiteboard-style callback for entries appended to the {@link LogStore}.
 *
 * <p>Listeners are notified synchronously on the logging thread after the
 * entry has been stored, so implementations must not block or perform expensive
 * work. They must also be careful not to log at levels the store captures, to
 * avoid re-entrant notification.</p>
 *
 * <p>Listeners registered before the {@code LogStore} is configured begin
 * receiving callbacks as soon as the store activates. Listeners that
 * unregister stop receiving callbacks before the next entry is appended.</p>
 */
@ConsumerType
public interface LogEntryListener {

    /**
     * Notification that a new {@link LogEntry} was appended to the store.
     *
     * @param entry the entry that was just appended; never {@code null}
     */
    void onEntry(LogEntry entry);
}
