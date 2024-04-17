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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.WarnStatus;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class SlingConfigurationPrinterTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();
    private BundleContext bundleContext;
    private LoggerContext loggerContext;

    private LogConfigManager logConfigManager;
    private SlingConfigurationPrinter slingConfigPrinter;

    @BeforeEach
    protected void beforeEach() {
        bundleContext = context.bundleContext();
        loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();

        try {
            System.setProperty(LogConstants.SLING_HOME, new File("target").getAbsolutePath());
            System.setProperty(LogConstants.LOG_FILE, "logs/hello.log");

            logConfigManager = new LogConfigManager(bundleContext);
            logConfigManager.start();
        } finally {
            System.clearProperty(LogConstants.LOG_FILE);
            System.clearProperty(LogConstants.SLING_HOME);
        }

        slingConfigPrinter = new SlingConfigurationPrinter(logConfigManager);
    }

    @AfterEach
    protected void afterEach() {
        logConfigManager.stop();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingConfigurationPrinter#printConfiguration(java.io.PrintWriter, java.lang.String)}.
     */
    @Test
    void testPrintConfiguration() throws IOException {
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            slingConfigPrinter.printConfiguration(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            String logFilePath = new File("target", "logs/hello.log").getAbsolutePath();
            assertTrue(output.contains(String.format("Log file %s", logFilePath)));
            assertTrue(output.contains("1. hello.log"));
        }
    }
    @Test
    void testPrintConfigurationWithGzipMode() throws IOException {
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            slingConfigPrinter.printConfiguration(pw, SlingConfigurationPrinter.MODE_ZIP);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            String logFilePath = new File("target", "logs/hello.log").getAbsolutePath();
            assertFalse(output.contains(String.format("Log file %s", logFilePath)));
        }
    }
    @Test
    void testPrintConfigurationWithConsoleAppender() throws Exception {
        Hashtable<String, String> config = new Hashtable<>();

        // clone the current bundle config
        Dictionary<String, String> bundleConfiguration = logConfigManager.getBundleConfiguration(bundleContext);
        config.putAll(Collections.list(bundleConfiguration.keys()).stream()
            .collect(Collectors.toMap(Function.identity(), bundleConfiguration::get)));

        // add custom config
        config.put(LogConstants.LOG_FILE, LogConstants.FILE_NAME_CONSOLE);

        // apply the new conig and wait for reset
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            logConfigManager.updateGlobalConfiguration(config);
            return null;
        });

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            slingConfigPrinter.printConfiguration(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertFalse(output.contains("Log file "));
        }
    }

    @Test
    void testPrintConfigurationWithLogFileoutsideRootDir() throws Exception {
        Path tempFile = Files.createTempFile("testPrintConfigurationWithLogFileoutsideRootDir", ".log");

        Hashtable<String, String> config = new Hashtable<>();

        // clone the current bundle config
        Dictionary<String, String> bundleConfiguration = logConfigManager.getBundleConfiguration(bundleContext);
        config.putAll(Collections.list(bundleConfiguration.keys()).stream()
            .collect(Collectors.toMap(Function.identity(), bundleConfiguration::get)));

        // add custom config
        config.put(LogConstants.LOG_FILE, tempFile.toString());

        // apply the new conig and wait for reset
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            logConfigManager.updateGlobalConfiguration(config);
            return null;
        });

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            slingConfigPrinter.printConfiguration(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            assertTrue(output.contains(String.format("Log file %s", tempFile.toString())));
            assertTrue(output.contains(String.format("1. %s", tempFile.toString())));
        } finally {
            Files.delete(tempFile);
        }
    }

    @Test
    void testPrintConfigurationWithLogFileThatDoesNotExist() throws IOException {
        // delete the log file
        assertTrue(new File("target", "logs/hello.log").delete());

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            slingConfigPrinter.printConfiguration(pw, null);
            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            String logFilePath = new File("target", "logs/hello.log").getAbsolutePath();
            assertFalse(output.contains(String.format("Log file %s", logFilePath)));
        }
    }
    @Test
    void testPrintConfigurationWithCaughtIOExceptionWhileTailing() throws IOException {
        // make the file not readable
        File file = new File("target", "logs/hello.log");

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            file.setReadable(false);

            String stderr = TestUtils.doWorkWithCapturedStdErr(() -> slingConfigPrinter.printConfiguration(pw, null));
            assertTrue(stderr.contains("Error occurred while processing log file"));

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
        } finally {
            // restore the original state
            file.setReadable(true);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 10})
    void testPrintConfigurationWithNumOfLines(int numOfLines) throws Exception {
        Hashtable<String, String> config = new Hashtable<>();

        // clone the current bundle config
        Dictionary<String, String> bundleConfiguration = logConfigManager.getBundleConfiguration(bundleContext);
        config.putAll(Collections.list(bundleConfiguration.keys()).stream()
            .collect(Collectors.toMap(Function.identity(), bundleConfiguration::get)));

        // add the numOfLines config
        config.put(LogConstants.PRINTER_NUM_OF_LINES, Integer.toString(numOfLines));
        config.put(LogConstants.LOG_FILE, "logs/hello.log");

        // apply the new conig and wait for reset
        TestUtils.doWaitForAsyncResetAfterWork(loggerContext, () -> {
            logConfigManager.updateGlobalConfiguration(config);
            return null;
        });

        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            slingConfigPrinter.printConfiguration(pw, null);

            String output = strWriter.toString();
            assertFalse(output.isEmpty());
            String logFilePath = new File("target", "logs/hello.log").getAbsolutePath();
            assertTrue(output.contains(String.format("Log file %s", logFilePath)));
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingConfigurationPrinter#includeWholeFile(java.io.PrintWriter, java.io.File)}.
     */
    @Test
    void testIncludeWholeFile() throws IOException {
        Path tempFile = Files.createTempFile("includeWholeFile", ".log");
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                for (int i = 1; i < 25; i++) {
                    writer.append("log message ").append(Integer.toString(i)).append("\r\n");
                }
                writer.append("\r\n");
            }

            SlingConfigurationPrinter.includeWholeFile(pw, tempFile.toFile());

            String output = strWriter.toString();
            assertTrue(output.contains("log message 1"));
            assertTrue(output.contains("log message 24"));
        } finally {
            Files.delete(tempFile);
        }
    }
    @Test
    void testIncludeWholeFileThatDoesNotExist() throws IOException {
        try (StringWriter strWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(strWriter)) {
            SlingConfigurationPrinter.includeWholeFile(pw, new File("notexisting.log"));

            String output = strWriter.toString();
            assertTrue(output.isEmpty());
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingConfigurationPrinter#getAttachments(java.lang.String)}.
     */
    @Test
    void testGetAttachments() {
        assertNull(slingConfigPrinter.getAttachments(null));

        URL[] attachments = slingConfigPrinter.getAttachments("zip");
        assertNotNull(attachments);
    }
    @Test
    void testGetAttachmentsWithNoFileAppenders() throws Exception {
        TestUtils.doWaitForAsyncResetAfterWork((LoggerContext)LoggerFactory.getILoggerFactory(), () -> {
            logConfigManager.updateGlobalConfiguration(new Hashtable<>(Map.of(
                    LogConstants.LOG_FILE, LogConstants.FILE_NAME_CONSOLE
                    )));
            return null;
        });

        assertNull(slingConfigPrinter.getAttachments("zip"));
    }

    @Test
    void testGetRotatedFilesForNotRollingFileAppender() {
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setFile("target/logs/testGetRotatedFiles.log");
        File[] rotatedFiles = slingConfigPrinter.getRotatedFiles(appender, -1);
        assertNotNull(rotatedFiles);
        assertEquals(1, rotatedFiles.length);
        assertEquals("target/logs/testGetRotatedFiles.log", rotatedFiles[0].getPath());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 2})
    void testGetRotatedFilesForRollingFileAppender(int maxOldFileCount) throws IOException, ParseException {
        Path tempDir = Files.createTempDirectory("testGetRotatedFilesForRollingFileAppender");
        try {
            Path logFile = Files.createFile(tempDir.resolve("testGetRotatedFilesForRollingFileAppender.log"));

            Path logFile1 = Files.createFile(tempDir.resolve("testGetRotatedFilesForRollingFileAppender.log.2023-11-01"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            logFile1.toFile().setLastModified(dateFormat.parse("2023-11-01").getTime());
            Path logFile2 = Files.createFile(tempDir.resolve("testGetRotatedFilesForRollingFileAppender.log.2023-11-03"));
            logFile2.toFile().setLastModified(dateFormat.parse("2023-11-03").getTime());
            Path logFile3 = Files.createFile(tempDir.resolve("testGetRotatedFilesForRollingFileAppender.log.2023-11-02"));
            logFile3.toFile().setLastModified(dateFormat.parse("2023-11-02").getTime());

            RollingFileAppender<ILoggingEvent> rollingAppender = new RollingFileAppender<>();
            rollingAppender.setFile(logFile.toString());

            TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
            String fileNamePattern = logFile.toString() + ".%d{yyyy-MM-dd}";
            policy.setFileNamePattern(fileNamePattern);
            policy.setMaxHistory(20);
            policy.setContext(loggerContext);
            policy.setParent(rollingAppender);
            rollingAppender.setTriggeringPolicy(policy);

            File[] rotatedFiles = slingConfigPrinter.getRotatedFiles(rollingAppender, maxOldFileCount);
            assertNotNull(rotatedFiles);
            assertEquals(logFile.toString(), rotatedFiles[0].getPath());
            if (maxOldFileCount == -1) {
                assertEquals(4, rotatedFiles.length);
                assertEquals(logFile2.toString(), rotatedFiles[1].getPath());
                assertEquals(logFile3.toString(), rotatedFiles[2].getPath());
                assertEquals(logFile1.toString(), rotatedFiles[3].getPath());
            } else {
                assertEquals(3, rotatedFiles.length);
                assertEquals(logFile2.toString(), rotatedFiles[1].getPath());
                assertEquals(logFile3.toString(), rotatedFiles[2].getPath());
            }
        } finally {
            // clean up the temp files
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    void testMaybeAddToUrlsList() throws URISyntaxException {
        List<URL> urlsList = new ArrayList<>();
        File f = Mockito.mock(File.class);
        URI mockURI = new URI("invalid:invalid");
        Mockito.doReturn(mockURI).when(f).toURI();
        slingConfigPrinter.maybeAddToUrlsList(urlsList, f);
        assertTrue(urlsList.isEmpty());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.SlingConfigurationPrinter#abbreviatedOrigin(ch.qos.logback.core.status.Status)}.
     */
    @Test
    void testAbbreviatedOrigin() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        Status s = new WarnStatus("msg", null);
        assertNull(SlingConfigurationPrinter.abbreviatedOrigin(s));

        s = new WarnStatus("msg", context);
        assertEquals("OsgiContext", SlingConfigurationPrinter.abbreviatedOrigin(s));

        s = new WarnStatus("msg", Class.forName("SimpleOrigin").getDeclaredConstructor().newInstance());
        assertEquals("SimpleOrigin", SlingConfigurationPrinter.abbreviatedOrigin(s));
    }

    @Test
    void testStatusLevelAsString() {
        assertEquals("INFO", slingConfigPrinter.statusLevelAsString(new InfoStatus("", null)));
        assertEquals("WARN", slingConfigPrinter.statusLevelAsString(new WarnStatus("", null)));
        assertEquals("ERROR", slingConfigPrinter.statusLevelAsString(new ErrorStatus("", null)));
        Status mockStatus = Mockito.mock(Status.class);
        Mockito.doReturn(-1).when(mockStatus).getEffectiveLevel();
        assertNull(slingConfigPrinter.statusLevelAsString(mockStatus));
    }

    @Test
    void testHumanReadableByteCount() {
        assertEquals("0", slingConfigPrinter.humanReadableByteCount(-1));

        assertEquals("999 B", slingConfigPrinter.humanReadableByteCount(999));
        assertEquals("1.1 kB", slingConfigPrinter.humanReadableByteCount(1101));
        assertEquals("2.1 MB", slingConfigPrinter.humanReadableByteCount(2050000));
    }

    @Test
    void testGetModifiedDate() {
        // not existing file should return UNKNOWN
        File file = new File("test.log");
        assertEquals("UNKNOWN", slingConfigPrinter.getModifiedDate(file));

        // existing file should return a formatted datetime
        file = new File("target", "logs/hello.log");
        assertNotEquals("UNKNOWN", slingConfigPrinter.getModifiedDate(file));
    }
}
