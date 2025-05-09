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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.ContextAware;
import org.apache.sling.commons.log.logback.internal.util.SlingContextUtil;
import org.jetbrains.annotations.NotNull;

class RootLoggerListener implements LogbackResetListener {

    /**
     * Callback after the reset is completed
     *
     * @param context the logger context being reset
     */
    @Override
    public void onResetComplete(@NotNull LoggerContext context) {
        // Remove the default console appender that we attached at start of reset
        ch.qos.logback.classic.Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ContextAware c = new SlingContextUtil(context, this);
        if (root.detachAppender(LogConstants.DEFAULT_CONSOLE_APPENDER_NAME)) {
            c.addInfo("Detaching the default console appender that is attached to the root logger");
        } else {
            c.addInfo("No default console appender was attached to the root logger");
        }
    }
}
