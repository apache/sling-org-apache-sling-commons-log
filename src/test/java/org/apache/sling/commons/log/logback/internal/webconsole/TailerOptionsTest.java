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
package org.apache.sling.commons.log.logback.internal.webconsole;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.sling.commons.log.logback.webconsole.TailerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 *
 */
class TailerOptionsTest {

    private TailerOptions tailerOptions;

    @BeforeEach
    protected void beforeEach() {
        tailerOptions = new TailerOptions(50, ".*");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.TailerOptions#tailAll()}.
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, 50})
    void testTailAll(int numLines) {
        tailerOptions = new TailerOptions(numLines, ".*");
        assertEquals(numLines < 0, tailerOptions.tailAll());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.TailerOptions#getNumOfLines()}.
     */
    @Test
    void testGetNumOfLines() {
        assertEquals(50, tailerOptions.getNumOfLines());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.webconsole.TailerOptions#getRegex()}.
     */
    @Test
    void testGetRegex() {
        assertEquals(".*", tailerOptions.getRegex());
    }

}
