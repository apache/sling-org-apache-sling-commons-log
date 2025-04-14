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
package org.apache.sling.commons.log.logback.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.sling.commons.log.logback.internal.Tailer.TailerListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class FilteringListenerTest {

    private StringWriter sw;
    private PrintWriter pw;

    @BeforeEach
    protected void beforeEach() {
        sw = new StringWriter();
        pw = new PrintWriter(sw);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.FilteringListener#handle(java.lang.String)}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"*", "line .*", "here"})
    @NullSource
    void testHandleForIncludedLine(String regex) {
        FilteringListener listener = new FilteringListener(pw, regex);

        listener.handle("line here");
        assertTrue(sw.toString().contains("line here"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"liney .*"})
    void testHandleForNotIncludedLine(String regex) {
        FilteringListener listener = new FilteringListener(pw, regex);

        listener.handle("liney here");
        assertFalse(sw.toString().contains("line here"));
    }

    @Test
    void nullPattern() throws Exception {
        TailerListener l = new FilteringListener(pw, null);
        l.handle("foo");
        assertThat(sw.toString(), containsString("foo"));
    }

    @Test
    void allPattern() throws Exception {
        TailerListener l = new FilteringListener(pw, FilteringListener.MATCH_ALL);
        l.handle("foo");
        assertThat(sw.toString(), containsString("foo"));
    }

    @Test
    void basicPattern() throws Exception {
        TailerListener l = new FilteringListener(pw, "foo");
        l.handle("foo");
        assertThat(sw.toString(), containsString("foo"));

        l.handle("bar");
        assertThat(sw.toString(), not(containsString("bar")));

        l.handle("foo bar");
        assertThat(sw.toString(), containsString("foo bar"));
    }

    @Test
    void regexPattern() throws Exception {
        TailerListener l = new FilteringListener(pw, "foo.*bar");
        l.handle("foo");
        assertThat(sw.toString(), not(containsString("foo")));

        l.handle("bar");
        assertThat(sw.toString(), not(containsString("bar")));

        l.handle("foo talks to bar");
        assertThat(sw.toString(), containsString("bar"));
        assertThat(sw.toString(), containsString("talks"));
    }
}
