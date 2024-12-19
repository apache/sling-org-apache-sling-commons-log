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

package org.apache.sling.commons.log.logback.internal.util;

import org.apache.sling.commons.log.logback.internal.LogWriter;
import org.jetbrains.annotations.Nullable;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;

/**
 * Custom class to allow the SlingLogPanel to differentiate between default
 * appenders and Sling config based appenders
 * 
 * @param <E> the type of event the appender is for (usually {@link ILoggingEvent}
 */
@SuppressWarnings("java:S110")
public class SlingRollingFileAppender<E> extends RollingFileAppender<E> {
    private LogWriter logWriter;

    /**
     * Get the LogWriter config associated with this appender
     * 
     * @return the LogWriter config object (or null if not set)
     */
    public @Nullable LogWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Set the LogWriter config associated with this appender
     * 
     * @param logWriter the config
     */
    public void setLogWriter(@Nullable LogWriter logWriter) {
        this.logWriter = logWriter;
    }

}
