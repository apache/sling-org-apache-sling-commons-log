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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.apache.sling.commons.log.logback.internal.AppenderOrigin;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.jetbrains.annotations.NotNull;

/**
 * Tracking of which osgi appenders are added to which loggers
 */
class OsgiAppenderRefModelHandler extends ModelHandlerBase {
    private LogConfigManager logConfigManager;

    public OsgiAppenderRefModelHandler(@NotNull Context context, @NotNull LogConfigManager manager) {
        super(context);
        this.logConfigManager = manager;
    }

    @Override
    protected @NotNull Class<? extends OsgiAppenderRefModel> getSupportedModelClass() {
        return OsgiAppenderRefModel.class;
    }

    @Override
    public void handle(@NotNull ModelInterpretationContext interpContext, @NotNull Model model)
            throws ModelHandlerException {
        Object o = interpContext.peekObject();

        if (!(o instanceof AppenderAttachable)) {
            String errMsg =
                    "Could not find an AppenderAttachable at the top of execution stack. Near " + model.idString();
            addError(errMsg);
            return;
        }

        OsgiAppenderRefModel appenderRefModel = (OsgiAppenderRefModel) model;
        AppenderAttachable<?> appenderAttachable = (AppenderAttachable<?>) o;

        attachOsgiReferencedAppenders(interpContext, appenderRefModel, appenderAttachable);
    }

    void attachOsgiReferencedAppenders(
            @NotNull ModelInterpretationContext mic,
            @NotNull OsgiAppenderRefModel appenderRefModel,
            @NotNull AppenderAttachable<?> appenderAttachable) {
        String appenderName = mic.subst(appenderRefModel.getRef());
        if (appenderAttachable instanceof Logger) {
            String loggerName = ((Logger) appenderAttachable).getName();
            // inform the LogConfigManager about the osgi appender reference
            logConfigManager.addedAppenderRef(AppenderOrigin.JORAN_OSGI, appenderName, loggerName);
        } else {
            addError("Failed to add osgi appender named [" + appenderName + "] as the attachable is not a Logger.");
        }
    }
}
