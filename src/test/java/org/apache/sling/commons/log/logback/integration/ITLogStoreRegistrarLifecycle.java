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
package org.apache.sling.commons.log.logback.integration;

import javax.inject.Inject;

import java.util.Dictionary;
import java.util.Hashtable;

import ch.qos.logback.core.Appender;
import org.apache.sling.commons.log.logback.store.LogStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITLogStoreRegistrarLifecycle extends LogTestBase {

    private static final String LOG_STORE_PID = "org.apache.sling.commons.log.LogStore";
    private static final String MAX_ENTRIES = "maxEntries";

    @Inject
    private ConfigurationAdmin ca;

    @Override
    protected Option addExtraOptions() {
        return composite(configAdmin(), mavenBundle("commons-io", "commons-io").versionAsInProject());
    }

    @Test
    public void testLifecycle() throws Exception {
        Configuration config = ca.getConfiguration(LOG_STORE_PID, null);
        try {
            assertEquals(
                    0, bundleContext.getServiceReferences(LogStore.class, null).size());
            assertEquals(
                    0, bundleContext.getServiceReferences(Appender.class, null).size());

            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(MAX_ENTRIES, 5);
            config.update(properties);
            delay();

            assertEquals(
                    1, bundleContext.getServiceReferences(LogStore.class, null).size());
            assertEquals(
                    1, bundleContext.getServiceReferences(Appender.class, null).size());

            properties.put(MAX_ENTRIES, 7);
            config.update(properties);
            delay();

            assertEquals(
                    1, bundleContext.getServiceReferences(LogStore.class, null).size());
            assertEquals(
                    1, bundleContext.getServiceReferences(Appender.class, null).size());
        } finally {
            config.delete();
            delay();
        }

        assertEquals(0, bundleContext.getServiceReferences(LogStore.class, null).size());
        assertEquals(0, bundleContext.getServiceReferences(Appender.class, null).size());
    }
}
