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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.EventPlayer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.Model;
import org.apache.sling.commons.log.logback.internal.ConfigSourceTracker;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.joran.OsgiModel;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class OsgiActionTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private OsgiAction action;
    private LogConfigManager mgr;

    private LoggerContext loggerContext;

    @BeforeEach
    protected void beforeEach() {
        mgr = new LogConfigManager(context.bundleContext());
        action = new OsgiAction();
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        action.setContext(loggerContext);
    }

    @AfterEach
    protected void afterEach() {
        if (mgr != null) {
            mgr.stop();
        }
    }

    protected SaxEventInterpretationContext mockSaxEventInterpretation() {
        // mock the sax event interpretation objects
        SaxEventInterpreter sei = Mockito.mock(SaxEventInterpreter.class);
        SaxEventInterpretationContext intercon = new SaxEventInterpretationContext(loggerContext, sei);
        EventPlayer eventPlayer = new EventPlayer(sei, new ArrayList<>());
        Mockito.doReturn(eventPlayer).when(sei).getEventPlayer();
        return intercon;
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.OsgiAction#buildCurrentModel(ch.qos.logback.core.joran.spi.SaxEventInterpretationContext, java.lang.String, org.xml.sax.Attributes)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testBuildCurrentModelWithFragmentProviders() throws InvalidSyntaxException {
        BundleContext bundleContext = context.bundleContext();
        ConfigProvider configProvider1 = Mockito.spy(new ConfigProvider() {
            @Override
            public @NotNull InputSource getConfigSource() {
                return new InputSource(new StringReader("<included>  <logger name=\"foo.ref.osgi1\" level=\"DEBUG\">\n"
                        + "    <appender-ref-osgi ref=\"TestAppender1\" />\n"
                        + "  </logger>\n"
                        + "</included>"));
            }
        });
        ServiceRegistration<?> serviceReg =
                bundleContext.registerService(ConfigProvider.class, configProvider1, new Hashtable<>(Map.of()));
        ServiceReference<?> serviceRef = serviceReg.getReference();

        String configProvider2 = "<included>  <logger name=\"foo.ref.osgi2\" level=\"DEBUG\">\n"
                + "    <appender-ref-osgi ref=\"TestAppender2\" />\n"
                + "  </logger>\n"
                + "</included>";
        ServiceRegistration<?> serviceReg2 = bundleContext.registerService(
                String.class, configProvider2, new Hashtable<>(Map.of(ConfigSourceTracker.PROP_LOGBACK_CONFIG, true)));
        ServiceReference<?> serviceRef2 = serviceReg2.getReference();

        // for reference at OsgiAction#getLogbackManager
        loggerContext.putObject(LogConfigManager.class.getName(), mgr);
        // for reference at OsgiAction#getFragmentProviders
        ConfigSourceTracker configSourceTracker = new ConfigSourceTracker(context.bundleContext(), mgr);
        configSourceTracker.addingService((ServiceReference<Object>) serviceRef);
        configSourceTracker.addingService((ServiceReference<Object>) serviceRef2);
        loggerContext.putObject(ConfigSourceTracker.class.getName(), configSourceTracker);

        SaxEventInterpretationContext intercon = mockSaxEventInterpretation();

        Model model = action.buildCurrentModel(intercon, "osgi", new AttributesImpl());
        assertTrue(model instanceof OsgiModel);
        assertEquals(2, model.getSubModels().size());
    }

    @Test
    void testBuildCurrentModelWithNoFragmentProviders() {
        SaxEventInterpretationContext intercon = mockSaxEventInterpretation();

        Model model = action.buildCurrentModel(intercon, "osgi", new AttributesImpl());
        assertTrue(model instanceof OsgiModel);
        assertTrue(model.getSubModels().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBuildConfigModelWithWrongRootElementInFragmentProvider() throws InvalidSyntaxException {
        BundleContext bundleContext = context.bundleContext();
        ConfigProvider configProvider1 = Mockito.spy(new ConfigProvider() {
            @Override
            public @NotNull InputSource getConfigSource() {
                return new InputSource(new StringReader("<invalid>  <logger name=\"foo.ref.osgi1\" level=\"DEBUG\">\n"
                        + "    <appender-ref-osgi ref=\"TestAppender1\" />\n"
                        + "  </logger>\n"
                        + "</invalid>"));
            }
        });
        ServiceRegistration<?> serviceReg =
                bundleContext.registerService(ConfigProvider.class, configProvider1, new Hashtable<>(Map.of()));
        ServiceReference<?> serviceRef = serviceReg.getReference();

        // for reference at OsgiAction#getLogbackManager
        loggerContext.putObject(LogConfigManager.class.getName(), mgr);
        // for reference at OsgiAction#getFragmentProviders
        ConfigSourceTracker configSourceTracker = new ConfigSourceTracker(context.bundleContext(), mgr);
        configSourceTracker.addingService((ServiceReference<Object>) serviceRef);
        loggerContext.putObject(ConfigSourceTracker.class.getName(), configSourceTracker);
        action.setContext(loggerContext);

        SaxEventInterpretationContext intercon = mockSaxEventInterpretation();

        Model model = action.buildCurrentModel(intercon, "osgi", new AttributesImpl());
        assertTrue(model instanceof OsgiModel);
        assertTrue(model.getSubModels().isEmpty());

        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s ->
                                "Could not find valid configuration instructions. Exiting.".equals(s.getMessage())),
                "Expected parsing error status msg");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBuildConfigModelWithInvalidXmlFragmentProvider() throws InvalidSyntaxException {
        BundleContext bundleContext = context.bundleContext();
        ConfigProvider configProvider1 = Mockito.spy(new ConfigProvider() {
            @Override
            public @NotNull InputSource getConfigSource() {
                return new InputSource(new StringReader("<invalid>"));
            }
        });
        ServiceRegistration<?> serviceReg =
                bundleContext.registerService(ConfigProvider.class, configProvider1, new Hashtable<>(Map.of()));
        ServiceReference<?> serviceRef = serviceReg.getReference();

        // for reference at OsgiAction#getLogbackManager
        loggerContext.putObject(LogConfigManager.class.getName(), mgr);
        // for reference at OsgiAction#getFragmentProviders
        ConfigSourceTracker configSourceTracker = new ConfigSourceTracker(context.bundleContext(), mgr);
        configSourceTracker.addingService((ServiceReference<Object>) serviceRef);
        loggerContext.putObject(ConfigSourceTracker.class.getName(), configSourceTracker);
        action.setContext(loggerContext);

        SaxEventInterpretationContext intercon = mockSaxEventInterpretation();

        Model model = action.buildCurrentModel(intercon, "osgi", new AttributesImpl());
        assertTrue(model instanceof OsgiModel);
        assertTrue(model.getSubModels().isEmpty());

        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s ->
                                Pattern.matches("Error processing XML data in \\[Service ID .*\\]", s.getMessage())),
                "Expected parsing error status msg");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBuildConfigModelWithEmptySaxEvents() throws InvalidSyntaxException, JoranException {
        BundleContext bundleContext = context.bundleContext();
        ConfigProvider configProvider1 = Mockito.spy(new ConfigProvider() {
            @Override
            public @NotNull InputSource getConfigSource() {
                return new InputSource(new StringReader(""));
            }
        });
        ServiceRegistration<?> serviceReg =
                bundleContext.registerService(ConfigProvider.class, configProvider1, new Hashtable<>(Map.of()));
        ServiceReference<?> serviceRef = serviceReg.getReference();

        // for reference at OsgiAction#getLogbackManager
        loggerContext.putObject(LogConfigManager.class.getName(), mgr);
        // for reference at OsgiAction#getFragmentProviders
        ConfigSourceTracker configSourceTracker = new ConfigSourceTracker(context.bundleContext(), mgr);
        configSourceTracker.addingService((ServiceReference<Object>) serviceRef);
        loggerContext.putObject(ConfigSourceTracker.class.getName(), configSourceTracker);
        action.setContext(loggerContext);

        SaxEventInterpretationContext intercon = mockSaxEventInterpretation();

        // mock sax event recorder return an empty list
        action = Mockito.spy(action);
        SaxEventRecorder ser = new SaxEventRecorder(loggerContext);
        Mockito.doReturn(ser).when(action).populateSaxEventRecorder(any(InputSource.class));

        Model model = action.buildCurrentModel(intercon, "osgi", new AttributesImpl());
        assertTrue(model instanceof OsgiModel);
        assertTrue(model.getSubModels().isEmpty());

        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s -> "Empty sax event list".equals(s.getMessage())),
                "Expected parsing error status msg");
    }
}
