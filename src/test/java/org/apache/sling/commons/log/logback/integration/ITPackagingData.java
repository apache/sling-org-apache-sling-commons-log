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

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;

import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.sling.commons.log.logback.integration.bundle.PackageDataActivator;
import org.apache.sling.commons.log.logback.internal.LogbackManager;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

import static org.apache.sling.commons.log.logback.integration.ITConfigAdminSupport.PID;
import static org.apache.sling.commons.log.logback.integration.PackagingDataTestUtil.TEST_BUNDLE_NAME;
import static org.apache.sling.commons.log.logback.integration.PackagingDataTestUtil.TEST_BUNDLE_VERSION;
import static org.apache.sling.commons.log.logback.internal.LogConfigManager.FACTORY_PID_CONFIGS;
import static org.apache.sling.commons.log.logback.internal.LogConfigManager.LOG_FILE;
import static org.apache.sling.commons.log.logback.internal.LogConfigManager.LOG_LEVEL;
import static org.apache.sling.commons.log.logback.internal.LogConfigManager.LOG_LOGGERS;
import static org.apache.sling.commons.log.logback.internal.LogConfigManager.LOG_PACKAGING_DATA;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ITPackagingData extends LogTestBase {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private ConfigurationAdmin ca;

    @Override
    protected Option addExtraOptions() {
        return composite(
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                configAdmin(),
                mavenBundle("org.osgi", "org.osgi.service.log").versionAsInProject(),
                mavenBundle("biz.aQute.bnd", "biz.aQute.bndlib").versionAsInProject(),
                mavenBundle("org.ops4j.pax.tinybundles", "tinybundles").versionAsInProject(),
                mavenBundle("commons-io", "commons-io").versionAsInProject()
        );
    }

    @Test
    public void defaultWorking() throws Exception{
        ServiceTracker<WeavingHook, WeavingHook> tracker = createWeavingHookTracker();
        tracker.open();

        assertNull(tracker.getService());
        tracker.close();
    }

    @Test
    public void packagingEnabled() throws Exception{
        // Set log level to debug for Root logger
        Configuration config = ca.getConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LOG_PACKAGING_DATA, Boolean.TRUE);
        p.put(LOG_LEVEL, "INFO");
        config.update(p);

        delay();

        ServiceTracker<WeavingHook, WeavingHook> tracker = createWeavingHookTracker();
        tracker.open();

        assertNotNull(tracker.getService());
        tracker.close();
    }

    @Test
    public void packageDataWorking() throws Exception{
        System.getenv().entrySet().stream()
            .filter(e -> e.getKey().contains("BUILD") || e.getKey().contains("JENKINS") )
            .forEach( System.out::println );
        Assume.assumeFalse("SLING-12711", 
            System.getProperty("os.name").toLowerCase().contains("windows") &&
            System.getenv("JENKINS_URL") != null);

        // Enable packaging
        Configuration config = ca.getConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put(LOG_PACKAGING_DATA, Boolean.TRUE);
        p.put(LOG_LEVEL, "INFO");
        config.update(p);
        delay();

        //Configure a file logger for test class
        Configuration config2 = ca.createFactoryConfiguration(FACTORY_PID_CONFIGS, null);
        Dictionary<String, Object> p2 = new Hashtable<String, Object>();
        p2.put(LOG_LOGGERS, new String[] {
                PackageDataActivator.LOGGER_NAME
        });
        String logFileName = "logs/package-test.log";
        p2.put(LOG_FILE, logFileName);
        p2.put(LOG_LEVEL, "INFO");
        config2.update(p2);
        delay();


        //Ensure that weaving hook comes up
        ServiceTracker<WeavingHook, WeavingHook> tracker = createWeavingHookTracker();
        tracker.open();
        tracker.waitForService(60*1000);

        //Now install the test bundle such that hook picks it up
        InputStream inp = PackagingDataTestUtil.createTestBundle();
        Bundle b = bundleContext.installBundle("test", inp);
        b.start();

        //Now wait for runnable registered by the test bundle
        ServiceTracker<Runnable, Runnable> runableTracker = createTracker(Runnable.class, PackageDataActivator.LOGGER_NAME);
        runableTracker.open();

        //Now invoke the method to trigger logger call with exception
        Runnable r = runableTracker.waitForService(60*1000);
        r.run();

        //Now read the log file content to assert that stacktrace has version present
        String slingHome = System.getProperty("sling.home");
        File logFile = new File(FilenameUtils.concat(slingHome, logFileName));
        String logFileContent = FileUtils.readFileToString(logFile);

        System.out.println("--------------------");
        System.out.println(logFileContent);
        System.out.println("--------------------");

        List<String> lines = FileUtils.readLines(logFile);
        String testLine = null;
        for (String l : lines) {
            if (l.contains("org.apache.sling.commons.log.logback.integration.bundle.TestRunnable.run(")){
                testLine = l;
                break;
            }
        }
        assertNotNull(testLine);
        assertThat(testLine, containsString("["+ TEST_BUNDLE_NAME+":"+TEST_BUNDLE_VERSION+"]"));

        //Check that default logback support is still disabled
        assertFalse(((LoggerContext) LoggerFactory.getILoggerFactory()).isPackagingDataEnabled());
    }

    private ServiceTracker<WeavingHook, WeavingHook> createWeavingHookTracker() throws InvalidSyntaxException {
        return createTracker(WeavingHook.class, LogbackManager.PACKAGE_INFO_COLLECTOR_DESC);
    }

    private <T> ServiceTracker<T,T> createTracker(Class<T> clazz, String desc) throws InvalidSyntaxException {
        String filter = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, clazz.getName(),
                Constants.SERVICE_DESCRIPTION, desc);
        Filter f = FrameworkUtil.createFilter(filter);
        return new ServiceTracker<>(bundleContext, f, null);
    }

}
