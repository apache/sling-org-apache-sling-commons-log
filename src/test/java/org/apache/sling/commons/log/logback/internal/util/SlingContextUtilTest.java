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

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class SlingContextUtilTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.SlingContextUtil#getDeclaredOrigin()}.
     */
    @Test
    void testGetDeclaredOrigin() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Object origin = new Object();
        SlingContextUtil contextUtil = new SlingContextUtil(loggerContext, origin);
        assertEquals(origin, contextUtil.getDeclaredOrigin());
    }
}
