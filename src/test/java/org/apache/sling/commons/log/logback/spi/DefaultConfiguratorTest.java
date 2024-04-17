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
package org.apache.sling.commons.log.logback.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator.ExecutionStatus;

/**
 *
 */
class DefaultConfiguratorTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.spi.DefaultConfigurator#configure(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testConfigure() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        DefaultConfigurator defaultConfigurator = new DefaultConfigurator();
        defaultConfigurator.setContext(loggerContext);

        assertEquals(ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY, 
                defaultConfigurator.configure(loggerContext));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.spi.DefaultConfigurator#configure(ch.qos.logback.classic.LoggerContext)}.
     */
    @Test
    void testConfigureWithCaughtException() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        DefaultConfigurator defaultConfigurator = Mockito.mock(DefaultConfigurator.class, CALLS_REAL_METHODS);
        defaultConfigurator.setContext(loggerContext);
        Mockito.doReturn(getClass().getResource("/logback-invalid.txt")).when(defaultConfigurator).getConfigResource();

        String output = TestUtils.doWorkWithCapturedStdErr(() -> {
            assertEquals(ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY,
                    defaultConfigurator.configure(loggerContext));
        });
        assertTrue(output.contains("ch.qos.logback.core.joran.spi.JoranException: Problem parsing XML document. See previously reported errors."));
    }

}
