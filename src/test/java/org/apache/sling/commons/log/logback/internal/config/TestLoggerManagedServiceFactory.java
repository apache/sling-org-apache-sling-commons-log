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

package org.apache.sling.commons.log.logback.internal.config;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestLoggerManagedServiceFactory {
    
    private static final String TEST_OVERRIDE = "TEST_OVERRIDE";

    @Test
    public void testFileAddition() throws ConfigurationException, org.apache.sling.commons.log.logback.internal.config.ConfigurationException {
        LoggerManagedServiceFactory lmsf = Mockito.spy(LoggerManagedServiceFactory.class);
        LogConfigManager lcm = mock(LogConfigManager.class);
        doReturn(lcm).when(lmsf).getLogConfigManager();

        ArgumentCaptor<Dictionary<String, String>> effectiveConfigCaptor = ArgumentCaptor.forClass(Dictionary.class);
        lmsf.updated("test", new Hashtable<String, String>());
        verify(lcm).updateLoggerConfiguration(anyString(), effectiveConfigCaptor.capture(), anyBoolean());
        assertEquals("logs/error.log", effectiveConfigCaptor.getValue().get(LogConfigManager.LOG_FILE));
    }
    
    @Test
    public void testFileNoOverride() throws ConfigurationException, org.apache.sling.commons.log.logback.internal.config.ConfigurationException {
        LoggerManagedServiceFactory lmsf = Mockito.spy(LoggerManagedServiceFactory.class);
        LogConfigManager lcm = mock(LogConfigManager.class);
        doReturn(lcm).when(lmsf).getLogConfigManager();
        
        ArgumentCaptor<Dictionary<String, String>> effectiveConfigCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(LogConfigManager.LOG_FILE, TEST_OVERRIDE);
        lmsf.updated("test", dict);
        verify(lcm).updateLoggerConfiguration(anyString(), effectiveConfigCaptor.capture(), anyBoolean());
        assertEquals(TEST_OVERRIDE, effectiveConfigCaptor.getValue().get(LogConfigManager.LOG_FILE));
    }

}
