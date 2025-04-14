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
package org.apache.sling.commons.log.logback.internal.joran;

import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.AppenderRefModelHandler;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.apache.sling.commons.log.logback.internal.AppenderOrigin;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps the original impl to add tracking of which appenders are
 * added to which loggers
 */
class AppenderRefModelHandlerWrapper extends AppenderRefModelHandler {

    private LogConfigManager logConfigManager;

    AppenderRefModelHandlerWrapper(@NotNull Context context, @NotNull LogConfigManager manager) {
        super(context);
        this.logConfigManager = manager;
    }

    /**
     * wrap the super impl to track which appenders are referenced by which logger
     *
     * @param mic the interpretation context
     * @param model the model to process
     */
    @Override
    public void handle(@NotNull ModelInterpretationContext mic, @NotNull Model model) throws ModelHandlerException {
        // delegate to the original implementation
        super.handle(mic, model);

        // dig out which appenders were added which loggers
        AppenderRefModel appenderRefModel = (AppenderRefModel) model;
        String appenderName = mic.subst(appenderRefModel.getRef());
        @SuppressWarnings("unchecked")
        Map<String, Appender<ILoggingEvent>> appenderBag =
                (Map<String, Appender<ILoggingEvent>>) mic.getObjectMap().get(JoranConstants.APPENDER_BAG);
        Appender<ILoggingEvent> appender = appenderBag.get(appenderName);
        if (appender != null) {
            Object o = mic.peekObject();
            if (o instanceof AppenderAttachable) {
                @SuppressWarnings("unchecked")
                AppenderAttachable<ILoggingEvent> appenderAttachable = (AppenderAttachable<ILoggingEvent>) o;
                if (appenderAttachable instanceof Logger) {
                    appenderAttachable.addAppender(appender);

                    String loggerName = ((Logger) appenderAttachable).getName();
                    // inform the LogConfigManager about the appender reference
                    logConfigManager.addedAppenderRef(AppenderOrigin.JORAN, appenderName, loggerName);
                }
            }
        }
    }
}
