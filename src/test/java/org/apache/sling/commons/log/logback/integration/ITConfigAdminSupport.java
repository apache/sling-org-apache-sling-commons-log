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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITConfigAdminSupport extends LogTestBase {

    @Inject
    private ConfigurationAdmin ca;

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Override
    protected Option addExtraOptions() {
        return composite(configAdmin(), mavenBundle("commons-io", "commons-io").versionAsInProject());
    }

    @Test
    public void testChangeLogLevelWithConfig() throws Exception {
        // Set log level to debug for foo1.bar
        Configuration config = ca.createFactoryConfiguration(LogConstants.FACTORY_PID_CONFIGS, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LOGGERS, new String[] {
            "foo1.bar"
        });
        p.put(LogConstants.LOG_LEVEL, "DEBUG");
        config.update(p);

        delay();

        Logger slf4jLogger = LoggerFactory.getLogger("foo1.bar");
        assertTrue(slf4jLogger.isDebugEnabled());
        assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isInfoEnabled());
        assertFalse(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());
    }

    @Test
    public void testResetToDefault() throws Exception {
        ch.qos.logback.classic.Logger lgLog =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("foo2.bar");

        lgLog.setLevel(Level.TRACE);
        assertEquals(Level.TRACE, lgLog.getLevel());

        // Set log level to debug for foo2.bar
        Configuration config = ca.createFactoryConfiguration(LogConstants.FACTORY_PID_CONFIGS, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LOGGERS, new String[]{
                "foo2.bar"
        });
        p.put(LogConstants.LOG_LEVEL, "DEFAULT");
        config.update(p);

        delay();

        Logger slf4jLogger = LoggerFactory.getLogger("foo2.bar");
        assertFalse(slf4jLogger.isDebugEnabled());
        assertFalse(slf4jLogger.isTraceEnabled());
        assertTrue(lgLog.isAdditive());
    }

    @Test
    public void testChangeGlobalConfig() throws Exception {
        // Set log level to debug for Root logger
        Configuration config = ca.getConfiguration(LogConstants.PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LEVEL, "DEBUG");
        config.update(p);

        delay();

        assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());

        // Reset back to Info
        config = ca.getConfiguration(LogConstants.PID, null);
        p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LEVEL, "INFO");
        config.update(p);

        delay();

        assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isInfoEnabled());
        assertFalse(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());
    }

    @Test
    public void testPackagingDataConfig() throws Exception {
        // Set log level to debug for Root logger
        Configuration config = ca.getConfiguration(LogConstants.PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_PACKAGING_DATA, Boolean.TRUE);
        p.put(LogConstants.LOG_LEVEL, "INFO");
        config.update(p);

        delay();

        assertFalse(((LoggerContext)LoggerFactory.getILoggerFactory()).isPackagingDataEnabled());
    }

    @Test
    public void testExternalConfig() throws Exception {
        Configuration config = ca.getConfiguration(LogConstants.PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LEVEL, "DEBUG");
        p.put(LogConstants.LOGBACK_FILE,absolutePath("test1-external-config.xml"));
        config.update(p);

        delay();

        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        assertNotNull(rootLogger.getAppender("FILE"));
    }
}
