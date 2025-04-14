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
package org.apache.sling.commons.log.logback.internal.stacktrace;

import ch.qos.logback.classic.pattern.EnsureExceptionHandling;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil;

/**
 * Extend EnsureExceptionHandling to add extra OSGi bundle info to the lines
 * in the stack trace output
 */
public class OSGiAwareExceptionHandling extends EnsureExceptionHandling {
    private final PackageInfoCollector collector;

    /**
     * Constructor
     *
     * @param collector the package information collecgtor
     */
    public OSGiAwareExceptionHandling(PackageInfoCollector collector) {
        this.collector = collector;
    }

    @Override
    public void process(Context context, Converter<ILoggingEvent> head) {
        if (head == null) {
            // this should never happen
            throw new IllegalArgumentException("cannot process empty chain");
        }
        if (!chainHandlesThrowable(head)) {
            Converter<ILoggingEvent> tail = ConverterUtil.findTail(head);
            Converter<ILoggingEvent> exConverter = new OSGiAwareConverter(collector);
            tail.setNext(exConverter);
        }
    }

    @SuppressWarnings("java:S110")
    static class OSGiAwareConverter extends MaskingMessageUtil.MaskingExtendedThrowableProxyConverter {
        private final PackageInfoCollector collector;

        public OSGiAwareConverter(PackageInfoCollector collector) {
            this.collector = collector;
        }

        @Override
        protected void extraData(StringBuilder builder, StackTraceElementProxy step) {
            if (step != null) {
                String bundleInfo =
                        collector.getBundleInfo(step.getStackTraceElement().getClassName());
                if (bundleInfo != null) {
                    builder.append(" [").append(bundleInfo).append(']');
                }
            }
        }
    }
}
