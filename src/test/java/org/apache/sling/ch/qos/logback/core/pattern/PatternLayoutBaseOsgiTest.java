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
package org.apache.sling.ch.qos.logback.core.pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.PatternLayoutBaseOsgi;
import ch.qos.logback.core.pattern.color.BlueCompositeConverter;
import ch.qos.logback.core.status.Status;

/**
 *
 */
class PatternLayoutBaseOsgiTest {

    private PatternLayoutBaseOsgi<ILoggingEvent> patternLayoutBase;
    private Map<String, Supplier<Converter<ILoggingEvent>>> converterSupplierMap1;
    private Map<String, Supplier<Converter<ILoggingEvent>>> converterSupplierMap2;

    @BeforeEach
    private void beforeEach() {
        patternLayoutBase = new PatternLayoutBaseOsgi<ILoggingEvent>() {
            private Map<String, Supplier<Converter<ILoggingEvent>>> defConverterSupplierMap = new HashMap<>();

            @Override
            public Map<String, Supplier<Converter<ILoggingEvent>>> getDefaultConverterSupplierMap() {
                return defConverterSupplierMap;
            }

            @Override
            public String doLayout(ILoggingEvent event) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<String, String> getDefaultConverterMap() {
                return Collections.emptyMap();
            }
        };
        Context context = new ContextBase();
        patternLayoutBase.setContext(context);
        converterSupplierMap1 = Map.of(
                "keyword1", MessageConverter::new
                );
        converterSupplierMap2 = Map.of(
                "composite1", BlueCompositeConverter::new
                );
    }

    /**
     * Test method for {@link ch.qos.logback.core.pattern.PatternLayoutBaseOsgi#start()}.
     */
    @ParameterizedTest
    @NullAndEmptySource
    void testStartWithMissingPattern(String pattern) {
        patternLayoutBase.setPattern(pattern);
        patternLayoutBase.start();

        List<Status> copyOfStatusList = patternLayoutBase.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Empty or null pattern.".equals(s.getMessage())),
                "Expected empty pattern error status msg");
    }
    @Test
    void testStart() {
        patternLayoutBase.getInstanceConverterSupplierMap().putAll(converterSupplierMap1);
        patternLayoutBase.getDefaultConverterSupplierMap().putAll(converterSupplierMap2);

        patternLayoutBase.setPattern("%keyword1 %composite1");
        patternLayoutBase.start();

        List<Status> copyOfStatusList = patternLayoutBase.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.isEmpty());
    }
    @Test
    void testStartWithNullContext() {
        patternLayoutBase.setContext(null);

        patternLayoutBase.getInstanceConverterSupplierMap().putAll(converterSupplierMap1);
        patternLayoutBase.getDefaultConverterSupplierMap().putAll(converterSupplierMap2);

        patternLayoutBase.setPattern("%keyword1 %composite1");
        patternLayoutBase.start();

        assertNull(patternLayoutBase.getStatusManager());
    }
    @Test
    void testStartWithCaughtScanException() {
        patternLayoutBase.getInstanceConverterSupplierMap().putAll(converterSupplierMap1);
        patternLayoutBase.getDefaultConverterSupplierMap().putAll(converterSupplierMap2);

        patternLayoutBase.setPattern("%d{");
        patternLayoutBase.start();

        List<Status> copyOfStatusList = patternLayoutBase.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to parse pattern \"%d{\".".equals(s.getMessage())),
                "Expected empty pattern error status msg");
    }

    /**
     * Test method for {@link ch.qos.logback.core.pattern.PatternLayoutBaseOsgi#getDefaultConverterSupplierMap()}.
     */
    @Test
    void testGetDefaultConverterSupplierMap() {
        Map<String, Supplier<Converter<ILoggingEvent>>> map = patternLayoutBase.getDefaultConverterSupplierMap();
        assertNotNull(map);

        patternLayoutBase.getDefaultConverterSupplierMap().putAll(converterSupplierMap1);
        patternLayoutBase.getDefaultConverterSupplierMap().putAll(converterSupplierMap2);
        assertTrue(map.containsKey("keyword1"));
        assertTrue(map.containsKey("composite1"));
    }

    /**
     * Test method for {@link ch.qos.logback.core.pattern.PatternLayoutBaseOsgi#getEffectiveConverterSupplierMap()}.
     */
    @Test
    void testGetEffectiveConverterSupplierMap() {
        Map<String, Supplier<Converter<ILoggingEvent>>> map = patternLayoutBase.getEffectiveConverterSupplierMap();
        assertNotNull(map);

        patternLayoutBase.getInstanceConverterSupplierMap().putAll(converterSupplierMap1);
        patternLayoutBase.getDefaultConverterSupplierMap().putAll(converterSupplierMap2);

        map = patternLayoutBase.getEffectiveConverterSupplierMap();
        assertTrue(map.containsKey("keyword1"));
        assertTrue(map.containsKey("composite1"));
    }
    @Test
    void testGetEffectiveConverterSupplierMapWithNullMaps() {
        patternLayoutBase = Mockito.spy(patternLayoutBase);
        Mockito.doReturn(null).when(patternLayoutBase).getDefaultConverterSupplierMap();
        Mockito.doReturn(null).when(patternLayoutBase).getInstanceConverterSupplierMap();
        Map<String, Supplier<Converter<ILoggingEvent>>> map = patternLayoutBase.getEffectiveConverterSupplierMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    /**
     * Test method for {@link ch.qos.logback.core.pattern.PatternLayoutBaseOsgi#getInstanceConverterSupplierMap()}.
     */
    @Test
    void testGetInstanceConverterSupplierMap() {
        Map<String, Supplier<Converter<ILoggingEvent>>> map = patternLayoutBase.getInstanceConverterSupplierMap();
        assertNotNull(map);

        patternLayoutBase.getInstanceConverterSupplierMap().putAll(converterSupplierMap1);
        patternLayoutBase.getInstanceConverterSupplierMap().putAll(converterSupplierMap2);
        assertTrue(map.containsKey("keyword1"));
        assertTrue(map.containsKey("composite1"));
    }

}
