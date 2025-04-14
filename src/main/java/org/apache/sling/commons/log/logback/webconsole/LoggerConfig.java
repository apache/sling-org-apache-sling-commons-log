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
package org.apache.sling.commons.log.logback.webconsole;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the info about a logger configuration
 */
public final class LoggerConfig {
    private final String pid;
    private final String logLevel;
    private final String[] loggers;
    private final String logFile;
    private boolean additive;

    /**
     * Constructor
     * @param pid the pid of the logger configuration (may be null)
     * @param logLevel the log level for the loggers (may be null)
     * @param loggers the set of loggers
     * @param logFile the target file for the logging output
     * @param additive true if root should log too, false otherwise
     */
    public LoggerConfig(
            @Nullable String pid,
            @Nullable String logLevel,
            @Nullable String[] loggers,
            @Nullable String logFile,
            boolean additive) {
        this.pid = pid;
        this.logLevel = logLevel;
        this.loggers = loggers == null ? null : Arrays.copyOf(loggers, loggers.length);
        this.logFile = logFile;
        this.additive = additive;
    }

    /**
     * Gets the PID
     *
     * @return the pid of the logger configuration (may be null)
     */
    public @Nullable String getPid() {
        return pid;
    }

    /**
     * Gets the log level
     *
     * @return the log level for the loggers (may be null)
     */
    public @Nullable String getLogLevel() {
        return logLevel;
    }

    /**
     * Gets the loggers
     *
     * @return the set of loggers
     */
    public @Nullable String[] getLoggers() {
        return loggers;
    }

    /**
     * Gets the log file
     *
     * @return the target file for the logging output
     */
    public @Nullable String getLogFile() {
        return logFile;
    }

    /**
     * Gets additive value
     *
     * @return true if root should log too, false otherwise
     */
    public boolean isAdditive() {
        return additive;
    }
}
