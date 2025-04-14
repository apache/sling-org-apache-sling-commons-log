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

import ch.qos.logback.classic.spi.StackTraceElementProxy;
import org.apache.sling.commons.log.logback.internal.stacktrace.OSGiAwareExceptionHandling.OSGiAwareConverter;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class OSGiAwareConverterTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private PackageInfoCollector collector;

    @BeforeEach
    protected void beforeEach() {
        collector = new PackageInfoCollector();
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.OSGiAwareExceptionHandling.OSGiAwareConverter#extraData(java.lang.StringBuilder, ch.qos.logback.classic.spi.StackTraceElementProxy)}.
     */
    @Test
    void testExtraData() {
        Bundle bundle = context.bundleContext().getBundle();
        String className = getClass().getName();
        collector.add(bundle, className);

        OSGiAwareConverter converter = new OSGiAwareConverter(collector);

        StringBuilder builder = new StringBuilder();
        StackTraceElement ste = new StackTraceElement(className, "methodName", "fileName", 1);
        StackTraceElementProxy step = new StackTraceElementProxy(ste);
        converter.extraData(builder, step);
        assertEquals(" [mock-bundle:0.0.0]", builder.toString());
    }

    @Test
    void testExtraDataWithNullStep() {
        OSGiAwareConverter converter = new OSGiAwareConverter(collector);

        StringBuilder builder = new StringBuilder();
        converter.extraData(builder, null);
        assertEquals("", builder.toString());
    }

    @Test
    void testExtraDataWithNullBundleInfo() {
        OSGiAwareConverter converter = new OSGiAwareConverter(collector);

        StringBuilder builder = new StringBuilder();
        String className = getClass().getName();
        StackTraceElement ste = new StackTraceElement(className, "methodName", "fileName", 1);
        StackTraceElementProxy step = new StackTraceElementProxy(ste);
        converter.extraData(builder, step);
        assertEquals("", builder.toString());
    }
}
