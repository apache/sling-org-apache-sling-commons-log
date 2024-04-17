/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.log.logback.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.apache.sling.commons.log.helpers.LogCapture;
import org.apache.sling.commons.log.logback.ConfigProvider;
import org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext;
import org.apache.sling.commons.log.logback.internal.util.SlingContextUtil;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.apache.sling.commons.log.logback.webconsole.LoggerConfig;
import org.apache.sling.commons.log.logback.webconsole.TailerOptions;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.WarnStatus;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class SlingLogPanelTest {
    private static final class TestTurboFilter extends TurboFilter {
        @Override
        public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
            return FilterReply.NEUTRAL;
        }
    }

    protected final OsgiContext context = new OsgiContextBuilder().build();

    private BundleContext bundleContext;
    private LoggerContext loggerContext;

    private LogConfigManager logConfigManager;
    private SlingLogPanel logPanel;

    @BeforeEach
    protected void beforeEach() {
        bundleContext = Mockito.spy(context.bundleContext());
        loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();

        try {
            System.setProperty(LogConstants.SLING_HOME, new File("target").getAbsolutePath());
            System.setProperty(LogConstants.LOG_FILE, "logs/slingLogPanelTest.log");

            logConfigManager = Mockito.spy(new LogConfigManager(bundleContext));
            logConfigManager.start();
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.SLING_HOME);
        }

        logPanel = new SlingLogPanel(logConfigManager, bundleContext);
    }

    @AfterEach
    protected void afterEach() {
        logConfigManager.stop();
    }

    protected ConfigurationAdmin mockConfigAdmin() {
        ConfigurationAdmin configAdmin = Mockito.spy(context.getService(ConfigurationAdmin.class));
        ServiceReference<ConfigurationAdmin> serviceRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
        Mockito.doReturn(configAdmin).when(bundleContext).getService(serviceRef);
        return configAdmin;
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingLogPanel#tail(java.io.PrintWriter, java.lang.String, org.apache.sling.commons.log.logback.webconsole.TailerOptions)}.
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10})
    void testTail(int numLines) throws IOException {
        org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
        for (int i = 1; i < 15; i++) {
            logger.info(String.format("log message %d", i));
        }
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.tail(pw, "/logs/slingLogPanelTest.log", new TailerOptions(numLines, "*"));

            String output = strWriter.toString();
            assertTrue(output.contains("log message 14"));
            if (numLines <= 0) {
                assertTrue(output.contains("log message 3"));
            } else {
                assertFalse(output.contains("log message 3"));
            }
        }
    }

    @Test
    void testTailWithConsoleAppenderName() throws IOException {
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Hashtable<String, Object> appenderConfig = new Hashtable<>(Map.of(
                    LogConstants.LOG_LOGGERS, List.of("log.testTailWithConsoleAppenderName"),
                    LogConstants.LOG_FILE, LogConstants.FILE_NAME_CONSOLE
                ));
        logConfigManager.addOrUpdateAppender(AppenderOrigin.CONFIGSERVICE, appenderName, appenderConfig);

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.tail(pw, LogConstants.FILE_NAME_CONSOLE, new TailerOptions(10, "*"));

            String output = strWriter.toString();
            assertEquals("No file appender with name [CONSOLE] found", output);
        }
    }
    @Test
    void testTailWithInvalidAppenderName() throws IOException {
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.tail(pw, "invalid", new TailerOptions(10, "*"));

            String output = strWriter.toString();
            assertEquals("No file appender with name [invalid] found", output);
        }
    }

    @Test
    void testTailWithFileNotExist() throws IOException {
        File file = new File("target", "logs/slingLogPanelTest.log");
        assertTrue(file.delete());

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.tail(pw, "/logs/slingLogPanelTest.log", new TailerOptions(10, "*"));

            String output = strWriter.toString();
            assertTrue(output.isEmpty());
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingLogPanel#render(java.io.PrintWriter, java.lang.String)}.
     */
    @Test
    void testRender() throws IOException {
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertFalse(output.contains("<th>Turbo Filter</th>"));
            assertFalse(output.contains("<div class='ui-widget-header ui-corner-top buttonGroup'>Logback Config Fragments</div>"));
            assertTrue(output.contains("<td>Source : Default</td>"));
        }
    }
    @Test
    void testRenderWithThrowableStatus() throws IOException {
        loggerContext.getStatusManager().clear();
        new SlingContextUtil(loggerContext, this)
            .addError("Something happened", new Exception("Only a test"));

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains("<td>Something happened</td>"));
            assertTrue(output.contains("java.lang.Exception: Only a test"));
        }
    }

    @Test
    void testRenderWithNonOsgiConfiguredLogger() throws Exception {
        Logger logger = (Logger)LoggerFactory.getLogger("testRenderWithNonOsgiConfiguredLogger");
        logger.setLevel(Level.INFO);
        Appender<ILoggingEvent> appender = new ListAppender<>();
        appender.setName("myappender1");
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logger.addAppender(appender);

            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains("<div class='ui-widget-header ui-corner-top buttonGroup'>Logger (Configured via other means)</div>"));
            assertTrue(output.contains("<td>testRenderWithNonOsgiConfiguredLogger</td>"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void testRenderWithOsgiConfiguredLogger() throws Exception {
        String pid = String.format("%s~appender1", LogConstants.FACTORY_PID_CONFIGS);
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            logConfigManager.updateLoggerConfiguration(pid, new Hashtable<>(Map.of(
                    LogConstants.LOG_FILE, "logs/testRenderWithOsgiConfiguredLogger.log",
                    LogConstants.LOG_LOGGERS, new String[] {
                              "log.testing"
                          }
                    )), true);
            return null;
        });

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains("<a href=\"configMgr/org.apache.sling.commons.log.LogManager.factory.config~appender1\">org.apache.sling.commons.log.LogManager.factory.config~appender1</a>"));
        }
    }

    @Test
    void testRenderWithTurboFilter() throws IOException {
        TurboFilter turboFilter = new TestTurboFilter();
        ServiceRegistration<TurboFilter> svcReg = bundleContext.registerService(TurboFilter.class, turboFilter, new Hashtable<>());
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains("<th>Turbo Filter</th>"));
        } finally {
            svcReg.unregister();
        }
    }

    @Test
    void testRenderWithLogbackFragment() throws IOException {
        ConfigProvider configProvider = new ConfigProvider() {
            @Override
            public @NotNull InputSource getConfigSource() {
                return new InputSource(new StringReader("<include></include>"));
            }
        };

        ServiceRegistration<ConfigProvider> svcReg = bundleContext.registerService(ConfigProvider.class, configProvider, new Hashtable<>());
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains("<div class='ui-widget-header ui-corner-top buttonGroup'>Logback Config Fragments</div>"));
        } finally {
            svcReg.unregister();
        }
    }

    @Test
    void testRenderWithLogbackFile() throws IOException {
        logConfigManager.stop();

        String logbackFilePath = new File("src/test/resources/logback-test2.xml").getAbsolutePath();
        try {
            System.setProperty(LogConstants.LOGBACK_FILE, logbackFilePath);

            logConfigManager.start();
        } finally {
            System.clearProperty(LogConstants.LOGBACK_FILE);
        }

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            logPanel.render(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains(String.format("<td>Source %s</td>", logbackFilePath)));
        }
    }
    @Test
    void testRenderWithLogbackFileWithCaughtIOException() throws Exception {
        logConfigManager.stop();

        Path tempFilePath = java.nio.file.Files.createTempFile("logback-test2", ".xml");
        try {
            try (InputStream is = getClass().getResourceAsStream("/logback-test2.xml")) {
                Files.copy(is, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            File file = tempFilePath.toFile();
            String logbackFilePath = file.toString();
            try {
                System.setProperty(LogConstants.LOGBACK_FILE, logbackFilePath);

                logConfigManager.start();
            } finally {
                System.clearProperty(LogConstants.LOGBACK_FILE);
            }

            try (StringWriter strWriter = new StringWriter();
                    PrintWriter pw = new PrintWriter(strWriter)) {
                // make the file not readable so it throws IOException during render
                file.setReadable(false);

                // verify that the msg was logged
                try (LogCapture capture = new LogCapture(logPanel.getClass().getName(), true)) {
                    TestUtils.doWorkWithoutRootConsoleAppender(() -> {
                        logPanel.render(pw, null);
                        return null;
                    });

                    // verify the msg was logged
                    capture.assertContains(Level.WARN, String.format("Error occurred while opening file [%s]", file.getAbsolutePath()));
                }

                String output = strWriter.toString();
                assertFalse(output.isEmpty());
                assertFalse(output.contains(String.format("<td>Source %s</td>", logbackFilePath)));
            } finally {
                file.setReadable(true);
            }
        } finally {
            java.nio.file.Files.delete(tempFilePath);
        }
    }

    @Test
    void testRenderWithLogbackUrlWithCaughtIOException() throws Exception {
        URL url = Mockito.spy(getClass().getResource("/logback-invalid.txt"));
        Mockito.doThrow(IOException.class).when(url).openConnection();
        Mockito.doReturn(url).when(logConfigManager).getDefaultConfigURL();
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {

            // verify that the msg was logged
            try (LogCapture capture = new LogCapture(logPanel.getClass().getName(), true)) {
                TestUtils.doWorkWithoutRootConsoleAppender(() -> {
                    logPanel.render(pw, null);
                    return null;
                });

                // verify the msg was logged
                capture.assertContains(Level.WARN, String.format("Error occurred while opening url [%s]", url));
            }

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertFalse(output.contains("<td>Source : Default</td>"));
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingLogPanel#deleteLoggerConfig(java.lang.String)}.
     */
    @Test
    void testDeleteLoggerConfig() throws Exception {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);
        // create a config to delete
        testCreateLoggerConfig(configPid);

        // delete the config and wait for reset
        TestUtils.doWaitForAsyncResetAfterWork((LoggerContext)LoggerFactory.getILoggerFactory(), () -> {
            assertDoesNotThrow(() -> logPanel.deleteLoggerConfig(configPid));

            //workaround the mock configadmin not triggering the delete event
            logConfigManager.updateLoggerConfiguration(configPid, null, true);

            return null;
        });
        // verify the deleted config is is not there
        assertTrue(StreamSupport.stream(logConfigManager.getLogConfigs().spliterator(), false)
            .noneMatch(lc -> configPid.equals(lc.getConfigPid())));

        // TODO verify internalFailure output
    }
    @Test
    void testDeleteLoggerConfigWithCaughtConfigurationException() throws Exception {
        // null pid
        String output = TestUtils.doWorkWithCapturedStdErr(() -> logPanel.deleteLoggerConfig(null));
        assertTrue(output.contains("Reason: PID has to be specified."));

        // workaround the MockConfigurationAdmin always returning non-null mock object
        ConfigurationAdmin configAdmin = mockConfigAdmin();
        Mockito.doReturn(null).when(configAdmin).getConfiguration("invalid");

        // not existing pid
        output = TestUtils.doWorkWithCapturedStdErr(() -> logPanel.deleteLoggerConfig("invalid"));
        assertTrue(output.contains("Reason: No configuration for this PID: invalid"));
    }
    @Test
    void testDeleteLoggerConfigWithNullConfigAdminServiceRef() throws Exception {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);
        Mockito.doReturn(null).when(bundleContext).getServiceReference(ConfigurationAdmin.class);
        assertDoesNotThrow(() -> logPanel.deleteLoggerConfig(configPid));
    }
    @Test
    void testDeleteLoggerConfigWithNullConfigAdminService() throws Exception {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);
        ServiceReference<ConfigurationAdmin> serviceRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
        Mockito.doReturn(null).when(bundleContext).getService(serviceRef);
        assertDoesNotThrow(() -> logPanel.deleteLoggerConfig(configPid));
    }
    @Test
    void testDeleteLoggerConfigWithCaughtIOException() throws Exception {
        // null pid
        String output = TestUtils.doWorkWithCapturedStdErr(() -> logPanel.deleteLoggerConfig(null));
        assertTrue(output.contains("Reason: PID has to be specified."));

        // workaround the MockConfigurationAdmin always returning non-null mock object
        ConfigurationAdmin configAdmin = mockConfigAdmin();
        Mockito.doThrow(IOException.class).when(configAdmin).getConfiguration("invalid");

        // not existing pid
        output = TestUtils.doWorkWithCapturedStdErr(() -> logPanel.deleteLoggerConfig("invalid"));
        assertTrue(output.contains("Cannot delete configuration for pid invalid"));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingLogPanel#createLoggerConfig(org.apache.sling.commons.log.logback.webconsole.LoggerConfig)}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"org.apache.sling.commons.log.LogManager.factory.config~createlogger1"})
    @NullAndEmptySource
    void testCreateLoggerConfig(String configPid) throws Exception {
        String targetPid;
        if (configPid == null || configPid.isEmpty()) {
            targetPid = LogConstants.FACTORY_PID_CONFIGS  + "~testCreateLoggerConfig";
        } else {
            targetPid = configPid;
        }

        // workaround MockConfigurationAdmin#createFactoryConfiguration not implemented
        ConfigurationAdmin mockConfigAdmin = mockConfigAdmin();
        Mockito.doAnswer(inv -> mockConfigAdmin.getConfiguration(targetPid))
            .when(mockConfigAdmin).createFactoryConfiguration(LogConstants.FACTORY_PID_CONFIGS);

        boolean additive = configPid == null || configPid.isEmpty() ? true : false;
        LoggerConfig config = new LoggerConfig(configPid, 
                "warn", new String[] {"log.createLogger"}, "logs/createLogger.log", additive);
        TestUtils.doWaitForAsyncResetAfterWork((LoggerContext)LoggerFactory.getILoggerFactory(), () -> {
            assertDoesNotThrow(() -> logPanel.createLoggerConfig(config));

            //workaround the mock configadmin not triggering the update event
            ServiceReference<ConfigurationAdmin> sr = bundleContext.getServiceReference(ConfigurationAdmin.class);
            assertNotNull(sr);
            try {
                final ConfigurationAdmin configAdmin = this.bundleContext.getService(sr);
                assertNotNull(configAdmin);
                Configuration conf = configAdmin.getConfiguration(targetPid);
                assertNotNull(conf);
                logConfigManager.updateLoggerConfiguration(targetPid, conf.getProperties(), true);
            } finally {
                // release the configadmin reference
                bundleContext.ungetService(sr);
            }

            return null;
        });
        assertTrue(StreamSupport.stream(logConfigManager.getLogConfigs().spliterator(), false)
                .anyMatch(lc -> targetPid.equals(lc.getConfigPid())));
    }
    @Test
    void testCreateLoggerConfigWithNullConfigAdminServiceRef() throws Exception {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);
        Mockito.doReturn(null).when(bundleContext).getServiceReference(ConfigurationAdmin.class);

        LoggerConfig config = new LoggerConfig(configPid, 
                "warn", new String[] {"log.createLogger"}, "logs/createLogger.log", false);
        assertDoesNotThrow(() -> logPanel.createLoggerConfig(config));
    }
    @Test
    void testCreateLoggerConfigWithNullConfigAdminService() throws Exception {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);
        ServiceReference<ConfigurationAdmin> serviceRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
        Mockito.doReturn(null).when(bundleContext).getService(serviceRef);

        LoggerConfig config = new LoggerConfig(configPid, 
                "warn", new String[] {"log.createLogger"}, "logs/createLogger.log", false);
        assertDoesNotThrow(() -> logPanel.createLoggerConfig(config));
    }
    @Test
    void testCreateLoggerConfigWithNullConfiguration() throws Exception {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);

        // workaround MockConfigurationAdmin#createFactoryConfiguration not implemented
        ConfigurationAdmin mockConfigAdmin = mockConfigAdmin();
        Mockito.doReturn(null).when(mockConfigAdmin)
            .getConfiguration(configPid);

        LoggerConfig config = new LoggerConfig(configPid, 
                "warn", new String[] {"log.createLogger"}, "logs/createLogger.log", false);
        assertDoesNotThrow(() -> logPanel.createLoggerConfig(config));
    }

    @Test
    void testCreateLoggerConfigWithCaughtConfigurationException() {
        String configPid = String.format("%s~createlogger1", LogConstants.FACTORY_PID_CONFIGS);
        LoggerConfig config = new LoggerConfig(configPid, 
                null, new String[] {"log.createLogger"}, "logs/createLogger.log", false);
        createLoggerConfigWithCaughtConfigurationException(config, "Reason: Log level has to be specified.");

        config = new LoggerConfig(configPid, 
                "warn", null, "logs/createLogger.log", false);
        createLoggerConfigWithCaughtConfigurationException(config, "Reason: Logger categories have to be specified.");

        config = new LoggerConfig(configPid, 
                "warn", new String[] {"log.createLogger"}, null, false);
        createLoggerConfigWithCaughtConfigurationException(config, "Reason: LogFile name has to be specified.");
    }

    void createLoggerConfigWithCaughtConfigurationException(LoggerConfig config, String reason) {
        loggerContext.getStatusManager().clear();

        String output = TestUtils.doWorkWithCapturedStdErr(() -> {
            try {
                logPanel.createLoggerConfig(config);
            } catch (IOException e) {
                fail("Unexpected IOException");
            }
        });
        assertTrue(output.contains(reason));

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext
                .getStatusManager()
                .getCopyOfStatusList();
        assertFalse(copyOfStatusList.isEmpty());
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> s.getMessage().equals("Failed to create logger config")),
                "Expected internal failure message");
    }

    @Test
    void testAreAllLogfilesInSameFolder() {
        LogConfig logConfig1 = new LogConfig(logConfigManager, LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.getLevelStr"), Level.WARN,
                new File(logConfigManager.getRootDir(), "logs/testAreAllLogfilesInSameFolder1").getAbsolutePath(),
                true, "pid1", true);
        LogConfig logConfig2 = new LogConfig(logConfigManager, LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.getLevelStr"), Level.WARN,
                new File(logConfigManager.getRootDir(), "logs/testAreAllLogfilesInSameFolder2").getAbsolutePath(),
                true, "pid2", true);

        Iterable<LogConfig> logConfigs = List.of(logConfig1, logConfig2);
        String rootPath = logConfigManager.getRootDir();
        assertTrue(logPanel.areAllLogfilesInSameFolder(logConfigs, rootPath));
    }
    @Test
    void testAreAllLogfilesInSameFolderWithNotSame() {
        LogConfig logConfig1 = new LogConfig(logConfigManager, LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.getLevelStr"), Level.WARN,
                new File(logConfigManager.getRootDir(), "logs/testAreAllLogfilesInSameFolder1").getAbsolutePath(),
                true, "pid1", true);
        LogConfig logConfig2 = new LogConfig(logConfigManager, LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.getLevelStr"), Level.WARN,
                "/tmp/logs/testAreAllLogfilesInSameFolder2",
                true, "pid2", true);

        Iterable<LogConfig> logConfigs = List.of(logConfig1, logConfig2);
        String rootPath = logConfigManager.getRootDir();
        assertFalse(logPanel.areAllLogfilesInSameFolder(logConfigs, rootPath));
    }

    @Test
    void testGetLevelStr() {
        // reset to default
        LogConfig logConfig = new LogConfig(logConfigManager, LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.getLevelStr"), Level.WARN, "logs/testGetLevelStr", true, "pid1", true);
        assertEquals("DEFAULT", logPanel.getLevelStr(logConfig));

        // not reset to default
        logConfig = new LogConfig(logConfigManager, LogConstants.LOG_PATTERN_DEFAULT,
                Set.of("log.getLevelStr"), Level.WARN, "logs/testGetLevelStr", true, "pid1", false);
        assertEquals("WARN", logPanel.getLevelStr(logConfig));
    }

    @Test
    void testFormatPidForTurboFilter() {
        String consoleAppRoot = null;
        TurboFilter turboFilter = new TestTurboFilter();
        LoggerStateContext ctx = logConfigManager.determineLoggerState();
        assertEquals("[config]", logPanel.formatPid(consoleAppRoot, turboFilter, ctx));

        ServiceRegistration<TurboFilter> svcReg = bundleContext.registerService(TurboFilter.class, turboFilter, new Hashtable<>());
        try {
            turboFilter = bundleContext.getService(svcReg.getReference());
            ctx = logConfigManager.determineLoggerState();
            String pid = logPanel.formatPid(consoleAppRoot, turboFilter, ctx);
            assertNotNull(pid);
            assertTrue(pid.matches("<a href=\"services\\/\\d+\">\\d+<\\/a>"));
        } finally {
            svcReg.unregister();
        }
    }

    @Test
    void testGetNameForTurboFilter() {
        TurboFilter turboFilter = new TestTurboFilter();
        assertEquals("org.apache.sling.commons.log.logback.internal.SlingLogPanelTest$TestTurboFilter", logPanel.getName(turboFilter));

        turboFilter.setName("turbofilter1");
        assertEquals("turbofilter1 (org.apache.sling.commons.log.logback.internal.SlingLogPanelTest$TestTurboFilter)", logPanel.getName(turboFilter));
    }

    @Test
    void testFormatPidForAppenderNotDynamic() {
        String consoleAppRoot = null;
        Appender<ILoggingEvent> appender = new ListAppender<>();
        LoggerStateContext ctx = logConfigManager.determineLoggerState();
        assertEquals("[others]", logPanel.formatPid(consoleAppRoot, appender, ctx));
    }

    @Test
    void testFormatPidForAppenderDynamic() {
        @SuppressWarnings("rawtypes")
        ServiceRegistration<Appender> svcReg = bundleContext.registerService(Appender.class, new ListAppender<>(), new Hashtable<>(Map.of(
                Constants.SERVICE_PID, "appender1",
                AppenderTracker.PROP_LOGGER, new String[] {"log.logger1", "log.logger2"}
                )));
        try {
            String consoleAppRoot = null;
            @SuppressWarnings("unchecked")
            Appender<ILoggingEvent> appender = bundleContext.getService(svcReg.getReference());
            LoggerStateContext ctx = logConfigManager.determineLoggerState();
            String pid = logPanel.formatPid(consoleAppRoot, appender, ctx);
            assertNotNull(pid);
            assertTrue(pid.matches("<a href=\"services\\/\\d+\">\\d+<\\/a>"));
        } finally {
            svcReg.unregister();
        }
    }

    @Test
    void testFormatPidForAppenderWithSlingRollingFileAppender() {
        SlingRollingFileAppender<ILoggingEvent> appender = new SlingRollingFileAppender<>();
        LogWriter logWriter = new LogWriter("appender1", "logs/appender1.log", -1, null);
        appender.setLogWriter(logWriter);

        String consoleAppRoot = null;
        LoggerStateContext ctx = logConfigManager.determineLoggerState();
        assertEquals("<a href=\"configMgr/org.apache.sling.commons.log.LogManager\">org.apache.sling.commons.log.LogManager</a>",
                logPanel.formatPid(consoleAppRoot, appender, ctx));

        logWriter = new LogWriter("pid1", "appender1", -1, null, "logs/appender1.log", false);
        appender.setLogWriter(logWriter);
        assertEquals("<a href=\"configMgr/pid1\">pid1</a>",
                logPanel.formatPid(consoleAppRoot, appender, ctx));
    }

    @Test
    void testGetNameForAppender() {
        Appender<ILoggingEvent> appender = new ListAppender<>();
        assertEquals("ch.qos.logback.core.read.ListAppender", logPanel.getName(appender));

        appender.setName("appender1");
        assertEquals("appender1 (ch.qos.logback.core.read.ListAppender)", logPanel.getName(appender));

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setName("fileAppender1");
        fileAppender.setFile("logs/logfile.log");
        assertEquals("File : [fileAppender1] logs/logfile.log", logPanel.getName(fileAppender));
    }

    @Test
    void testGetConfigColTitle() {
        assertEquals("PID", logPanel.getConfigColTitle(null));
        assertEquals("Configuration", logPanel.getConfigColTitle("/approot"));
    }

    @Test
    void testCreateUrl() {
        String consoleAppRoot = null;
        String subContext = "/subcontext";
        String pid = "mypid1";
        assertEquals("<a href=\"/subcontext/mypid1\">mypid1</a>", logPanel.createUrl(consoleAppRoot, subContext, pid, true));
        assertEquals("<a href=\"/subcontext/mypid1\">mypid1</a>", logPanel.createUrl(consoleAppRoot, subContext, pid, false));
        assertEquals("<a href=\"/subcontext/mypid1\">mypid1</a>", logPanel.createUrl(consoleAppRoot, subContext, pid));

        consoleAppRoot = "/approot";
        assertEquals("<a class=\"configureLink\" href=\"/subcontext/mypid1\"><img src=\"/approot/res/imgs/component_configure.png\" border=\"0\" /></a>",
                logPanel.createUrl(consoleAppRoot, subContext, pid, true));
        assertEquals("<a  href=\"/subcontext/mypid1\"><img src=\"/approot/res/imgs/component_configure.png\" border=\"0\" /></a>",
                logPanel.createUrl(consoleAppRoot, subContext, pid, false));
    }

    @Test
    void testGetPath() {
        String path = null;
        String rootPath = null;
        assertEquals("[stdout]", logPanel.getPath(path, rootPath, true));
        assertEquals("[stdout]", logPanel.getPath(path, rootPath, false));

        path = "/root/path/here/logs/mylogs.log";
        rootPath = "/root/path/here";
        assertEquals("/root/path/here/logs/mylogs.log", logPanel.getPath(path, rootPath, false));
        assertEquals("logs/mylogs.log", logPanel.getPath(path, rootPath, true));
    }

    @Test
    void testStatusLevelAsString() {
        assertEquals("INFO", logPanel.statusLevelAsString(new InfoStatus("", null)));
        assertEquals("<span class=\"warn\">WARN</span>", logPanel.statusLevelAsString(new WarnStatus("", null)));
        assertEquals("<span class=\"error\">ERROR</span>", logPanel.statusLevelAsString(new ErrorStatus("", null)));
        Status mockStatus = Mockito.mock(Status.class);
        Mockito.doReturn(-1).when(mockStatus).getEffectiveLevel();
        assertNull(logPanel.statusLevelAsString(mockStatus));
    }

    @Test
    void testPrintThrowable() throws IOException {
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            Throwable t = new Exception("Something happened");
            logPanel.printThrowable(pw, t);

            String output = strWriter.toString();
            assertNotNull(output);
            assertTrue(output.contains("java.lang.Exception: Something happened"));
        }
    }
}
