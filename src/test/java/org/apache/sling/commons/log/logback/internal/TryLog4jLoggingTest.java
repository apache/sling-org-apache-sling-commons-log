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

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class TryLog4jLoggingTest extends BaseTryLoggingTest {

    @Test
    void testLog4jCallsHandledByLogback() throws IOException {
        Logger logger = Logger.getLogger(getClass());
        logger.warn("Log Message from Log4J");

        assertTrue(TestUtils.containsString(new File("target", logFilePath), "Log Message from Log4J"));
    }
}
