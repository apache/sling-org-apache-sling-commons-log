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
package org.apache.sling.commons.log.logback.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.Status;

/**
 * Utilities to assist in testing
 */
public class TestUtils {

    /**
     * Checks if the text is present in the file
     * 
     * @param file the file to check
     * @param expected the text to look for
     * @return true if the text was found, false otherwise
     */
    public static boolean containsString(File file, String expected) throws IOException {
        try (Stream<String> stream = Files.lines(file.toPath())) {
            return stream.anyMatch(line -> line.contains(expected));
        }
    }

    /**
     * Returns the contents of the input source as a string
     * 
     * @param inputSource the input source to read
     * @return the contents as a string
     */
    public static String contentsAsString(InputSource inputSource) throws IOException {
        try (Reader reader = inputSource.getCharacterStream();
                StringWriter writer = new StringWriter()) {
            char[] buffer = new char[1024];
            for (int length; (length = reader.read(buffer)) != -1; ) {
                writer.write(buffer, 0, length);
            }
            return writer.toString();
        }
    }

    public static Object doWaitForAsyncResetAfterWork(LoggerContext loggerContext, Callable<?> work) throws Exception {
        loggerContext.getStatusManager().clear();
        Object value = work.call();
        waitForAsyncReset(loggerContext);
        return value;
    }
    public static void waitForAsyncReset(LoggerContext loggerContext) {
        // wait for the async reset to complete
        Awaitility.await("wait for async reset")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> {
                return loggerContext
                        .getStatusManager()
                        .getCopyOfStatusList()
                        .stream()
                        .anyMatch(s -> s.getMessage().equals("Re configuration done"));
            });

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext
                .getStatusManager()
                .getCopyOfStatusList();
        // the last status should be the re-configuration done mst
        assertEquals("Re configuration done", 
                copyOfStatusList.get(copyOfStatusList.size() - 1).getMessage());
    }

    public static String doWorkWithCapturedStdErr(Runnable work) {
        // Save the old System.err
        PrintStream old = System.err;
        try {
            // redirect the System.err output to a custom PrintStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            System.setErr(ps);

            work.run();

            return baos.toString();
        } finally {
            // Restore the old System.err
            System.setErr(old);
        }
    }
    public static String doWorkWithCapturedStdOut(Runnable work) {
        // Save the old System.out
        PrintStream old = System.out;
        try {
            // redirect the System.out output to a custom PrintStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            System.setOut(ps);

            work.run();

            return baos.toString();
        } finally {
            // Restore the old System.out
            System.setOut(old);
        }
    }

    /**
     * Do some work where the console output should be ignored, usually
     * because of an expected exception that you don't need cluttering the output of
     * a test run
     * 
     * @param <T> the return type of the worker
     * @param work the worker to do the work
     * @return the value returned by the worker
     */
    public static <T> T doWorkWithoutRootConsoleAppender(Callable<T> work) throws Exception {
        Logger rootLogger = ((Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME));
        Iterator<Appender<ILoggingEvent>> iteratorForAppenders = rootLogger.iteratorForAppenders();
        List<Appender<ILoggingEvent>> consoleAppenders = new ArrayList<>();
        try {
            while (iteratorForAppenders.hasNext()) {
                Appender<ILoggingEvent> appender = iteratorForAppenders.next();
                if (appender instanceof ConsoleAppender) {
                    consoleAppenders.add(appender);
                    rootLogger.detachAppender(appender);
                }
            }

            return work.call();
        } finally {
            for (Appender<ILoggingEvent> appender : consoleAppenders) {
                rootLogger.addAppender(appender);
            }
        }
    }

}
