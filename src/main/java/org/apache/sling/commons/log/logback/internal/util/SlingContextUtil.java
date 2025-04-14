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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.jetbrains.annotations.NotNull;

/**
 * Custom util such that origin can be customized
 */
public class SlingContextUtil extends ContextAwareBase {
    /**
     * The declared origin value
     */
    private final Object origin;

    /**
     * Constructor
     *
     * @param context the logging context
     * @param origin the origin object
     */
    public SlingContextUtil(@NotNull Context context, @NotNull Object origin) {
        this.origin = origin;
        setContext(context);
    }

    /**
     * Return the origin where this instance was declared
     *
     * @return the declared origin
     */
    @Override
    protected @NotNull Object getDeclaredOrigin() {
        return origin;
    }
}
