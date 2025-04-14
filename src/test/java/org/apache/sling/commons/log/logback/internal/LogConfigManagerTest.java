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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import org.apache.sling.commons.log.helpers.LogCapture;
import org.apache.sling.commons.log.helpers.ReflectionTools;
import org.apache.sling.commons.log.logback.internal.LogConfigManager.LoggerStateContext;
import org.apache.sling.commons.log.logback.internal.config.ConfigurationException;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class LogConfigManagerTest {
    protected final OsgiContext context = new OsgiContext();
    private LogConfigManager manager;
    private LoggerContext loggerContext;

    @BeforeEach
    protected void beforeEach() {
        System.setProperty(LogConstants.SLING_HOME, new File("target").getAbsolutePath());

        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        manager = new LogConfigManager(context.bundleContext());
    }

    @AfterEach
    protected void afterEach() {
        // cleanup to not interfere with other tests
        System.clearProperty(LogConstants.SLING_HOME);

        // cleanup so we don't interfere with other tests
        if (manager != null) {
            manager.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#internalFailure(java.lang.String, java.lang.Throwable)}.
     */
    @Test
    void testInternalFailure() {
        loggerContext.getStatusManager().clear();

        String output = TestUtils.doWorkWithCapturedStdErr(() -> manager.internalFailure("something happened", null));
        assertTrue(output.contains("something happened"));

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertFalse(copyOfStatusList.isEmpty());
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage().equals("something happened")),
                "Expected internal failure message");
    }

    @Test
    void testInternalFailureWithThrowable() {
        loggerContext.getStatusManager().clear();

        Throwable throwable = new Exception("throwable here");
        String output =
                TestUtils.doWorkWithCapturedStdErr(() -> manager.internalFailure("something happened", throwable));
        assertTrue(output.contains("something happened"));
        assertTrue(output.contains("java.lang.Exception: throwable here"));

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertFalse(copyOfStatusList.isEmpty());
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage().equals("something happened")),
                "Expected internal failure message");
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getThrowable().equals(throwable)),
                "Expected internal failure throwable");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#start()}.
     */
    @Test
    void testStart() {
        assertDoesNotThrow(() -> manager.start());
    }

    @Test
    void testStartWithJulSupport() throws Exception {
        try {
            System.setProperty(LogConstants.JUL_SUPPORT, "true");
            System.setProperty(LogConstants.LOG_FILE, "logs/testStartWithJulSupport.log");

            // verify that the msg was logged
            try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
                assertDoesNotThrow(() -> manager.start());

                // verify the msg was logged
                capture.assertContains(Level.DEBUG, "The JUL logging configuration was reset to empty");
            }

            // confirm JUL calls get routed through logback
            java.util.logging.Logger logger =
                    java.util.logging.Logger.getLogger(getClass().getName());
            assertNull(logger.getLevel());
            logger.warning("Log Message from JUL");

            assertTrue(TestUtils.containsString(
                    new File("target", "logs/testStartWithJulSupport.log"), "Log Message from JUL"));
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.JUL_SUPPORT);
        }
    }

    @Test
    void testStartWithJulSupportWithEventAdminClassNotFound() throws Exception {
        // mock the class not being visible
        manager = Mockito.spy(manager);
        Mockito.doReturn(false).when(manager).isClassNameVisible(EventHandler.class.getName());

        try {
            System.setProperty(LogConstants.LOG_FILE, "logs/testStartWithJulSupportWithEventAdminClassNotFound.log");

            // verify that the msg was logged
            try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
                assertDoesNotThrow(() -> manager.start());

                // verify the msg was logged
                capture.assertContains(
                        Level.WARN,
                        "Failed to register the config reset event handler since the event handler class was not found. "
                                + "Check if the eventadmin bundle is deployed.");
            }
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
        }
    }

    @Test
    void testStartWithJulSupportWithSLF4JBridgeClassNotFound() throws Exception {
        // mock the class not being visible
        manager = Mockito.spy(manager);
        Mockito.doReturn(false).when(manager).isClassNameVisible(SLF4JBridgeHandler.class.getName());

        try {
            System.setProperty(LogConstants.JUL_SUPPORT, "true");
            System.setProperty(LogConstants.LOG_FILE, "logs/testStartWithJulSupportWithSLF4JBridgeClassNotFound.log");

            // verify that the msg was logged
            try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
                assertDoesNotThrow(() -> manager.start());

                // verify the msg was logged
                capture.assertContains(
                        Level.WARN,
                        "Failed to re-configure the SLF4JBridgeHandler since that class was not found. "
                                + "Check if the jul-to-slf4j bundle is deployed.");
            }
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.JUL_SUPPORT);
        }
    }

    @Test
    void testStartWithJulSupportButBridgeAlreadyStarted() throws IOException {
        try {
            SLF4JBridgeHandler.install();
            System.setProperty(LogConstants.JUL_SUPPORT, "true");
            System.setProperty(LogConstants.LOG_FILE, "logs/testStartWithJulSupportButBridgeAlreadyStarted.log");

            // verify the bridge handler install doesn't get called again
            try (MockedStatic<SLF4JBridgeHandler> bridgeMock =
                    Mockito.mockStatic(SLF4JBridgeHandler.class, CALLS_REAL_METHODS); ) {
                // if install() gets called it would throw the exception
                bridgeMock.when(() -> SLF4JBridgeHandler.install()).thenThrow(UnsupportedOperationException.class);

                // verify no execption was thrown
                assertDoesNotThrow(() -> manager.start());
            }

            // confirm JUL calls get routed through logback
            java.util.logging.Logger logger =
                    java.util.logging.Logger.getLogger(getClass().getName());
            assertNull(logger.getLevel());
            logger.warning("Log Message from JUL");

            assertTrue(TestUtils.containsString(
                    new File("target", "logs/testStartWithJulSupportButBridgeAlreadyStarted.log"),
                    "Log Message from JUL"));
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.JUL_SUPPORT);
            SLF4JBridgeHandler.uninstall();
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                LogConstants.SYSPROP_JAVA_UTIL_LOGGING_CONFIG_FILE,
                LogConstants.SYSPROP_JAVA_UTIL_LOGGING_CONFIG_CLASS
            })
    void testStartWithJulSupportWithJULConfigFileSystemPropDefined(String sysProp) throws Exception {
        try {
            System.setProperty(
                    LogConstants.LOG_FILE, "logs/testStartWithJulSupportWithJULConfigFileSystemPropDefined.log");
            System.setProperty(LogConstants.JUL_SUPPORT, "true");
            System.setProperty(sysProp, "somevalue");

            // verify that the msg was logged
            try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
                assertDoesNotThrow(() -> manager.start());

                // verify the msg was logged
                capture.assertContains(
                        Level.DEBUG,
                        "The JUL logging configuration was not reset to empty as JUL config system properties were set");
            }

            // confirm JUL calls get routed through logback
            java.util.logging.Logger logger =
                    java.util.logging.Logger.getLogger(getClass().getName());
            assertNull(logger.getLevel());
            logger.warning("Log Message from JUL");

            assertTrue(TestUtils.containsString(
                    new File("target", "logs/testStartWithJulSupportWithJULConfigFileSystemPropDefined.log"),
                    "Log Message from JUL"));
        } finally {
            System.clearProperty(sysProp);
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.JUL_SUPPORT);
        }
    }

    @Test
    void testStartWithCaughtException() throws Exception {
        // verify the exception handling
        try (MockedStatic<FrameworkUtil> frameworkUtil =
                Mockito.mockStatic(FrameworkUtil.class, CALLS_REAL_METHODS); ) {
            frameworkUtil.when(() -> FrameworkUtil.createFilter(anyString())).thenThrow(InvalidSyntaxException.class);

            // verify that the msg was logged
            try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
                assertDoesNotThrow(() -> manager.start());

                // verify the msg was logged
                capture.assertContains(Level.ERROR, "Failed to open the appender tracker");
                capture.assertContains(Level.ERROR, "Failed to open the filter tracker");
                capture.assertContains(Level.ERROR, "Failed to open the config source tracker");
            }
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#stop()}.
     */
    @Test
    void testStopWithoutPreviousStart() {
        assertDoesNotThrow(() -> manager.stop());
    }

    @Test
    void testStop() throws Exception {
        manager.start();

        // attach a console appender to detach during stop
        Dictionary<String, String> config = new Hashtable<>(
                Map.of(LogConstants.LOG_LEVEL, "info", LogConstants.LOG_FILE, LogConstants.FILE_NAME_CONSOLE));
        doWaitForAsyncResetAfterWork(() -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify that the old console appender was detached
        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.clear();
        manager.stop();
        List<Status> copyOfStatusList = statusManager.getCopyOfStatusList();
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage().equals("detaching appender CONSOLE for ROOT")),
                "Expected detaching appender message");

        String output = TestUtils.doWorkWithCapturedStdOut(() -> {
            LoggerFactory.getLogger(getClass()).info("Logging should go to the CONSOLE after the manager is stopped");
        });
        assertTrue(output.contains("Logging should go to the CONSOLE after the manager is stopped"));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#checkForNewConfigsWhileStarting(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testCheckForNewConfigsWhileStartingWithNullConfigPidSet() {
        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.clear();
        manager.checkForNewConfigsWhileStarting(loggerContext);

        List<Status> copyOfStatusList = statusManager.getCopyOfStatusList();
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage().equals("Did not find any configPid set")),
                "Expected no configPid warning");
    }

    @Test
    void testCheckForNewConfigsWhileStartingWithSameConfigPidSet() {
        manager.start();

        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.clear();
        manager.checkForNewConfigsWhileStarting(loggerContext);

        List<Status> copyOfStatusList = statusManager.getCopyOfStatusList();
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage().equals("Configured the Logback with 1 configs")),
                "Expected configured with configs message");
    }

    @Test
    void testCheckForNewConfigsWhileStartingWithDifferentConfigPidSet() throws Exception {
        manager.start();

        // change something
        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testUpdateLoggerConfiguration"},
                LogConstants.LOG_LEVEL, "warn",
                LogConstants.LOG_FILE, "logs/testUpdateLoggerConfiguration.log"));
        manager.updateLoggerConfiguration(pid, config, false);

        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.clear();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.checkForNewConfigsWhileStarting(loggerContext);
            return null;
        });

        List<Status> copyOfStatusList = statusManager.getCopyOfStatusList();
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage()
                        .equals("Config change detected post start. Scheduling config reload")),
                "Expected config change detected message");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#isPackagingDataEnabled()}.
     */
    @Test
    void testIsPackagingDataEnabled() {
        assertFalse(manager.isPackagingDataEnabled());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getMaxCallerDataDepth()}.
     */
    @Test
    void testGetMaxCallerDataDepth() {
        assertEquals(0, manager.getMaxCallerDataDepth());

        manager.start();

        assertEquals(ClassicConstants.DEFAULT_MAX_CALLEDER_DATA_DEPTH, manager.getMaxCallerDataDepth());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getMaxOldFileCount()}.
     */
    @Test
    void testGetMaxOldFileCount() {
        assertEquals(0, manager.getMaxOldFileCount());

        manager.start();

        assertEquals(LogConstants.PRINTER_MAX_INCLUDED_FILES_DEFAULT, manager.getMaxOldFileCount());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getNumOfLines()}.
     */
    @Test
    void testGetNumOfLines() {
        assertEquals(0, manager.getNumOfLines());

        manager.start();

        assertEquals(LogConstants.PRINTER_NUM_OF_LINES_DEFAULT, manager.getNumOfLines());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getDefaultAppender()}.
     */
    @Test
    void testGetDefaultAppender() {
        Appender<ILoggingEvent> defaultAppender = manager.getDefaultAppender();
        assertNotNull(defaultAppender);
        assertTrue(defaultAppender instanceof ConsoleAppender);
        assertEquals(LogConstants.DEFAULT_CONSOLE_APPENDER_NAME, defaultAppender.getName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#onResetStart(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnResetStart() {
        ch.qos.logback.classic.Logger logger1 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("logger1");
        Appender<ILoggingEvent> appender1 = new ListAppender<>();
        appender1.setName("appender1");
        logger1.addAppender(appender1);
        Appender<ILoggingEvent> appender2 = new ListAppender<>();
        appender2.setName("appender2");
        logger1.addAppender(appender2);
        manager.addedAppenderRef(AppenderOrigin.JORAN, "appender1", "logger1");
        manager.addedAppenderRef(AppenderOrigin.JORAN_OSGI, "appender2", "logger1");
        assertFalse(manager.getKnownAppenders(AppenderOrigin.JORAN).isEmpty());
        assertFalse(manager.getKnownAppenders(AppenderOrigin.JORAN_OSGI).isEmpty());

        manager.onResetStart(loggerContext);
        assertTrue(manager.getKnownAppenders(AppenderOrigin.JORAN).isEmpty());
        assertTrue(manager.getKnownAppenders(AppenderOrigin.JORAN_OSGI).isEmpty());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#onResetComplete(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testOnResetCompleteWithEmptyLogConfigs() {
        assertDoesNotThrow(() -> manager.onResetComplete(loggerContext));
        assertNotNull(loggerContext.getObject(LogConstants.CONFIG_PID_SET));
    }

    @Test
    void testOnResetCompleteWithLogConfigWithAppenderDefined() throws ConfigurationException {
        manager.start();

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testOnResetCompleteWithLogConfigWithAppenderDefined"},
                LogConstants.LOG_LEVEL, "error",
                LogConstants.LOG_FILE, "logs/testOnResetCompleteWithLogConfigWithAppenderDefined.log"));
        manager.updateLoggerConfiguration(pid, config, false);

        assertDoesNotThrow(() -> manager.onResetComplete(loggerContext));

        @SuppressWarnings("unchecked")
        Set<String> configPidSet = ((Set<String>) loggerContext.getObject(LogConstants.CONFIG_PID_SET));
        assertNotNull(configPidSet);
        assertEquals(2, configPidSet.size());
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger("log.testOnResetCompleteWithLogConfigWithAppenderDefined");
        assertEquals(Level.ERROR, logger.getLevel());
    }

    @Test
    void testOnResetCompleteWithLogConfigWithAppenderNotDefined() throws ConfigurationException {
        manager.start();

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_LOGGERS,
                new String[] {"log.testOnResetCompleteWithLogConfigWithAppenderNotDefined"},
                LogConstants.LOG_LEVEL,
                "warn"));
        manager.updateLoggerConfiguration(pid, config, false);

        assertDoesNotThrow(() -> manager.onResetComplete(loggerContext));

        @SuppressWarnings("unchecked")
        Set<String> configPidSet = ((Set<String>) loggerContext.getObject(LogConstants.CONFIG_PID_SET));
        assertNotNull(configPidSet);
        assertEquals(2, configPidSet.size());
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger("log.testOnResetCompleteWithLogConfigWithAppenderNotDefined");
        assertEquals(Level.WARN, logger.getLevel());
    }

    @Test
    void testOnResetCompleteWithBlockedOverrideOfAppenderFromLogbackFile() throws ConfigurationException {
        try {
            System.setProperty(
                    LogConstants.LOGBACK_FILE, new File("src/test/resources/logback-test2.xml").getAbsolutePath());

            manager.start();
        } finally {
            System.clearProperty(LogConstants.LOGBACK_FILE);
        }

        // define a configuration using the same filename as an appender in logback-test2.xml
        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testOnResetCompleteWithLogConfigWithAppenderDefined"},
                LogConstants.LOG_LEVEL, "error",
                LogConstants.LOG_FILE, "target/logs/testing2.log"));
        manager.updateLoggerConfiguration(pid, config, false);

        loggerContext.getStatusManager().clear();
        assertDoesNotThrow(() -> manager.onResetComplete(loggerContext));
        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Found overriding configuration for appender /target/logs/testing2.log in Logback config. OSGi config would be ignored")),
                "Expected overriding configurtion message");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#configChanged()}.
     */
    @Test
    void testConfigChangedWhenNotStarted() throws Exception {
        // bundle is stopping so skipping the unregister of services
        try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
            manager.configChanged();

            // verify the msg was logged
            capture.assertContains(Level.DEBUG, "LoggerContext is not started so skipping reset handling");
        }
    }

    @SuppressWarnings("java:S2699")
    @Test
    void testConfigChanged() throws Exception {
        manager.start();

        doWaitForAsyncResetAfterWork(() -> {
            manager.configChanged();
            return null;
        });
    }

    @Test
    void testConfigChangedWhenFailToAquireLock() throws Exception {
        manager.start();

        loggerContext.getStatusManager().clear();

        mockRestLockIsLocked();
        manager.configChanged();

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        // the last status should be the re-configuration done mst
        assertEquals(
                "LoggerContext reset in progress. Marking config changed to true",
                copyOfStatusList.get(copyOfStatusList.size() - 1).getMessage());
    }

    protected Object doWaitForAsyncResetAfterWork(Callable<?> work) throws Exception {
        return TestUtils.doWaitForAsyncResetAfterWork(loggerContext, work);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#rescheduleIfConfigChanged()}.
     */
    @Test
    void testRescheduleIfConfigChangedWhenConfigNotChanged() {
        manager.start();

        // config not changed
        assertFalse(manager.rescheduleIfConfigChanged());
    }

    @Test
    void testRescheduleIfConfigChangedWhenConfigChanged() throws Exception {
        manager.start();

        // config changed
        ReflectionTools.setFieldWithReflection(manager, "configChanged", true);
        doWaitForAsyncResetAfterWork(() -> {
            assertTrue(manager.rescheduleIfConfigChanged());
            return null;
        });
    }

    @Test
    void testRescheduleIfConfigChangedWhenConfigChangedAndFailToAquireLock() {
        manager.start();

        // config changed
        ReflectionTools.setFieldWithReflection(manager, "configChanged", true);
        mockRestLockIsLocked();
        assertFalse(manager.rescheduleIfConfigChanged());
    }

    protected void mockRestLockIsLocked() {
        Semaphore mockResetLock = Mockito.mock(Semaphore.class);
        Mockito.doReturn(false).when(mockResetLock).tryAcquire();
        ReflectionTools.setFieldWithReflection(manager, "resetLock", mockResetLock);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#scheduleConfigReload()}.
     */
    @Test
    void testScheduleConfigReload() throws Exception {
        doWaitForAsyncResetAfterWork(() -> {
            assertNotNull(manager.scheduleConfigReload());
            return null;
        });
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#fireResetStartListeners()}.
     */
    @Test
    void testFireResetStartListeners() {
        manager.start();

        loggerContext.getStatusManager().clear();

        manager.fireResetStartListeners();
        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertFalse(copyOfStatusList.isEmpty());
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetStart class org.apache.sling.commons.log.logback.internal.AppenderTracker")),
                "Expected onResetStart status for AppenderTracker");
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetStart class org.apache.sling.commons.log.logback.internal.ConfigSourceTracker")),
                "Expected onResetStart status for ConfigSourceTracker");
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetStart class org.apache.sling.commons.log.logback.internal.FilterTracker")),
                "Expected onResetStart status for FilterTracker");
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetStart class org.apache.sling.commons.log.logback.internal.TurboFilterTracker")),
                "Expected onResetStart status for TurboFilterTracker");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#fireResetCompleteListeners()}.
     */
    @Test
    void testFireResetCompleteListeners() {
        manager.start();

        loggerContext.getStatusManager().clear();

        manager.fireResetCompleteListeners();
        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertFalse(copyOfStatusList.isEmpty());
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetComplete class org.apache.sling.commons.log.logback.internal.AppenderTracker")),
                "Expected onRestComplete status for AppenderTracker");
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetComplete class org.apache.sling.commons.log.logback.internal.ConfigSourceTracker")),
                "Expected onRestComplete status for ConfigSourceTracker");
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetComplete class org.apache.sling.commons.log.logback.internal.FilterTracker")),
                "Expected onRestComplete status for FilterTracker");
        assertTrue(
                copyOfStatusList.stream()
                        .anyMatch(
                                s -> s.getMessage()
                                        .equals(
                                                "Firing reset listener - onResetComplete class org.apache.sling.commons.log.logback.internal.TurboFilterTracker")),
                "Expected onRestComplete status for TurboFilterTracker");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getBundleConfiguration(org.osgi.framework.BundleContext)}.
     */
    @Test
    void testGetBundleConfiguration() {
        BundleContext bundleContext = context.bundleContext();
        try {
            System.setProperty(LogConstants.LOG_LEVEL, "warn");
            System.setProperty(LogConstants.LOG_FILE, "logs/testGetBundleConfiguration.log");
            System.setProperty(LogConstants.LOG_FILE_NUMBER, "5");
            System.setProperty(LogConstants.LOG_FILE_SIZE, LogConstants.LOG_FILE_SIZE_DEFAULT);
            System.setProperty(LogConstants.LOG_PATTERN, LogConstants.LOG_PATTERN_DEFAULT);
            System.setProperty(LogConstants.LOGBACK_FILE, "logback.xml");
            System.setProperty(LogConstants.LOG_PACKAGING_DATA, "true");

            Dictionary<String, String> bundleConfiguration = manager.getBundleConfiguration(bundleContext);
            assertEquals("warn", bundleConfiguration.get(LogConstants.LOG_LEVEL));
            assertEquals("logs/testGetBundleConfiguration.log", bundleConfiguration.get(LogConstants.LOG_FILE));
            assertEquals("5", bundleConfiguration.get(LogConstants.LOG_FILE_NUMBER));
            assertEquals(LogConstants.LOG_FILE_SIZE_DEFAULT, bundleConfiguration.get(LogConstants.LOG_FILE_SIZE));
            assertEquals(LogConstants.LOG_PATTERN_DEFAULT, bundleConfiguration.get(LogConstants.LOG_PATTERN));
            assertEquals("logback.xml", bundleConfiguration.get(LogConstants.LOGBACK_FILE));
            assertEquals("true", bundleConfiguration.get(LogConstants.LOG_PACKAGING_DATA));
        } finally {
            System.clearProperty(LogConstants.LOG_LEVEL);
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.LOG_FILE_NUMBER);
            System.clearProperty(LogConstants.LOG_FILE_SIZE);
            System.clearProperty(LogConstants.LOG_PATTERN);
            System.clearProperty(LogConstants.LOGBACK_FILE);
            System.clearProperty(LogConstants.LOG_PACKAGING_DATA);
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#setDefaultConfiguration(java.util.Dictionary)}.
     */
    @Test
    void testSetDefaultConfiguration() {
        Dictionary<String, String> bundleConfiguration = manager.getBundleConfiguration(context.bundleContext());
        String output = TestUtils.doWorkWithCapturedStdErr(() -> manager.setDefaultConfiguration(bundleConfiguration));
        assertTrue(output.isEmpty());
    }

    @Test
    void testSetDefaultConfigurationWithCaughtException() {
        loggerContext.getStatusManager().clear();

        String output = TestUtils.doWorkWithCapturedStdErr(
                () -> manager.setDefaultConfiguration(new Hashtable<>(Map.of(LogConstants.LOG_LEVEL, "invalid"))));
        assertTrue(output.contains("Unexpected Configuration Problem"));

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertFalse(copyOfStatusList.isEmpty());
        assertTrue(
                copyOfStatusList.stream().anyMatch(s -> s.getMessage().equals("Unexpected Configuration Problem")),
                "Expected internal failure message");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getAbsoluteFilePath(java.lang.String)}.
     */
    @Test
    void testGetAbsoluteFilePath() throws IOException {
        // absolute path
        assertEquals(Paths.get("/tmp/path1").toAbsolutePath().toString(), manager.getAbsoluteFilePath("/tmp/path1"));

        // relative path
        String slingHome = new File(System.getProperty(LogConstants.SLING_HOME)).getAbsolutePath();
        assertEquals(
                new File(slingHome, "log/error.log").getAbsolutePath(), manager.getAbsoluteFilePath("log/error.log"));

        // relative path with no sling.home value
        System.clearProperty(LogConstants.SLING_HOME);
        manager.stop();
        manager = new LogConfigManager(context.bundleContext());
        assertEquals(
                Paths.get("log/error.log").toFile().getAbsolutePath(), manager.getAbsoluteFilePath("log/error.log"));
    }

    @Test
    void testGetAbsoluteFilePathWithRootPath() throws IOException {
        try {
            System.setProperty(LogConstants.SLING_LOG_ROOT, new File("target2").getPath());
            manager.stop();
            manager = new LogConfigManager(context.bundleContext());

            // relative path
            String slingHome = new File(System.getProperty(LogConstants.SLING_LOG_ROOT)).getAbsolutePath();
            assertEquals(
                    new File(slingHome, "log/error.log").getAbsolutePath(),
                    manager.getAbsoluteFilePath("log/error.log"));
        } finally {
            System.clearProperty(LogConstants.SLING_LOG_ROOT);
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#updateGlobalConfiguration(java.util.Dictionary)}.
     */
    @Test
    void testUpdateGlobalConfiguration() throws Exception {
        System.clearProperty(LogConstants.SLING_HOME);
        manager.stop();
        manager = new LogConfigManager(context.bundleContext());
        manager.start();

        Dictionary<String, String> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PACKAGING_DATA, "true",
                LogConstants.LOG_PATTERN, "Custom %msg%n",
                LogConstants.LOG_LEVEL, "warn",
                LogConstants.LOG_FILE, "target/logs/error2.log",
                LogConstants.LOG_FILE_NUMBER, "6",
                LogConstants.LOG_FILE_SIZE, "logs/error2.%d{yyyy-MM-dd}.log.gz"));
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify the updated changes were applied
        assertFalse(loggerContext.isPackagingDataEnabled());
        assertTrue(manager.isPackagingDataEnabled());
        ch.qos.logback.classic.Logger rootLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.WARN, rootLogger.getLevel());
        Appender<ILoggingEvent> appender = rootLogger.getAppender("/target/logs/error2.log");
        assertTrue(appender instanceof RollingFileAppender);
        String expectedPath =
                Paths.get("target/logs/error2.log").toAbsolutePath().toString();
        assertEquals(expectedPath, ((RollingFileAppender<ILoggingEvent>) appender).getFile());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", LogConstants.FILE_NAME_CONSOLE})
    void testUpdateGlobalConfigurationWithConsoleAppender(String file) throws Exception {
        manager.start();

        Dictionary<String, String> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PACKAGING_DATA, "true",
                LogConstants.LOG_PATTERN, "Custom %msg%n",
                LogConstants.LOG_LEVEL, "warn",
                LogConstants.LOG_FILE, file));
        doWaitForAsyncResetAfterWork(() -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify the updated changes were applied
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertFalse(loggerContext.isPackagingDataEnabled());
        assertTrue(manager.isPackagingDataEnabled());
        ch.qos.logback.classic.Logger rootLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.WARN, rootLogger.getLevel());
        Appender<ILoggingEvent> appender = rootLogger.getAppender(LogConstants.FILE_NAME_CONSOLE);
        assertTrue(appender instanceof ConsoleAppender);
    }

    @Test
    void testUpdateGlobalConfigurationWithNullConfiguration() throws Exception {
        manager.start();

        Dictionary<String, String> config = null;
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger rootLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> appender = rootLogger.getAppender(LogConstants.FILE_NAME_CONSOLE);
        assertTrue(appender instanceof ConsoleAppender);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#updateLogWriter(java.lang.String, java.util.Dictionary, boolean)}.
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpdateLogWriter(boolean withRefresh) throws Exception {
        String pid = String.format("%s~logwriter1", LogConstants.FACTORY_PID_CONFIGS);
        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        if (withRefresh) {
            manager.start();

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
                manager.updateLogWriter(pid, new Hashtable<>(Map.of(LogConstants.LOG_FILE, filename1)), withRefresh);
                return null;
            });
        } else {
            manager.updateLogWriter(pid, new Hashtable<>(Map.of(LogConstants.LOG_FILE, filename1)), withRefresh);
        }
        assertTrue(manager.hasWriterByPid(pid));
        assertTrue(manager.hasWriterByName(filename1));

        LogWriter logWriter = manager.getLogWriter(filename1);
        assertNotNull(logWriter);
        assertEquals(filename1, logWriter.getFileName());
        assertEquals("/logs/logwriter1.log", logWriter.getAppenderName());
    }

    @Test
    void testUpdateLogWriterTwice() throws Exception {
        // first call for add
        testUpdateLogWriter(false);

        // second carll for update
        testUpdateLogWriter(false);
    }

    @Test
    void testUpdateLogWriterWithInvalidLogFileNumber() throws ConfigurationException {
        String pid = String.format("%s~logwriter1", LogConstants.FACTORY_PID_CONFIGS);
        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        manager.updateLogWriter(
                pid,
                new Hashtable<>(Map.of(LogConstants.LOG_FILE, filename1, LogConstants.LOG_FILE_NUMBER, "invalid")),
                false);
        assertTrue(manager.hasWriterByPid(pid));
        assertTrue(manager.hasWriterByName(filename1));
        assertEquals(
                LogConstants.LOG_FILE_NUMBER_DEFAULT,
                manager.getLogWriter(filename1).getLogNumber());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUpdateLogWriterWithNullFileName(String filename) throws ConfigurationException {
        String pid = String.format("%s~logwriter1", LogConstants.FACTORY_PID_CONFIGS);
        Hashtable<String, Object> config = new Hashtable<>();
        if (filename != null) {
            config.put(LogConstants.LOG_FILE, filename);
        }
        manager.updateLogWriter(pid, config, false);
        assertTrue(manager.hasWriterByPid(pid));
        assertTrue(manager.hasWriterByName(LogConstants.FILE_NAME_CONSOLE));

        LogWriter logWriter = manager.getLogWriter(LogConstants.FILE_NAME_CONSOLE);
        assertNotNull(logWriter);
    }

    @Test
    void testUpdateLogWriterWithNullConfiguration() throws Exception {
        testUpdateLogWriter(false);

        String pid = String.format("%s~logwriter1", LogConstants.FACTORY_PID_CONFIGS);
        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        manager.updateLogWriter(pid, null, false);
        assertFalse(manager.hasWriterByPid(pid));
        assertFalse(manager.hasWriterByName(filename1));

        // one more time (for code coverage) should do nothing
        manager.updateLogWriter(pid, null, false);
        assertFalse(manager.hasWriterByPid(pid));
        assertFalse(manager.hasWriterByName(filename1));
    }

    @Test
    void testUpdateLogWriterWithNotUniqueFilename() throws Exception {
        testUpdateLogWriter(false);

        String pid = String.format("%s~logwriter2", LogConstants.FACTORY_PID_CONFIGS);
        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        assertThrows(
                ConfigurationException.class,
                () -> manager.updateLogWriter(pid, new Hashtable<>(Map.of(LogConstants.LOG_FILE, filename1)), false));
    }

    @Test
    void testIsLogbackFileValidWhenFileDoesNotExist() throws Exception {
        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
            File file = new File("test/logback-invalid.xml");
            String logbackPath = file.getAbsolutePath();
            assertFalse(manager.isLogbackFileValid(file));

            // verify the msg was logged
            capture.assertContains(
                    Level.WARN, String.format("Logback configuration file [%s] does not exist", logbackPath));
        }
    }

    @Test
    void testIsLogbackFileValidWhenFileIsNotAFile() throws Exception {
        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
            File file = new File(new File(".").getCanonicalPath());
            String logbackPath = file.getCanonicalPath();
            assertFalse(manager.isLogbackFileValid(file));

            // verify the msg was logged
            capture.assertContains(
                    Level.WARN, String.format("Logback configuration file [%s] is not a file", logbackPath));
        }
    }

    @Test
    void testIsLogbackFileValidWhenFileIsNotReadable() throws Exception {
        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
            File file = Paths.get("src/test/resources/logback-test.xml").toFile();
            String logbackPath = file.getAbsolutePath();
            file = Mockito.spy(file);
            Mockito.doReturn(false).when(file).canRead();
            assertFalse(manager.isLogbackFileValid(file));

            // verify the msg was logged
            capture.assertContains(
                    Level.WARN, String.format("Logback configuration file [%s] cannot be read", logbackPath));
        }
    }

    @Test
    void testUpdateGlobalConfigurationFromNotExistingLogbackXmlFile() throws Exception {
        String logbackPath = new File("src/test/resources/logback-notexisting.xml").getAbsolutePath();
        Dictionary<String, String> config = new Hashtable<>(Map.of(LogConstants.LOGBACK_FILE, logbackPath));
        // verify that the msg was logged
        try (LogCapture capture = new LogCapture(manager.getClass().getName(), true)) {
            manager.updateGlobalConfiguration(config);

            // verify the msg was logged
            capture.assertContains(
                    Level.WARN, String.format("Logback configuration file [%s] does not exist", logbackPath));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"logback-invalid.txt", "logback-invalid.xml"})
    void testUpdateGlobalConfigurationFromInvalidLogbackXmlFile(String file) throws Exception {
        manager.start();

        String logbackPath = new File("src/test/resources/" + file).getAbsolutePath();
        Dictionary<String, String> config = new Hashtable<>(Map.of(LogConstants.LOGBACK_FILE, logbackPath));

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().clear();

        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify the error status was reported
        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(
                                s -> "Given previous errors, falling back to previously registered safe configuration."
                                        .equals(s.getMessage())),
                "Expected error status msg");
    }

    @Test
    void testUpdateGlobalConfigurationWithDebug() throws Exception {
        try {
            System.setProperty(LogConstants.DEBUG, "true");
            manager = new LogConfigManager(context.bundleContext());
        } finally {
            System.clearProperty(LogConstants.DEBUG);
        }
        manager.start();

        String logbackPath = new File("src/test/resources/logback-invalid.txt").getAbsolutePath();
        Dictionary<String, String> config = new Hashtable<>(Map.of(LogConstants.LOGBACK_FILE, logbackPath));

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().clear();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify the error status was reported
        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(
                                s -> "Given previous errors, falling back to previously registered safe configuration."
                                        .equals(s.getMessage())),
                "Expected error status msg");
    }

    @Test
    void testUpdateGlobalConfigurationWithNullSafeJoranConfiguration() throws Exception {
        manager.start();

        String logbackPath = new File("src/test/resources/logback-invalid.txt").getAbsolutePath();
        Dictionary<String, String> config = new Hashtable<>(Map.of(LogConstants.LOGBACK_FILE, logbackPath));

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Object originalSafeConfig = loggerContext.getObject(CoreConstants.SAFE_JORAN_CONFIGURATION);
        try {
            // set the safe configuration to null
            loggerContext.removeObject(CoreConstants.SAFE_JORAN_CONFIGURATION);
            loggerContext.getStatusManager().clear();

            TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
                manager.updateGlobalConfiguration(config);
                return null;
            });
        } finally {
            loggerContext.putObject(CoreConstants.SAFE_JORAN_CONFIGURATION, originalSafeConfig);
        }

        // verify the error status was reported
        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s -> "No previous configuration to fall back on.".equals(s.getMessage())),
                "Expected error status msg");
    }

    @Test
    void testUpdateGlobalConfigurationWithSafeJoranConfigurationThatThrowsException() throws Exception {
        manager.start();

        String logbackPath = new File("src/test/resources/logback-invalid.txt").getAbsolutePath();
        Dictionary<String, String> config = new Hashtable<>(Map.of(LogConstants.LOGBACK_FILE, logbackPath));

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Object originalSafeConfig = loggerContext.getObject(CoreConstants.SAFE_JORAN_CONFIGURATION);
        try {
            // set the safe configuration to something that will throw an
            //  unexpected exception during processing
            Model model = new Model() {
                private static final long serialVersionUID = 1724281914903815476L;

                @Override
                public List<Model> getSubModels() {
                    throw new IllegalStateException("Something is wrong");
                }
            };
            loggerContext.putObject(CoreConstants.SAFE_JORAN_CONFIGURATION, model);
            loggerContext.getStatusManager().clear();

            TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
                manager.updateGlobalConfiguration(config);
                return null;
            });
        } finally {
            loggerContext.putObject(CoreConstants.SAFE_JORAN_CONFIGURATION, originalSafeConfig);
        }

        // verify the error status was reported
        assertTrue(
                loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s -> "Unexpected exception thrown by a configuration considered safe."
                                .equals(s.getMessage())),
                "Expected error status msg");
    }

    @Test
    void testUpdateGlobalConfigurationFromLogbackXmlFile() throws Exception {
        manager.start();

        Dictionary<String, String> config = new Hashtable<>(
                Map.of(LogConstants.LOGBACK_FILE, new File("src/test/resources/logback-test2.xml").getAbsolutePath()));
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // verify the updated changes were applied
        assertTrue(loggerContext.isPackagingDataEnabled());
        ch.qos.logback.classic.Logger auditLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.audit");
        assertEquals(Level.WARN, auditLogger.getLevel());
        ch.qos.logback.classic.Logger rootLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> appender = rootLogger.getAppender("/target/logs/testing2.log");
        assertTrue(appender instanceof FileAppender);
        assertEquals("target/logs/testing2.log", ((FileAppender<ILoggingEvent>) appender).getFile());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#addOrUpdateAppender(java.util.Dictionary, java.lang.String, java.lang.String[])}.
     */
    @Test
    void testAddOrUpdateAppenderFromConfiguration() {
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testAddOrUpdateAppender"},
                LogConstants.LOG_LEVEL, "debug",
                LogConstants.LOG_FILE, "logs/testAddOrUpdateAppender.log"));
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        manager.addOrUpdateAppender(AppenderOrigin.CONFIGSERVICE, appenderName, config);

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppender");
        assertEquals(Level.DEBUG, logger.getLevel());
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        assertTrue(appender instanceof RollingFileAppender);
        String expectedPath = Paths.get(System.getProperty(LogConstants.SLING_HOME), "logs/testAddOrUpdateAppender.log")
                .toAbsolutePath()
                .toString();
        assertEquals(expectedPath, ((RollingFileAppender<ILoggingEvent>) appender).getFile());
    }

    @Test
    void testAddOrUpdateAppenderFromConfigurationWithFilterTracker() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        bundleContext.registerService(
                Filter.class,
                filter,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName, "invalid"})));

        manager.start();
        testAddOrUpdateAppenderFromConfiguration();

        // verify the filter was attached to the logger
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppender");
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter));
    }

    @Test
    void testAddOrUpdateAppender() {
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Appender<ILoggingEvent> appender = new ListAppender<>();
        Collection<String> loggers = List.of("log.testAddOrUpdateAppender");
        manager.addOrUpdateAppender(AppenderOrigin.CONFIGSERVICE, appenderName, appender, loggers);

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppender");
        assertSame(appender, logger.getAppender(appenderName));
    }

    @Test
    void testAddOrUpdateAppenderWithFilterTracker() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        bundleContext.registerService(
                Filter.class,
                filter,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName, "invalid"})));

        manager.start();
        testAddOrUpdateAppender();

        // verify the filter was attached to the logger
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppender");
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        assertTrue(appender.getCopyOfAttachedFiltersList().contains(filter));
    }

    @Test
    void testAddOrUpdateAppenderFromConfig() {
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Hashtable<String, Object> appenderConfig = new Hashtable<>(Map.of(
                LogConstants.LOG_FILE,
                "logs/testAddOrUpdateAppenderFromConfig.log",
                LogConstants.LOG_LOGGERS,
                List.of("log.testAddOrUpdateAppenderFromConfig")));
        manager.addOrUpdateAppender(AppenderOrigin.CONFIGSERVICE, appenderName, appenderConfig);

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppenderFromConfig");
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        assertTrue(appender instanceof RollingFileAppender);
        String expectedPath = new File(
                        System.getProperty(LogConstants.SLING_HOME), "logs/testAddOrUpdateAppenderFromConfig.log")
                .getAbsolutePath();
        assertEquals(expectedPath, ((RollingFileAppender<ILoggingEvent>) appender).getFile());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {LogConstants.FILE_NAME_CONSOLE})
    void testAddOrUpdateAppenderFromConfigForConsole(String file) {
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        Hashtable<String, Object> appenderConfig = new Hashtable<>(
                Map.of(LogConstants.LOG_LOGGERS, List.of("log.testAddOrUpdateAppenderFromConfigForConsole")));
        if (file != null) {
            appenderConfig.put(LogConstants.LOG_FILE, file);
        }
        manager.addOrUpdateAppender(AppenderOrigin.CONFIGSERVICE, appenderName, appenderConfig);

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger("log.testAddOrUpdateAppenderFromConfigForConsole");
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        assertTrue(appender instanceof ConsoleAppender);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#maybeDetachAppender(java.lang.String, ch.qos.logback.classic.Logger)}.
     */
    @Test
    void testMaybeDetachAppender() {
        String appenderName = "myappender1";
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("testing");
        Appender<ILoggingEvent> appender = new ListAppender<>();
        appender.setName(appenderName);
        logger.addAppender(appender);
        manager.maybeDetachAppender(AppenderOrigin.CONFIGSERVICE, appenderName, logger);
        assertFalse(logger.isAttached(appender));

        // one more time to verify nothing else is done
        manager.maybeDetachAppender(AppenderOrigin.CONFIGSERVICE, appenderName, logger);
    }

    @Test
    void testMaybeDetachAppenderWithFilter() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";
        bundleContext.registerService(
                Filter.class,
                filter,
                new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {appenderName, "invalid"})));

        manager.start();
        testAddOrUpdateAppender();

        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testAddOrUpdateAppender");
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        manager.maybeDetachAppender(AppenderOrigin.CONFIGSERVICE, appenderName, logger);

        // verify the filter was detached from the appender
        assertNull(logger.getAppender(appenderName));
        assertFalse(appender.getCopyOfAttachedFiltersList().contains(filter));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#updateLoggerConfiguration(java.lang.String, java.util.Dictionary)}.
     */
    @Test
    void testUpdateLoggerConfiguration() throws Exception {
        manager.start();

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testUpdateLoggerConfiguration"},
                LogConstants.LOG_LEVEL, "warn",
                LogConstants.LOG_FILE, "logs/testUpdateLoggerConfiguration.log"));

        doWaitForAsyncResetAfterWork(() -> {
            manager.updateLoggerConfiguration(pid, config, true);
            return null;
        });
        assertTrue(manager.hasConfigByPid(pid));
        assertTrue(manager.hasConfigByName("log.testUpdateLoggerConfiguration"));

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testUpdateLoggerConfiguration");
        assertEquals(Level.WARN, logger.getLevel());
        Appender<ILoggingEvent> appender = logger.getAppender("/logs/testUpdateLoggerConfiguration.log");
        assertTrue(appender instanceof SlingRollingFileAppender);
        String expectedPath = Paths.get("target/logs/testUpdateLoggerConfiguration.log")
                .toAbsolutePath()
                .toString();
        assertEquals(expectedPath, ((RollingFileAppender<ILoggingEvent>) appender).getFile());
    }

    @Test
    void testUpdateLoggerConfigurationWithNullFile() throws Exception {
        manager.start();

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testUpdateLoggerConfigurationWithNullFile"},
                LogConstants.LOG_LEVEL, "warn"));

        doWaitForAsyncResetAfterWork(() -> {
            manager.updateLoggerConfiguration(pid, config, true);
            return null;
        });
        assertTrue(manager.hasConfigByPid(pid));
        assertTrue(manager.hasConfigByName("log.testUpdateLoggerConfigurationWithNullFile"));

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger("log.testUpdateLoggerConfigurationWithNullFile");
        assertEquals(Level.WARN, logger.getLevel());
        Appender<ILoggingEvent> appender = logger.getAppender(LogConstants.FILE_NAME_CONSOLE);
        assertNull(appender);
    }

    @Test
    void testUpdateLoggerConfigurationWithEmptyFile() throws Exception {
        manager.start();

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testUpdateLoggerConfigurationWithNullFile"},
                LogConstants.LOG_LEVEL, "warn",
                LogConstants.LOG_FILE, " "));

        doWaitForAsyncResetAfterWork(() -> {
            manager.updateLoggerConfiguration(pid, config, true);
            return null;
        });
        assertTrue(manager.hasConfigByPid(pid));
        assertTrue(manager.hasConfigByName("log.testUpdateLoggerConfigurationWithNullFile"));

        // verify the updated changes were applied
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger("log.testUpdateLoggerConfigurationWithNullFile");
        assertEquals(Level.WARN, logger.getLevel());
        Appender<ILoggingEvent> appender = logger.getAppender(LogConstants.FILE_NAME_CONSOLE);
        assertNotNull(appender);
    }

    @Test
    void testUpdateLoggerConfigurationWithLevelResetToDefault() throws Exception {
        manager.start();

        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testUpdateLoggerConfiguration");
        logger.setLevel(Level.WARN);

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN,
                "%msg%n",
                LogConstants.LOG_LOGGERS,
                new String[] {"log.testUpdateLoggerConfiguration"},
                LogConstants.LOG_LEVEL,
                LogConstants.LOG_LEVEL_RESET_TO_DEFAULT,
                LogConstants.LOG_FILE,
                "logs/testUpdateLoggerConfigurationWithLevelResetToDefault.log"));

        doWaitForAsyncResetAfterWork(() -> {
            manager.updateLoggerConfiguration(pid, config, true);
            return null;
        });
        assertTrue(manager.hasConfigByPid(pid));
        assertTrue(manager.hasConfigByName("log.testUpdateLoggerConfiguration"));

        // verify the updated changes were applied
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.testUpdateLoggerConfiguration");
        assertNull(logger.getLevel());
    }

    @Test
    void testUpdateLoggerConfigurationWithNullConfiguration() throws Exception {
        testUpdateLoggerConfiguration();

        String pid = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        doWaitForAsyncResetAfterWork(() -> {
            manager.updateLoggerConfiguration(pid, null, true);
            return null;
        });
        assertFalse(manager.hasConfigByPid(pid));
        assertFalse(manager.hasConfigByName("log.testUpdateLoggerConfiguration"));

        // one more time for code coverage (does nothing
        manager.updateLoggerConfiguration(pid, null, false);
        assertFalse(manager.hasConfigByPid(pid));
        assertFalse(manager.hasConfigByName("log.testUpdateLoggerConfiguration"));
    }

    @Test
    void testUpdateLoggerConfigurationWithLoggerNameExistsInTwoConfigs() throws ConfigurationException {
        manager.start();

        // create and register the first config
        String pid1 = LogConstants.FACTORY_PID_CONFIGS + "~myappender2";
        Dictionary<String, ?> config1 = new Hashtable<>(Map.of(
                LogConstants.LOG_PATTERN, "%msg%n",
                LogConstants.LOG_LOGGERS, new String[] {"log.testUpdateLoggerConfiguration"},
                LogConstants.LOG_LEVEL, "warn",
                LogConstants.LOG_FILE, "logs/testUpdateLoggerConfiguration.log"));
        manager.updateLoggerConfiguration(pid1, config1, false);

        // create and register the second config should report the exception
        String pid2 = LogConstants.FACTORY_PID_CONFIGS + "~myappender3";
        ConfigurationException ex = assertThrows(
                ConfigurationException.class, () -> manager.updateLoggerConfiguration(pid2, config1, false));
        assertEquals(LogConstants.LOG_LOGGERS, ex.getProperty());
        assertEquals(
                "Category log.testUpdateLoggerConfiguration already defined by configuration org.apache.sling.commons.log.LogManager.factory.config~myappender2",
                ex.getReason());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getKnownAppenders()}.
     */
    @Test // TODO: try the other AppenderOrigin values as well
    void testGetKnownAppenders() {
        assertTrue(manager.getKnownAppenders(AppenderOrigin.CONFIGSERVICE).isEmpty());

        testAddOrUpdateAppender();
        assertTrue(manager.getKnownAppenders(AppenderOrigin.CONFIGSERVICE)
                .containsKey(String.format("%s~myappender1", LogConstants.FACTORY_PID_CONFIGS)));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getLoggerNamesForKnownAppender(java.lang.String)}.
     */
    @Test
    void testGetLoggerNamesForKnownAppender() {
        String appenderName = LogConstants.FACTORY_PID_CONFIGS + "~myappender1";

        Set<String> loggerNames = manager.getLoggerNamesForKnownAppender(AppenderOrigin.CONFIGSERVICE, appenderName);
        assertTrue(loggerNames.isEmpty());

        testAddOrUpdateAppender();

        loggerNames = manager.getLoggerNamesForKnownAppender(AppenderOrigin.CONFIGSERVICE, appenderName);
        assertFalse(loggerNames.isEmpty());
        assertTrue(loggerNames.contains("log.testAddOrUpdateAppender"));
    }

    @Test
    void testFirstAppenderFromLoggersForFoundAppender() {
        ch.qos.logback.classic.Logger logger1 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("log.logger1");
        Appender<ILoggingEvent> appender1 = new ListAppender<>();
        appender1.setName("knownappender1");
        try {
            logger1.addAppender(appender1);
            assertNotNull(manager.firstAppenderFromLoggers("knownappender1", List.of("log.logger1")));
        } finally {
            logger1.detachAppender(appender1);
        }
    }

    @Test
    void testFirstAppenderFromLoggersForNotFoundAppender() {
        assertNull(manager.firstAppenderFromLoggers("knownappender1", List.of("log.logger1")));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#addedAppenderRef(java.lang.String, java.lang.String)}.
     */
    @Test
    void testAddedAppenderRef() {
        Appender<ILoggingEvent> appender1 = new ListAppender<>();
        appender1.setName("appender1");
        ch.qos.logback.classic.Logger logger1 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("logger1");
        logger1.addAppender(appender1);

        // does not exist before
        assertNull(manager.getKnownAppenders(AppenderOrigin.JORAN).get("appender1"));

        manager.addedAppenderRef(AppenderOrigin.JORAN, "appender1", "logger1");

        // exists after
        assertEquals(appender1, manager.getKnownAppenders(AppenderOrigin.JORAN).get("appender1"));
    }

    @Test
    void testAddedAppenderRefWithFilterTracker() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        bundleContext.registerService(
                Filter.class, filter, new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {"appender1"})));

        manager.start();

        Appender<ILoggingEvent> appender1 = new ListAppender<>();
        appender1.setName("appender1");
        ch.qos.logback.classic.Logger logger1 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("logger1");
        logger1.addAppender(appender1);

        // does not exist before
        assertNull(manager.getKnownAppenders(AppenderOrigin.JORAN).get("appender1"));

        manager.addedAppenderRef(AppenderOrigin.JORAN, "appender1", "logger1");

        // exists after
        assertEquals(appender1, manager.getKnownAppenders(AppenderOrigin.JORAN).get("appender1"));

        // filter was attached to the appender by the FilterTracker
        assertTrue(appender1.getCopyOfAttachedFiltersList().contains(filter));
    }

    @Test
    void testAddedAppenderRefWithNullAppenderAndFilterTracker() {
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.NEUTRAL;
            }
        };

        BundleContext bundleContext = context.bundleContext();
        bundleContext.registerService(
                Filter.class, filter, new Hashtable<>(Map.of(FilterTracker.PROP_APPENDER, new String[] {"appender1"})));

        manager.start();

        // does not exist before
        assertNull(manager.getKnownAppenders(AppenderOrigin.JORAN).get("appender1"));

        manager.addedAppenderRef(AppenderOrigin.JORAN, "appender1", "logger1");

        // still not exists after
        assertNull(manager.getKnownAppenders(AppenderOrigin.JORAN).get("appender1"));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#addedOsgiAppenderRef(java.lang.String, java.lang.String)}.
     */
    @Test
    void testAddedOsgiAppenderRef() {
        Appender<ILoggingEvent> appender1 = new ListAppender<>();
        appender1.setName("appender1");
        ch.qos.logback.classic.Logger logger1 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("logger1");
        logger1.addAppender(appender1);

        // does not exist before
        assertNull(manager.getKnownAppenders(AppenderOrigin.JORAN_OSGI).get("appender1"));

        manager.addedAppenderRef(AppenderOrigin.JORAN_OSGI, "appender1", "logger1");

        // exists after
        assertEquals(
                appender1, manager.getKnownAppenders(AppenderOrigin.JORAN_OSGI).get("appender1"));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#addSubsitutionProperties(ch.qos.logback.core.spi.PropertyContainer)}.
     */
    @ParameterizedTest
    @ValueSource(strings = {LogConstants.SLING_LOG_ROOT, LogConstants.SLING_HOME})
    void testAddSubsitutionPropertiesWithLogRootDefined(String systemPropName) {
        try {
            System.setProperty(systemPropName, new File("target2").getPath());
            manager.stop();
            manager = new LogConfigManager(context.bundleContext());

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
            manager.addSubsitutionProperties(mic);
            assertEquals(
                    Paths.get(System.getProperty(systemPropName), "logs/testing.log")
                            .toAbsolutePath()
                            .toString(),
                    Paths.get(mic.subst("${sling.home}/logs/testing.log")).toString());
        } finally {
            System.clearProperty(systemPropName);
        }
    }

    @Test
    void testAddSubsitutionPropertiesWithoutDefinedRootDir() {
        System.clearProperty(LogConstants.SLING_HOME);
        manager.stop();
        manager = new LogConfigManager(context.bundleContext());

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ModelInterpretationContext mic = new ModelInterpretationContext(loggerContext);
        manager.addSubsitutionProperties(mic);
        assertEquals(
                Paths.get("logs/testing.log").toAbsolutePath().toString(),
                Paths.get(mic.subst("${sling.home}/logs/testing.log")).toString());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getPackageInfoCollector()}.
     */
    @Test
    void testGetPackageInfoCollector() {
        assertNotNull(manager.getPackageInfoCollector());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getDefaultWriter()}.
     */
    @Test
    void testGetDefaultWriter() {
        assertNull(manager.getDefaultWriter());

        manager.start();

        LogWriter defaultWriter = manager.getDefaultWriter();
        assertNotNull(defaultWriter);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getLogWriter(java.lang.String)}.
     */
    @Test
    void testGetLogWriterWithMissingDefaultWriter() {
        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        // throws exception when getDefaultWriter returns null
        assertThrows(IllegalStateException.class, () -> manager.getLogWriter(filename1));
    }

    @Test
    void testGetLogWriterThatDoesNotExistYet() {
        manager.start();

        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        LogWriter logWriter = manager.getLogWriter(filename1);
        assertNotNull(logWriter);
        assertEquals(filename1, logWriter.getFileName());
        assertEquals("/logs/logwriter1.log", logWriter.getAppenderName());
    }

    @Test
    void testGetLogWriterThatExists() throws ConfigurationException {
        manager.start();

        String pid = String.format("%s~logwriter1", LogConstants.FACTORY_PID_CONFIGS);
        String filename1 = manager.getAbsoluteFilePath("logs/logwriter1.log");
        manager.updateLogWriter(pid, new Hashtable<>(Map.of(LogConstants.LOG_FILE, filename1)), false);

        LogWriter logWriter = manager.getLogWriter(filename1);
        assertNotNull(logWriter);
        assertEquals(filename1, logWriter.getFileName());
        assertEquals("/logs/logwriter1.log", logWriter.getAppenderName());
    }

    @SuppressWarnings("java:S2699")
    @Test
    void testConfigResetRequestHandling() throws Exception {
        manager.start();

        // mock firing of event
        EventAdmin eventAdmin = context.getService(EventAdmin.class);
        Event mockEvent = Mockito.mock(Event.class);
        Mockito.doReturn(LogConstants.RESET_EVENT_TOPIC).when(mockEvent).getTopic();

        // event delivery should trigger a reset
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            eventAdmin.sendEvent(mockEvent);
            return null;
        });
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getRootDir()}.
     */
    @Test
    void testGetRootDir() {
        assertEquals(new File("target").getAbsolutePath(), manager.getRootDir());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getAppenderTracker()}.
     */
    @Test
    void testGetAppenderTracker() {
        assertNull(manager.getAppenderTracker());

        manager.start();

        assertNotNull(manager.getAppenderTracker());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getConfigSourceTracker()}.
     */
    @Test
    void testGetConfigSourceTracker() {
        assertNull(manager.getConfigSourceTracker());

        manager.start();

        assertNotNull(manager.getConfigSourceTracker());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getDefaultConfigURL()}.
     */
    @Test
    void testGetDefaultConfigURL() throws IOException {
        URL defaultConfigURL = manager.getDefaultConfigURL();
        assertNotNull(defaultConfigURL);
        // make sure it is readable
        try (InputStream inStream = defaultConfigURL.openStream()) {
            assertNotNull(inStream);
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getLogbackConfigFile()}.
     */
    @Test
    void testGetLogbackConfigFile() {
        assertNull(manager.getLogbackConfigFile());

        String logbackFilePath = new File("src/test/resources/logback-test2.xml").getAbsolutePath();
        try {
            System.setProperty(LogConstants.LOGBACK_FILE, logbackFilePath);

            manager.start();
        } finally {
            System.clearProperty(LogConstants.LOGBACK_FILE);
        }

        assertEquals(new File(logbackFilePath), manager.getLogbackConfigFile());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#getLogConfigs()}.
     */
    @Test
    void testGetLogConfigs() {
        Iterable<LogConfig> logConfigs = manager.getLogConfigs();
        assertNotNull(logConfigs);
        assertFalse(logConfigs.iterator().hasNext());

        manager.start();

        logConfigs = manager.getLogConfigs();
        assertNotNull(logConfigs);
        assertTrue(logConfigs.iterator().hasNext());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#determineLoggerState()}.
     */
    @Test
    void testDetermineLoggerState() throws Exception {
        // before start
        LoggerStateContext loggerState = manager.determineLoggerState();
        assertNotNull(loggerState);

        manager.start();

        // after start (still using console appender)
        loggerState = manager.determineLoggerState();
        assertNotNull(loggerState);
        org.slf4j.Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertTrue(loggerState.nonOSgiConfiguredLoggers.contains(rootLogger));

        // define main log file instead of console
        Dictionary<String, String> config = new Hashtable<>(Map.of(
                LogConstants.LOG_LEVEL, "info",
                LogConstants.LOG_FILE, "logs/testDetermineLoggerState.log"));
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            manager.updateGlobalConfiguration(config);
            return null;
        });

        // after update config (now using file appender)
        loggerState = manager.determineLoggerState();
        assertNotNull(loggerState);
        assertFalse(loggerState.nonOSgiConfiguredLoggers.contains(rootLogger));
    }

    @Test
    void testDetermineLoggerStateWithAppenderDefinedElsewhere() {
        manager.start();

        // with some other logger/appender defined outside of LogConfigManager
        Logger logger = (Logger) LoggerFactory.getLogger(getClass());
        logger.setLevel(Level.WARN);
        // appender with null name
        logger.addAppender(new ListAppender<>());
        // other appender with a name but not defined by configurations
        Appender<ILoggingEvent> otherAppender = new ListAppender<>();
        otherAppender.setName("other");
        logger.addAppender(otherAppender);

        LoggerStateContext loggerState = manager.determineLoggerState();
        assertNotNull(loggerState);
        assertTrue(loggerState.nonOSgiConfiguredLoggers.contains(logger));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LogConfigManager#isClassNameVisible(java.lang.String)}.
     */
    @Test
    void testIsClassNameVisible() {
        assertFalse(manager.isClassNameVisible("not.exsting"));
        assertTrue(manager.isClassNameVisible(LogWriter.class.getName()));
    }
}
