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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;

/**
 * Handler for the OsgiModel model class
 */
public class OsgiModelHandler extends ModelHandlerBase {
    boolean inError = false;

    /**
     * Constructor
     *
     * @param context the logging context
     */
    public OsgiModelHandler(Context context) {
        super(context);
    }

    /**
     * Factory to create an instance of this class
     *
     * @param context the logging context
     * @param mic the model interpretation context
     * @return a new instance of OsgiModelHandler
     */
    public static OsgiModelHandler makeInstance(Context context, ModelInterpretationContext mic) { // NOSONAR
        return new OsgiModelHandler(context);
    }

    @Override
    protected Class<OsgiModel> getSupportedModelClass() {
        return OsgiModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        // nothing to do here
    }
}
