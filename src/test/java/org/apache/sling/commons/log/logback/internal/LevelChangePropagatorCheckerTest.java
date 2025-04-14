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

import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class LevelChangePropagatorCheckerTest {

    @AfterEach
    protected void afterEach() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        // remove the listener so we don't interfere with other tests
        List<LoggerContextListener> listenerList = loggerContext.getCopyOfListenerList();
        listenerList.stream().filter(LevelChangePropagator.class::isInstance).forEach(l -> {
            loggerContext.removeListener(l);
        });
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.LevelChangePropagatorChecker#onResetComplete(ch.qos.logback.classic.LoggerContext)}.
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testOnResetComplete(boolean bridgeIsInstalled) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        LevelChangePropagatorChecker checker = new LevelChangePropagatorChecker(() -> bridgeIsInstalled);

        loggerContext.getStatusManager().clear();
        checker.onResetComplete(loggerContext);
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertEquals(!bridgeIsInstalled, copyOfStatusList.isEmpty());
        // verify the error status was reported
        assertEquals(
                bridgeIsInstalled,
                copyOfStatusList.stream()
                        .anyMatch(s -> "Slf4j bridge handler found to be enabled. Installing the LevelChangePropagator"
                                .equals(s.getMessage())),
                "Expected error status msg");
    }

    @Test
    void testOnResetCompleteWhenPropagatorAlreadyInstalled() {
        // pre-add the propagator
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
        levelChangePropagator.setContext(loggerContext);
        levelChangePropagator.start();
        loggerContext.addListener(levelChangePropagator);

        // now reset should not add it again
        LevelChangePropagatorChecker checker = new LevelChangePropagatorChecker(() -> true);
        loggerContext.getStatusManager().clear();
        checker.onResetComplete(loggerContext);
        List<Status> copyOfStatusList = loggerContext.getStatusManager().getCopyOfStatusList();
        assertTrue(copyOfStatusList.isEmpty());
    }
}
