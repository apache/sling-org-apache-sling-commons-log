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
package org.apache.sling.commons.log.logback.internal.util;

import java.io.PrintStream;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom StatusPrinter similar to Logback StatusPrinter to account for changes required
 * for SLING-3410
 */
public class SlingStatusPrinter {

    private SlingStatusPrinter() {
        // to hide public ctor
    }

    /**
     * Based on StatusPrinter. printInCaseOfErrorsOrWarnings. This has been adapted
     * to print more context i.e. some message from before the error message to better understand
     * the failure scenario
     *
     * @param context     the logback context
     * @param threshold   time since which the message have to be checked for errors/warnings
     * @param msgSince    time from which we are interested in the message logs
     * @param initSuccess flag indicating if Logback configuration failed or not
     */
    public static void printInCaseOfErrorsOrWarnings(
            @Nullable Context context, long threshold, long msgSince, boolean initSuccess) {
        if (context == null) {
            throw new IllegalArgumentException("Context argument cannot be null");
        }
        PrintStream ps = System.out; // NOSONAR
        StatusManager sm = context.getStatusManager();
        if (sm == null) {
            ps.println("WARN: Context named \"" + context.getName() + "\" has no status manager");
        } else {
            StatusUtil statusUtil = new StatusUtil(context);
            if (statusUtil.getHighestLevel(threshold) >= Status.WARN) {
                List<Status> filteredList =
                        StatusUtil.filterStatusListByTimeThreshold(sm.getCopyOfStatusList(), msgSince);
                print(filteredList, initSuccess);
            }
        }
    }

    private static void print(@NotNull List<Status> statusList, boolean initSuccess) {
        StringBuilder sb = new StringBuilder();

        if (initSuccess) {
            sb.append("While (re)configuring Logback transient issues were observed. "
                    + "More details are provided below.");
            sb.append(CoreConstants.LINE_SEPARATOR);
        }

        String prefix = "";
        if (initSuccess) {
            prefix = "*Logback Status* ";
        }

        StatusPrinter2 statusPrinter2 = new StatusPrinter2();
        for (Status s : statusList) {
            statusPrinter2.buildStr(sb, prefix, s);
        }

        // In case logging system completely fails then log the message in System out
        // otherwise make it part of 'normal' logs
        String output = sb.toString();
        if (!initSuccess) {
            System.out.println(output); // NOSONAR
        } else {
            Logger logger = LoggerFactory.getLogger(SlingStatusPrinter.class);
            logger.info(output);
        }
    }
}
