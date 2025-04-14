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

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.status.Status;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class LoggerManagedServiceFactoryTest {
    private static final String TEST_OVERRIDE = "TEST_OVERRIDE";

    protected final OsgiContext context = new OsgiContext();

    private LogConfigManager mgr;
    private LoggerManagedServiceFactory factory;

    private TestInfo testInfo;

    private File logFile;

    private String pid;

    @BeforeEach
    protected void beforeEach(TestInfo testInfo) {
        this.testInfo = testInfo;

        try {
            System.setProperty(LogConstants.SLING_HOME, new File("target").getPath());

            mgr = new LogConfigManager(context.bundleContext());
        } finally {
            // cleanup to not interfere with other tests
            System.clearProperty(LogConstants.SLING_HOME);
        }

        mgr.start();

        factory = new LoggerManagedServiceFactory();
        factory.setLogConfigManager(mgr);

        logFile = new File(String.format(
                "target/logs/%s/%s.log",
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName()));
        if (logFile.exists()) {
            logFile.delete();
        }

        pid = String.format(
                "%s~%s/%s.log",
                LogConstants.FACTORY_PID_CONFIGS,
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName());
    }

    @AfterEach
    protected void afterEach() {
        if (mgr != null) {
            mgr.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LoggerManagedServiceFactory#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals("Logger configurator", factory.getName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LoggerManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)}.
     */
    @Test
    void testUpdatedWithCalculatedFileName() throws Exception {
        Dictionary<String, ?> config = new Hashtable<>(
                Map.of(LogConstants.LOG_LOGGERS, new String[] {"log.testing"}, LogConstants.LOG_LEVEL, "info"));
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            factory.updated(pid, config);
            return null;
        });

        // test logging to the logger
        LoggerFactory.getLogger("log.testing").info("Testing logging to the logger");

        // the custom appender should now be attached to the logger
        Logger accessLogger = (Logger) LoggerFactory.getLogger("log.testing");
        String expectedAppenderName = "/" + LoggerManagedServiceFactory.LOG_FILE_DEFAULT;
        Appender<ILoggingEvent> appender = accessLogger.getAppender(expectedAppenderName);
        assertNotNull(appender);
        assertEquals(expectedAppenderName, appender.getName());
        assertEquals(Level.INFO, accessLogger.getLevel());

        logFile = new File("target", LoggerManagedServiceFactory.LOG_FILE_DEFAULT);
        assertTrue(TestUtils.containsString(logFile, "Testing logging to the logger"));
    }

    @Test
    void testUpdatedWithSuppliedFileNames() throws Exception {
        String fileVal = String.format(
                "logs/%s/%s.log",
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName());
        String fileSizeVal = String.format("%s.%%d{yyyy-MM-dd}.gz", fileVal);
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_FILE,
                fileVal,
                LogConstants.LOG_FILE_SIZE,
                fileSizeVal,
                LogConstants.LOG_PATTERN,
                "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* %msg%n",
                LogConstants.LOG_LOGGERS,
                new String[] {"log.testing"},
                LogConstants.LOG_LEVEL,
                "info"));
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            factory.updated(pid, config);
            return null;
        });

        // test logging to the logger
        LoggerFactory.getLogger("log.testing").info("Testing logging to the logger");

        // the custom appender should now be attached to the logger
        Logger accessLogger = (Logger) LoggerFactory.getLogger("log.testing");
        String expectedAppenderName =
                "/logs/org.apache.sling.commons.log.logback.internal.config.LoggerManagedServiceFactoryTest/testUpdatedWithSuppliedFileNames.log";
        Appender<ILoggingEvent> appender = accessLogger.getAppender(expectedAppenderName);
        assertNotNull(appender);
        assertEquals(expectedAppenderName, appender.getName());
        assertEquals(Level.INFO, accessLogger.getLevel());

        assertTrue(TestUtils.containsString(logFile, "Testing logging to the logger"));
    }

    @Test
    void testUpdatedWithConfigurationException() throws Exception {
        Dictionary<String, ?> config = new Hashtable<>(Map.of());
        assertThrows(ConfigurationException.class, () -> factory.updated(pid, config));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LoggerManagedServiceFactory#deleted(java.lang.String)}.
     */
    @Test
    void testDeleted() throws Exception {
        String fileVal = String.format(
                "logs/%s/%s.log",
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName());
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_FILE,
                fileVal,
                LogConstants.LOG_PATTERN,
                "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* %msg%n",
                LogConstants.LOG_LOGGERS,
                new String[] {"log.testing"},
                LogConstants.LOG_LEVEL,
                "info"));
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            factory.updated(pid, config);
            return null;
        });

        // test logging to the logger
        LoggerFactory.getLogger("log.testing").info("Testing logging to the logger");

        factory.deleted(pid);

        assertTrue(TestUtils.containsString(logFile, "Testing logging to the logger"));
    }

    @Test
    void testDeletedWithConfigurationException() throws Exception {
        mgr = Mockito.spy(mgr);
        Mockito.doThrow(org.apache.sling.commons.log.logback.internal.config.ConfigurationException.class)
                .when(mgr)
                .updateLoggerConfiguration(pid, null, true);
        factory.setLogConfigManager(mgr);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().clear();

        String output = TestUtils.doWorkWithCapturedStdErr(() -> {
            factory.deleted(pid);
        });
        assertTrue(output.contains("Unexpected Configuration Problem"));

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        // the last status should be the error msg
        assertEquals(
                "Unexpected Configuration Problem",
                copyOfStatusList.get(copyOfStatusList.size() - 1).getMessage());
    }

    @Test
    void testFileAddition()
            throws ConfigurationException, org.apache.sling.commons.log.logback.internal.config.ConfigurationException {
        LoggerManagedServiceFactory lmsf = Mockito.spy(LoggerManagedServiceFactory.class);
        LogConfigManager lcm = mock(LogConfigManager.class);
        doReturn(lcm).when(lmsf).getLogConfigManager();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Dictionary<String, String>> effectiveConfigCaptor = ArgumentCaptor.forClass(Dictionary.class);
        lmsf.updated("test", new Hashtable<String, String>());
        verify(lcm).updateLoggerConfiguration(anyString(), effectiveConfigCaptor.capture(), anyBoolean());
        assertEquals("logs/error.log", effectiveConfigCaptor.getValue().get(LogConstants.LOG_FILE));
    }

    @Test
    void testFileNoOverride()
            throws ConfigurationException, org.apache.sling.commons.log.logback.internal.config.ConfigurationException {
        LoggerManagedServiceFactory lmsf = Mockito.spy(LoggerManagedServiceFactory.class);
        LogConfigManager lcm = mock(LogConfigManager.class);
        doReturn(lcm).when(lmsf).getLogConfigManager();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Dictionary<String, String>> effectiveConfigCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(LogConstants.LOG_FILE, TEST_OVERRIDE);
        lmsf.updated("test", dict);
        verify(lcm).updateLoggerConfiguration(anyString(), effectiveConfigCaptor.capture(), anyBoolean());
        assertEquals(TEST_OVERRIDE, effectiveConfigCaptor.getValue().get(LogConstants.LOG_FILE));
    }
}
