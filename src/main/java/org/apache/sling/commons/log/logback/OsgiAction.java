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
package org.apache.sling.commons.log.logback;

import static ch.qos.logback.core.joran.JoranConstants.CONFIGURATION_TAG;
import static ch.qos.logback.core.joran.JoranConstants.INCLUDED_TAG;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.sling.commons.log.logback.internal.ConfigSourceTracker;
import org.apache.sling.commons.log.logback.internal.ConfigSourceTracker.ConfigSourceInfo;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.joran.JoranConfiguratorWrapper;
import org.apache.sling.commons.log.logback.internal.joran.OsgiModel;
import org.apache.sling.commons.log.logback.internal.util.Util;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.ErrorCodes;


/**
 * Joran action enabling integration between OSGi and Logback. It supports including
 * config fragments provided through OSGi ServiceRegistry
 * 
 * The action class needs to be referred in external files hence adding a
 * class in public package.
 * <p>
 * This class is for configuration reference only. Consumers are not intended to
 * instantiate or extend from it.
 */
@ProviderType
public final class OsgiAction extends BaseModelAction {

    /**
     * Build the OsgiModel for the given inputs
     *
     * @param interpretationContext the Sax event interpretation context
     * @param name the element name
     * @param attributes the attributes for the element
     * @return the built model 
     */
    @Override
    protected Model buildCurrentModel(@NotNull SaxEventInterpretationContext interpretationContext, @NotNull String name,
            @NotNull Attributes attributes) {
        OsgiModel osgiModel = new OsgiModel();

        // TO CHECK Should we add the config fragment at end
        final Collection<ConfigSourceInfo> providers = getFragmentProviders();
        for (ConfigSourceInfo cp : providers) {
            InputSource is = cp.getConfigProvider().getConfigSource();

            SaxEventRecorder recorder = null;
            try {
                recorder = populateSaxEventRecorder(is);

                List<SaxEvent> saxEvents = recorder.getSaxEventList();
                if (saxEvents.isEmpty()) {
                    addWarn("Empty sax event list");
                } else {
                    LogConfigManager lcm = (LogConfigManager)getContext().getObject(LogConfigManager.class.getName());
                    JoranConfigurator genericXMLConfigurator = new JoranConfiguratorWrapper(lcm);
                    genericXMLConfigurator.setContext(context);
                    genericXMLConfigurator.getRuleStore().addPathPathMapping(INCLUDED_TAG, CONFIGURATION_TAG);

                    Model modelFromIncludedFile = genericXMLConfigurator.buildModelFromSaxEventList(recorder.getSaxEventList());
                    if (modelFromIncludedFile == null) {
                        addError(ErrorCodes.EMPTY_MODEL_STACK);
                    } else {
                        osgiModel.getSubModels().addAll(modelFromIncludedFile.getSubModels());
                    }
                }

            } catch (JoranException e) {
                addError("Error processing XML data in [" + cp + "]", e);
            } finally {
                Util.close(is);
            }
        }

        return osgiModel;
    }

    /**
     * Get the fragment providers attached to the context
     * 
     * @return the found fragment providers or an empty list if not found
     */
    private @NotNull Collection<ConfigSourceInfo> getFragmentProviders() {
        ConfigSourceTracker tracker = (ConfigSourceTracker) getContext().getObject(ConfigSourceTracker.class.getName());
        if (tracker != null) {
            return tracker.getSources();
        }
        return Collections.emptyList();
    }

    SaxEventRecorder populateSaxEventRecorder(final InputSource inputSource) throws JoranException {
        SaxEventRecorder recorder = new SaxEventRecorder(context);
        recorder.recordEvents(inputSource);
        return recorder;
    }

}
