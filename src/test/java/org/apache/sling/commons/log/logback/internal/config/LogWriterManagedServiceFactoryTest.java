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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.apache.sling.commons.log.logback.internal.LogConstants;
import org.apache.sling.commons.log.logback.internal.util.TestUtils;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class LogWriterManagedServiceFactoryTest {
    protected final OsgiContext context = new OsgiContext();

    private LogConfigManager mgr;
    private LogWriterManagedServiceFactory factory;

    private String pid;

    @BeforeEach
    protected void beforeEach(TestInfo testInfo) throws InvalidSyntaxException {
        try {
            System.setProperty(LogConstants.SLING_HOME, new File("target").getPath());

            mgr = new LogConfigManager(context.bundleContext());
        } finally {
            // cleanup to not interfere with other tests
            System.clearProperty(LogConstants.SLING_HOME);
        }
        mgr.start();

        BundleContext bundleContext = context.bundleContext();
        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, LogConstants.FACTORY_PID_WRITERS);
        Collection<ServiceReference<ManagedServiceFactory>> serviceReferences = bundleContext.getServiceReferences(ManagedServiceFactory.class, filter);
        assertEquals(1, serviceReferences.size());
        ServiceReference<ManagedServiceFactory> svcRef = serviceReferences.stream().findFirst().orElse(null);
        assertNotNull(svcRef);
        factory = (LogWriterManagedServiceFactory)bundleContext.getService(svcRef);
        assertNotNull(factory);

        pid = String.format("%s~%s/%s.log", LogConstants.FACTORY_PID_WRITERS,
                testInfo.getTestClass().get().getName(),
                testInfo.getTestMethod().get().getName());
    }

    @AfterEach
    protected void afterEach() {
        if (mgr != null) {
            mgr.stop();
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LogWriterManagedServiceFactory#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals("LogWriter configurator", factory.getName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LogWriterManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)}.
     */
    @Test
    void testUpdated() throws ConfigurationException {
        Hashtable<String, Object> config = new Hashtable<>(Map.of(
                    LogConstants.LOG_FILE, "log/testUpdated.log",
                    LogConstants.LOG_FILE_NUMBER, 6,
                    LogConstants.LOG_FILE_SIZE, LogConstants.LOG_FILE_SIZE_DEFAULT,
                    LogConstants.LOG_FILE_BUFFERED, true
                ));
        assertDoesNotThrow(() -> factory.updated(pid, config));
        assertTrue(mgr.hasWriterByPid(pid));
        String logPath = Paths.get("target", "log/testUpdated.log").toAbsolutePath().toString();
        assertTrue(mgr.hasWriterByName(logPath));
    }

    @Test
    void testUpdatedWithConfigurationException() throws Exception {
        Dictionary<String, ?> config = new Hashtable<>(Map.of(
                    LogConstants.LOG_FILE, "log/testUpdatedWithConfigurationException.log"
                ));
        assertDoesNotThrow(() -> factory.updated(pid, config));

        // call again with the same pid (no error just replace the old config)
        assertDoesNotThrow(() -> factory.updated(pid, config));

        // call again with a different pid (throws exception about duplicate file path)
        ConfigurationException t = assertThrows(ConfigurationException.class, () -> factory.updated(pid + "2", config));
        String logPath = Paths.get("target", "log/testUpdatedWithConfigurationException.log").toAbsolutePath().toString();
        assertEquals(String.format("LogFile %s already configured by configuration %s", logPath, pid), t.getReason());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.config.LogWriterManagedServiceFactory#deleted(java.lang.String)}.
     */
    @Test
    void testDeleted() throws ConfigurationException {
        testUpdated();

        assertDoesNotThrow(() -> factory.deleted(pid));
        assertFalse(mgr.hasWriterByPid(pid));
        String logPath = Paths.get("target", "log/testUpdated.log").toAbsolutePath().toString();
        assertFalse(mgr.hasWriterByName(logPath));
    }

    @Test
    void testDeletedWithConfigurationException() throws Exception {
        mgr = Mockito.spy(mgr);
        Mockito.doThrow(org.apache.sling.commons.log.logback.internal.config.ConfigurationException.class)
            .when(mgr).updateLogWriter(pid, null, true);
        factory.setLogConfigManager(mgr);

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().clear();

        String output = TestUtils.doWorkWithCapturedStdErr(() -> {
            factory.deleted(pid);
        });
        assertTrue(output.contains("Unexpected Configuration Problem"));

        // verify the error status was reported
        List<Status> copyOfStatusList = loggerContext
                .getStatusManager()
                .getCopyOfStatusList();
        // the last status should be the error msg
        assertEquals("Unexpected Configuration Problem",
                copyOfStatusList.get(copyOfStatusList.size() - 1).getMessage());
    }

}
