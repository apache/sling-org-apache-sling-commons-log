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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.LoggerFactory;

import static org.apache.sling.commons.log.logback.integration.ITConfigFragments.RESET_EVENT_TOPIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITAppenderServices extends LogTestBase {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private ConfigurationAdmin ca;

    @SuppressWarnings("rawtypes")
    private ServiceRegistration<Appender> sr;

    @Inject
    private EventAdmin eventAdmin;

    static {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addExtraOptions() {
        return composite(configAdmin(), mavenBundle("commons-io", "commons-io").versionAsInProject(), eventAdmin());
    }

    @Test
    public void testAppenderService() throws Exception {
        TestAppender ta = registerAppender("foo.bar", "foo.baz");
        delay();

        Logger bar = (Logger) LoggerFactory.getLogger("foo.bar");
        bar.setLevel(Level.DEBUG);
        Logger baz = (Logger) LoggerFactory.getLogger("foo.baz");
        baz.setLevel(Level.INFO);

        bar.debug("Test message");
        baz.debug("Test message"); // Would not be logged

        // One event should be logged.
        assertEquals(1, ta.events.size());
    }

    @Test
    public void testRootAppenderService() throws Exception {
        TestAppender ta = registerAppender("ROOT");
        delay();

        Logger root = (Logger) LoggerFactory.getLogger("ROOT");
        root.setLevel(Level.DEBUG);
        Logger foobar = (Logger) LoggerFactory.getLogger("foo.bar");
        foobar.setLevel(Level.INFO);

        root.debug("one");
        foobar.debug("two");
        foobar.info("three");

        assertTrue(ta.events.size() >= 2);
        assertTrue(ta.events.stream().anyMatch(e -> "one".equals(e.getFormattedMessage())));
        assertFalse(ta.events.stream().anyMatch(e -> "two".equals(e.getFormattedMessage())));
        assertTrue(ta.events.stream().anyMatch(le -> "three".equals(le.getFormattedMessage())));
    }

    @Test
    public void testAppenderServiceModified() throws Exception {
        TestAppender ta = registerAppender("foo.bar", "foo.baz");
        delay();

        Logger bar = (Logger) LoggerFactory.getLogger("foo.bar");
        bar.setLevel(Level.DEBUG);
        Logger baz = (Logger) LoggerFactory.getLogger("foo.baz");
        baz.setLevel(Level.INFO);

        bar.debug("Test message");
        baz.debug("Test message"); // Would not be logged

        // One event should be logged.
        assertEquals(1, ta.events.size());

        ta.reset();

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("loggers", new String[] {"foo.bar2"});
        sr.setProperties(props);

        delay();
        LoggerFactory.getLogger("foo.bar2").info("foo.bar2");
        LoggerFactory.getLogger("foo.baz").info("foo.baz");

        assertEquals(1, ta.msgs.size());
        assertTrue(ta.msgs.contains("foo.bar2"));
        assertFalse(ta.msgs.contains("foo.baz"));
    }

    @Test
    public void testOsgiAppenderRef() throws Exception {
        Configuration config = ca.getConfiguration(LogConstants.PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LEVEL, "INFO");
        p.put(LogConstants.LOGBACK_FILE, absolutePath("test-osg-appender-ref-config.xml"));
        config.update(p);

        delay();

        Logger ref = (Logger) LoggerFactory.getLogger("foo.ref.osgi");
        assertTrue(ref.isDebugEnabled());

        TestAppender ta = registerAppender("foo.bar", "foo.baz");
        delay();

        Logger bar = (Logger) LoggerFactory.getLogger("foo.bar");
        bar.setLevel(Level.DEBUG);
        Logger baz = (Logger) LoggerFactory.getLogger("foo.baz");
        baz.setLevel(Level.INFO);

        bar.debug("Test message");
        baz.debug("Test message"); // Would not be logged

        ref.debug("Test message ref");

        // One event should be logged.
        assertEquals(2, ta.events.size());
    }

    @Test
    public void appenderRestartPostReset() throws Exception {
        final TestAppender ta = registerAppender("ROOT");
        delay();

        assertTrue(ta.isStarted());
        final int stopCount = ta.stopCount;
        final int startCount = ta.startCount;

        eventAdmin.sendEvent(new Event(RESET_EVENT_TOPIC, (Dictionary<String, ?>) null));

        new RetryLoop(
                new RetryLoop.Condition() {
                    @Override
                    public String getDescription() {
                        return "Stopcount not increased";
                    }

                    @Override
                    public boolean isTrue() throws Exception {
                        return ta.stopCount > stopCount && ta.startCount > startCount;
                    }
                },
                10,
                100);

        assertTrue(ta.isStarted());
    }

    @After
    public void unregisterAppender() {
        sr.unregister();
    }

    private TestAppender registerAppender(String... loggers) {
        TestAppender ta = new TestAppender();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("loggers", loggers);
        sr = bundleContext.registerService(Appender.class, ta, props);
        delay();
        return ta;
    }

    private static class TestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();
        final List<String> msgs = new ArrayList<String>();
        int stopCount;
        int startCount;

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
            msgs.add(eventObject.getFormattedMessage());
        }

        @Override
        public String getName() {
            return "TestAppender";
        }

        public void reset() {
            events.clear();
            msgs.clear();
        }

        @Override
        public void stop() {
            super.stop();
            stopCount++;
        }

        @Override
        public void start() {
            super.start();
            startCount++;
        }
    }
}
