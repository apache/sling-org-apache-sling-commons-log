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
package org.apache.sling.commons.log.logback.internal.joran;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class OsgiAppenderRefModelTest {

    private OsgiAppenderRefModel model = new OsgiAppenderRefModel();

    @BeforeEach
    protected void beforeEach() {
        model = new OsgiAppenderRefModel();
        model.setTag("appender-ref-osgi");
        model.setRef("appender1");
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel#hashCode()}.
     */
    @Test
    void testHashCode() {
        int hashCode = model.hashCode();

        OsgiAppenderRefModel model2 = new OsgiAppenderRefModel();
        int hashCode2 = model2.hashCode();

        assertNotEquals(hashCode, hashCode2);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel#makeNewInstance()}.
     */
    @Test
    void testMakeNewInstance() {
        OsgiAppenderRefModel newInstance = model.makeNewInstance();
        assertNotNull(newInstance);
        assertNotSame(model, newInstance);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel#mirror(ch.qos.logback.core.model.Model)}.
     */
    @Test
    void testMirrorModel() {
        OsgiAppenderRefModel newInstance = model.makeNewInstance();
        model.mirror(newInstance);

        assertEquals(model, newInstance);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel#getRef()}.
     */
    @Test
    void testGetRef() {
        assertEquals("appender1", model.getRef());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel#setRef(java.lang.String)}.
     */
    @Test
    void testSetRef() {
        model.setRef("appender2");
        assertEquals("appender2", model.getRef());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.joran.OsgiAppenderRefModel#equals(java.lang.Object)}.
     */
    @SuppressWarnings("unlikely-arg-type")
    @Test
    void testEquals() {
        assertTrue(model.equals(model)); // NOSONAR

        assertFalse(model.equals(null)); // NOSONAR

        assertFalse(model.equals("invalid")); // NOSONAR

        OsgiAppenderRefModel newInstance = model.makeNewInstance();
        assertFalse(model.equals(newInstance)); // NOSONAR

        model.mirror(newInstance);
        assertTrue(model.equals(newInstance)); // NOSONAR
    }
}
