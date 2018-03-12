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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.felix.framework.util.ImmutableList;
import org.apache.sling.commons.log.logback.internal.LogConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoggerSpecificEncoderTest {

    private LoggerSpecificEncoder tested;

    @Mock
    private ILoggingEvent mockTestEvent;

    @Before
    public void setUp() {
        tested = new LoggerSpecificEncoder(new PrefixTestLayout("DEFAULT:"));
        when(mockTestEvent.getMessage()).thenReturn("test message");
        when(mockTestEvent.getLoggerName()).thenReturn("org.apache.sling.testing.FooBar");
    }

    @Test
    public void testShouldReturnWithDefaultLayoutForNoConfigs() {
        assertThat(tested.encode(mockTestEvent), is(equalTo("DEFAULT:test message".getBytes())));
    }

    @Test
    public void testShouldIgnoreNonmatchingLayoutCategories() {
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(ImmutableList.newInstance("org.apache.commons", "com.initech.sling")));
        when(logConfigMock.createLayout()).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("DEFAULT:test message".getBytes())));
    }

    @Test
    public void testShouldIgnorePartialMatchingPackageName() {
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(ImmutableList.newInstance("org.apache.sling.test")));
        when(logConfigMock.createLayout()).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("DEFAULT:test message".getBytes())));
    }

    @Test
    public void testShouldUseExactMatchingCategoryLayout() {
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(ImmutableList.newInstance("org.apache.sling.testing.FooBar")));
        when(logConfigMock.createLayout()).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("INITECH:test message".getBytes())));
    }

    @Test
    public void testShouldUseInheritedCategoryLayout() {
        LogConfig logConfigMock = mock(LogConfig.class);
        when(logConfigMock.getCategories()).thenReturn(new HashSet<>(ImmutableList.newInstance("org.apache")));
        when(logConfigMock.createLayout()).thenReturn(new PrefixTestLayout("INITECH:"));
        tested.addLogConfig(logConfigMock);

        assertThat(tested.encode(mockTestEvent), is(equalTo("INITECH:test message".getBytes())));
    }

    /**
     * Simple partial implementation of {@link PatternLayout} that redirects all method calls that are not explicitly extended to an
     * underlying mock (available as a protected {@link PrefixTestLayout#wrapped} field).
     */
    private static class PrefixTestLayout extends AbstractPatternLayoutWrapper {

        private final String prefix;

        private PrefixTestLayout(String prefix) {
            super(mock(PatternLayout.class));
            this.prefix = prefix;
        }

        @Override
        public String doLayout(ILoggingEvent event) {
            return prefix + event.getMessage();
        }
    }
}