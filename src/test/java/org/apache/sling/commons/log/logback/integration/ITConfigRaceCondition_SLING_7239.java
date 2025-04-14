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

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static java.lang.String.format;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITConfigRaceCondition_SLING_7239 extends LogTestBase {
    private static final String LOG_BUNDLE_NAME = "org.apache.sling.commons.log";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder(new File("target"));

    @Inject
    private ConfigurationAdmin ca;

    @Inject
    private BundleContext bundleContext;

    private Random rnd = new Random();

    @Override
    protected Option addExtraOptions() {
        return composite(configAdmin(), mavenBundle("commons-io", "commons-io").versionAsInProject());
    }

    @Override
    protected boolean shouldStartLogBundle() {
        return false;
    }

    @Test
    public void multipleLogConfigs() throws Exception {
        int configCount = 200 + rnd.nextInt(100);

        // 1. Create lots of log configs. This would create a situation where
        // only few of the config get picked up at time of activation
        createLogConfigs(configCount);

        // 2. Register listener to get notified when the activator is done
        CountDownLatch startLatch = new CountDownLatch(1);
        BundleListener listener = e -> {
            if (e.getBundle().getSymbolicName().equals(LOG_BUNDLE_NAME) && e.getType() == BundleEvent.STARTED) {
                startLatch.countDown();
            }
        };

        bundleContext.addBundleListener(listener);

        Bundle logb = getBundle(LOG_BUNDLE_NAME);
        logb.start();

        // 3. Wait for activator to complete
        startLatch.await();

        // Now check by polling that number of log file created in log dir match the
        // actual number of configs created
        new RetryLoop(
                new RetryLoop.Condition() {
                    @Override
                    public String getDescription() {
                        return format("Expected log file count [%d], Found [%d]", configCount, getLogFileCount());
                    }

                    @Override
                    public boolean isTrue() throws Exception {
                        return configCount == getLogFileCount();
                    }
                },
                15,
                100);
    }

    private int getLogFileCount() {
        return tmpFolder.getRoot().listFiles().length;
    }

    private Bundle getBundle(String bundleName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (bundleName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        fail("Not able find bundle " + bundleName);
        return null;
    }

    private void createLogConfigs(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            createLogConfig(i);
        }
    }

    private void createLogConfig(int index) throws IOException {
        Configuration config = ca.createFactoryConfiguration(LogConstants.FACTORY_PID_CONFIGS, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LogConstants.LOG_LOGGERS, new String[] {"foo.bar." + index});
        p.put(LogConstants.LOG_LEVEL, "DEBUG");

        File logFile = new File(tmpFolder.getRoot(), "error-" + index + ".log");
        p.put(LogConstants.LOG_FILE, logFile.getAbsolutePath());
        config.update(p);
    }
}
