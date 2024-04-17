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
package org.apache.sling.commons.log.logback.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 *
 */
class ConfigurationExceptionTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigurationException#ConfigurationException(java.lang.String, java.lang.String)}.
     */
    @Test
    void testConfigurationExceptionStringString() {
        ConfigurationException e = new ConfigurationException("prop1", "reason1");
        assertEquals("prop1", e.getProperty());
        assertEquals("reason1", e.getReason());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigurationException#ConfigurationException(java.lang.String, java.lang.String, java.lang.Throwable)}.
     */
    @Test
    void testConfigurationExceptionStringStringThrowable() {
        IllegalStateException throwable = new IllegalStateException("Something is wrong");
        ConfigurationException e = new ConfigurationException("prop1", "reason1", throwable);
        assertEquals("prop1", e.getProperty());
        assertEquals("reason1", e.getReason());
        assertEquals(throwable, e.getCause());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigurationException#getProperty()}.
     */
    @Test
    void testGetProperty() {
        ConfigurationException e = new ConfigurationException("prop1", "reason1");
        assertEquals("prop1", e.getProperty());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.ConfigurationException#getReason()}.
     */
    @Test
    void testGetReason() {
        ConfigurationException e = new ConfigurationException("prop1", "reason1");
        assertEquals("reason1", e.getReason());
    }

}
