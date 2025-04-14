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
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class GlobalConfiguratorTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private GlobalConfigurator configurator = new GlobalConfigurator();
    private LogConfigManager mgr;

    private TestInfo testInfo;

    private File logFile;

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
        configurator.setLogConfigManager(mgr);

        logFile = new File(String.format(
                "target/logs/%s/%s.log",
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName()));
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    @AfterEach
    protected void afterEach() {
        mgr.stop();

        LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        // set this back to the default value
        logbackContext.setPackagingDataEnabled(false);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.GlobalConfigurator#updated(java.util.Dictionary)}.
     */
    @Test
    void testUpdated() throws Exception {
        String fileVal = String.format(
                "logs/%s/%s.log",
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName());
        String fileSizeVal = String.format("%s.%%d{yyyy-MM-dd}.gz", fileVal);

        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                LogConstants.LOG_PACKAGING_DATA,
                true,
                LogConstants.LOG_PATTERN,
                "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %msg%n",
                LogConstants.LOG_LEVEL,
                "info",
                LogConstants.LOG_FILE,
                fileVal,
                LogConstants.LOG_FILE_NUMBER,
                7,
                LogConstants.LOG_FILE_SIZE,
                fileSizeVal));
        LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        TestUtils.doWaitForAsyncResetAfterWork(logbackContext, () -> {
            configurator.updated(config);
            return null;
        });

        assertFalse(logbackContext.isPackagingDataEnabled());
        assertTrue(mgr.isPackagingDataEnabled());

        Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        ch.qos.logback.classic.Logger logbackRootLogger = (ch.qos.logback.classic.Logger) rootLogger;
        OutputStreamAppender<ILoggingEvent> appender = (OutputStreamAppender<ILoggingEvent>)
                logbackRootLogger.getAppender(
                        "/logs/org.apache.sling.commons.log.logback.internal.config.GlobalConfiguratorTest/testUpdated.log");
        assertTrue(appender instanceof SlingRollingFileAppender);
        String expectedPath = Paths.get(logFile.getPath()).toAbsolutePath().toString();
        assertEquals(expectedPath, ((SlingRollingFileAppender<ILoggingEvent>) appender).getFile());

        Logger testLogger = LoggerFactory.getLogger(getClass());
        assertEquals(Level.INFO, ((ch.qos.logback.classic.Logger) testLogger).getEffectiveLevel());
        testLogger.info("Test Logging");
        assertTrue(logFile.exists());

        assertTrue(TestUtils.containsString(
                logFile, "org.apache.sling.commons.log.logback.internal.config.GlobalConfiguratorTest Test Logging"));
    }

    @Test
    void testUpdatedWithConfigurationException() throws Exception {
        Dictionary<String, ?> config = new Hashtable<>(Map.of(LogConstants.LOG_LEVEL, "invalid"));
        assertThrows(org.osgi.service.cm.ConfigurationException.class, () -> configurator.updated(config));
    }
}
