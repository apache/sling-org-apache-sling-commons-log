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
package org.apache.sling.ch.qos.logback.core.pattern.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.color.BlueCompositeConverter;
import ch.qos.logback.core.pattern.parser.CompositeNode;
import ch.qos.logback.core.pattern.parser.ParserOsgi;
import ch.qos.logback.core.pattern.parser.SimpleKeywordNode;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.Status;

/**
 *
 */
class ParserOsgiTest {
    private ParserOsgi<ILoggingEvent> parser;
    private Map<String, String> converterMap;
    private Map<String, Supplier<Converter<ILoggingEvent>>> converterSupplierMap;

    private static class MockConverter<E> extends Converter<E> {
        @Override
        public String convert(E event) {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    private void beforeEach() throws ScanException {
        converterMap = Map.of(
                "keyword2", MessageConverter.class.getName(),
                "keyword4", "org.apache.sling.NotValid",
                "composite2", BlueCompositeConverter.class.getName(),
                "composite4", "org.apache.sling.NotValid"
                );
        converterSupplierMap = Map.of(
                "keyword1", MessageConverter::new,
                "keyword3", MockConverter::new, // not a DynamicConverter
                "composite1", BlueCompositeConverter::new,
                "composite3", MessageConverter::new // not a CompositeConverter
                );
        parser = new ParserOsgi<>("pattern");
        Context context = new ContextBase();
        parser.setContext(context);
    }

    @Test
    void testCreateConverterSimpleKeywordNodeFromClassname() {
        SimpleKeywordNode node = mockSimpleKeywordNode("keyword2");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof MessageConverter);
    }
    @Test
    void testCreateConverterSimpleKeywordNodeFromClassnameThatDoesNotExist() {
        SimpleKeywordNode node = mockSimpleKeywordNode("keyword4");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof LiteralConverter);
        assertEquals("%PARSER_ERROR[keyword4]", ((LiteralConverter<ILoggingEvent>)converter).convert(null));
        List<Status> copyOfStatusList = parser.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to instantiate converter class [org.apache.sling.NotValid] for keyword [keyword4]".equals(s.getMessage())),
                "Expected parsing error status msg");
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "[keyword4] is not a valid conversion word".equals(s.getMessage())),
                "Expected parsing error status msg");
    }
    @Test
    void testCreateConverterSimpleKeywordNodeFromNullConverter() {
        SimpleKeywordNode node = mockSimpleKeywordNode("keyword5");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof LiteralConverter);
        assertEquals("%PARSER_ERROR[keyword5]", ((LiteralConverter<ILoggingEvent>)converter).convert(null));
        List<Status> copyOfStatusList = parser.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "There is no conversion registered for conversion word [keyword5]".equals(s.getMessage())),
                "Expected parsing error status msg");
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "[keyword5] is not a valid conversion word".equals(s.getMessage())),
                "Expected parsing error status msg");
    }
    @Test
    void testCreateConverterSimpleKeywordNodeFromSupplier() {
        SimpleKeywordNode node = mockSimpleKeywordNode("keyword1");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof MessageConverter);
    }
    @Test
    void testCreateConverterSimpleKeywordNodeFromSupplierThatReturnsWrongType() {
        SimpleKeywordNode node = mockSimpleKeywordNode("keyword3");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof LiteralConverter);
        assertEquals("%PARSER_ERROR[keyword3]", ((LiteralConverter<ILoggingEvent>)converter).convert(null));
        List<Status> copyOfStatusList = parser.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to supply converter for keyword [keyword3]".equals(s.getMessage())),
                "Expected parsing error status msg");
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "[keyword3] is not a valid conversion word".equals(s.getMessage())),
                "Expected parsing error status msg");
    }

    @Test
    void testCreateCompositeConverterCompositeNodeFromClassname() {
        CompositeNode node = mockCompositeNode("composite2");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof BlueCompositeConverter);
    }
    @Test
    void testCreateCompositeConverterNodeFromClassnameThatDoesNotExist() {
        CompositeNode node = mockCompositeNode("composite4");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof LiteralConverter);
        assertEquals("%PARSER_ERROR[composite4]", ((LiteralConverter<ILoggingEvent>)converter).convert(null));
        List<Status> copyOfStatusList = parser.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to instantiate converter class [org.apache.sling.NotValid] as a composite converter for keyword [composite4]".equals(s.getMessage())),
                "Expected parsing error status msg");
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to create converter for [%composite4] keyword".equals(s.getMessage())),
                "Expected parsing error status msg");
    }
    @Test
    void testCreateCompositeConverterNodeFromNullConverter() {
        CompositeNode node = mockCompositeNode("composite5");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof LiteralConverter);
        assertEquals("%PARSER_ERROR[composite5]", ((LiteralConverter<ILoggingEvent>)converter).convert(null));
        List<Status> copyOfStatusList = parser.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "There is no conversion registered for composite conversion word [composite5]".equals(s.getMessage())),
                "Expected parsing error status msg");
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to create converter for [%composite5] keyword".equals(s.getMessage())),
                "Expected parsing error status msg");
    }
    @Test
    void testCreateCompositeConverterCompositeNodeFromSupplier() {
        CompositeNode node = mockCompositeNode("composite1");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof BlueCompositeConverter);
    }
    @Test
    void testCreateCompositeConverterCompositeNodeFromSupplierThatReturnsWrongType() {
        CompositeNode node = mockCompositeNode("composite3");
        Converter<ILoggingEvent> converter = parser.compile(node, converterMap, converterSupplierMap);
        assertTrue(converter instanceof LiteralConverter);
        assertEquals("%PARSER_ERROR[composite3]", ((LiteralConverter<ILoggingEvent>)converter).convert(null));
        List<Status> copyOfStatusList = parser.getStatusManager().getCopyOfStatusList();
        assertNotNull(copyOfStatusList);
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to supply composite converter for keyword [composite3]".equals(s.getMessage())),
                "Expected parsing error status msg");
        assertTrue(copyOfStatusList.stream()
                    .anyMatch(s -> "Failed to create converter for [%composite3] keyword".equals(s.getMessage())),
                "Expected parsing error status msg");
    }

    protected CompositeNode mockCompositeNode(String value) {
        CompositeNode node = null;
        try {
            Constructor<CompositeNode> declaredConstructor = CompositeNode.class.getDeclaredConstructor(String.class);
            declaredConstructor.setAccessible(true);
            node = declaredConstructor.newInstance(value);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            fail("Failed co create composite node");
        }
        return node;
    }

    protected SimpleKeywordNode mockSimpleKeywordNode(String value) {
        SimpleKeywordNode node = null;
        try {
            Constructor<SimpleKeywordNode> declaredConstructor = SimpleKeywordNode.class.getDeclaredConstructor(Object.class);
            declaredConstructor.setAccessible(true);
            node = declaredConstructor.newInstance(value);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            fail("Failed co create composite node");
        }
        return node;
    }

}
