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
package org.apache.sling.commons.log.logback.internal.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception for any errors encountered while utilizing any
 * properties for configuration services
 */
public class ConfigurationException extends Exception {
    private static final long serialVersionUID = -9213226340780391070L;

    /**
     * the property that was invalid
     */
    private final String property;
    /**
     * the reason that the property value was invalid
     */
    private final String reason;

    /**
     * Constructor
     *
     * @param property the property that was invalid
     * @param reason the reason that the property value was invalid
     */
    public ConfigurationException(@NotNull final String property, @NotNull final String reason) {
        this(property, reason, null);
    }

    /**
     * Constructor
     *
     * @param property the property that was invalid
     * @param reason the reason that the property value was invalid
     * @param cause the exception that caused the failure
     */
    public ConfigurationException(
            @NotNull final String property, @NotNull final String reason, @Nullable final Throwable cause) {
        super(String.format("Property %s was invalid. Reason: %s", property, reason), cause);
        this.property = property;
        this.reason = reason;
    }

    /**
     * The property that was invalid
     *
     * @return the property name that caused the failure
     */
    public @NotNull String getProperty() {
        return property;
    }

    /**
     * The reason that the property value was invalid
     *
     * @return the reason that the property was invalid
     */
    public @NotNull String getReason() {
        return reason;
    }
}
