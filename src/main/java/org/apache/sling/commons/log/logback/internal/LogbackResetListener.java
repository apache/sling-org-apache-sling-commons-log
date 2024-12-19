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
package org.apache.sling.commons.log.logback.internal;

import org.jetbrains.annotations.NotNull;

import ch.qos.logback.classic.LoggerContext;

/**
 * Implement for doing work when the logback configuration is reset
 */
public interface LogbackResetListener {

    /**
     * Callback before the reset is started
     *
     * @param context the logger context being reset
     */
    default void onResetStart(@NotNull LoggerContext context) {
        // do nothing by default
    }

    /**
     * Callback after the reset is completed
     * 
     * @param context the logger context being reset
     */
    default void onResetComplete(@NotNull LoggerContext context) {
        // do nothing by default
    }

}
