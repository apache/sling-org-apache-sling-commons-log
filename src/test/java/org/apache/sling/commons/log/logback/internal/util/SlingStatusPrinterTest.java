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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.commons.log.helpers.LogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.WarnStatus;

/**
 *
 */
class SlingStatusPrinterTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.SlingStatusPrinter#printInCaseOfErrorsOrWarnings(ch.qos.logback.core.Context, long, long, boolean)}.
     */
    @Test
    void testPrintInCaseOfErrorsOrWarningsWithNullContext() {
        LoggerContext context = null;
        long threshold = Long.MIN_VALUE;
        long msgSince = System.currentTimeMillis();
        boolean initSuccess = true;
        assertThrows(IllegalArgumentException.class, () -> SlingStatusPrinter.printInCaseOfErrorsOrWarnings(context, threshold, 
                        msgSince, initSuccess));
    }

    @Test
    void testPrintInCaseOfErrorsOrWarningsWithNullStatusMangager() {
        LoggerContext context = Mockito.mock(LoggerContext.class);
        Mockito.doReturn(null).when(context).getStatusManager();
        long threshold = Long.MIN_VALUE;
        long msgSince = System.currentTimeMillis();
        boolean initSuccess = true;
        String output = TestUtils.doWorkWithCapturedStdOut(
                () -> SlingStatusPrinter.printInCaseOfErrorsOrWarnings(context, threshold, 
                        msgSince, initSuccess));
        assertEquals("WARN: Context named \"null\" has no status manager\n", output);
    }

    @Test
    void testPrintInCaseOfErrorsOrWarningsWithNoApplicableStatus() {
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        context.getStatusManager().clear();
        long threshold = Long.MAX_VALUE;
        long msgSince = System.currentTimeMillis();
        boolean initSuccess = true;
        String output = TestUtils.doWorkWithCapturedStdOut(
                () -> SlingStatusPrinter.printInCaseOfErrorsOrWarnings(context, threshold,
                        msgSince, initSuccess));
        assertTrue(output.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testPrintInCaseOfErrorsOrWarningsWithApplicableStatus(boolean initSuccess) throws Exception {
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        context.getStatusManager().add(new WarnStatus("Something went wrong", context));
        long threshold = Long.MIN_VALUE;
        long msgSince = System.currentTimeMillis();
        String output;
        try (LogCapture capture = new LogCapture(SlingStatusPrinter.class.getName(), true)) {
            output = TestUtils.doWorkWithCapturedStdOut(
                    () -> SlingStatusPrinter.printInCaseOfErrorsOrWarnings(context, threshold,
                            msgSince, initSuccess));
            assertFalse(output.isEmpty());

            if (initSuccess) {
                assertTrue(output.contains("While (re)configuring Logback transient issues were observed. More details are provided below."));
                assertTrue(output.contains(String.format("*Logback Status* |-WARN in ch.qos.logback.classic.LoggerContext[%s] - Something went wrong", context.getName())));

                // verify the msg was logged
                capture.assertContains(Level.INFO, "While (re)configuring Logback transient issues were observed. More details are provided below.");
                capture.assertContains(Level.INFO, String.format("*Logback Status* |-WARN in ch.qos.logback.classic.LoggerContext[%s] - Something went wrong", context.getName()));
            } else {
                assertTrue(output.contains(String.format("|-WARN in ch.qos.logback.classic.LoggerContext[%s] - Something went wrong", context.getName())));

                // verify the msg was not logged
                capture.assertNotContains(Level.INFO, "While (re)configuring Logback transient issues were observed. More details are provided below.");
                capture.assertNotContains(Level.INFO, String.format("*Logback Status* |-WARN in ch.qos.logback.classic.LoggerContext[%s] - Something went wrong", context.getName()));
            }
        }
    }

}
