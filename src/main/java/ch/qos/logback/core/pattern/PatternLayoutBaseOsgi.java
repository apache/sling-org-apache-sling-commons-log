/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.pattern;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.ParserOsgi;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusManager;

public abstract class PatternLayoutBaseOsgi<E> extends PatternLayoutBase<E> {

    // instance converters whose value is a supplier function
    Map<String, Supplier<Converter<E>>> instanceConverterSupplierMap = new HashMap<>();

    /**
     * Concrete implementations of this class may override this for elaborating the
     * mapping between pattern words and converters.
     * 
     * @return A map associating pattern words to the supplier of converter instances
     */
    public Map<String, Supplier<Converter<E>>> getDefaultConverterSupplierMap() {
        return Collections.emptyMap();
    }

    /**
     * Returns a map where the default converter supplier map is merged with the
     * instance converter supplier map
     */
    public Map<String, Supplier<Converter<E>>> getEffectiveConverterSupplierMap() {
        Map<String, Supplier<Converter<E>>> effectiveMap = new HashMap<>();

        // add the least specific map fist
        Map<String, Supplier<Converter<E>>> defaultMap = getDefaultConverterSupplierMap();
        if (defaultMap != null) {
            effectiveMap.putAll(defaultMap);
        }

        // set the most specific map last
        Map<String, Supplier<Converter<E>>> instanceMap = getInstanceConverterSupplierMap();
        if (instanceMap != null) {
            effectiveMap.putAll(instanceMap);
        }
        return effectiveMap;
    }

    @Override
    public void start() {
        if (pattern == null || pattern.length() == 0) {
            addError("Empty or null pattern.");
            return;
        }
        try {
            ParserOsgi<E> p = new ParserOsgi<>(pattern);
            if (getContext() != null) {
                p.setContext(getContext());
            }
            Node t = p.parse();
            this.head = p.compile(t, getEffectiveConverterMap(), getEffectiveConverterSupplierMap());
            if (postCompileProcessor != null) {
                postCompileProcessor.process(context, head);
            }
            ConverterUtil.setContextForConverters(getContext(), head);
            ConverterUtil.startConverters(this.head);
            started = true;
        } catch (ScanException sce) {
            StatusManager sm = getContext().getStatusManager();
            sm.add(new ErrorStatus("Failed to parse pattern \"" + getPattern() + "\".", this, sce));
        }
    }

    public Map<String, Supplier<Converter<E>>> getInstanceConverterSupplierMap() {
        return instanceConverterSupplierMap;
    }

}
