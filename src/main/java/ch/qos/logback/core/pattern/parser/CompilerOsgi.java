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
package ch.qos.logback.core.pattern.parser;

import java.util.Map;
import java.util.function.Supplier;

import ch.qos.logback.core.pattern.CompositeConverter;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.util.OptionHelper;

class CompilerOsgi<E> extends Compiler<E> {

    // converters whose value is a supplier function
    final Map<String, Supplier<Converter<E>>> converterSupplierMap;

    CompilerOsgi(final Node top, final Map<String, String> converterMap, Map<String, Supplier<Converter<E>>> converterSupplierMap) {
        super(top, converterMap);
        this.converterSupplierMap = converterSupplierMap;
    }

    /**
     * Attempt to create a converter using the information found in 'converterMap'.
     *
     * @param kn
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    DynamicConverter<E> createConverter(SimpleKeywordNode kn) {
        DynamicConverter<E> converter = null;
        String keyword = (String) kn.getValue();
        Supplier<Converter<E>> supplier = converterSupplierMap.get(keyword);
        if (supplier != null) {
            // get the value from the supplier
            Converter<E> suppliedValue = supplier.get();
            if (suppliedValue instanceof DynamicConverter) {
                converter = (DynamicConverter<E>)suppliedValue;
            } else {
                addError("Failed to supply converter for keyword [" + keyword + "]");
            }
        } else {
            // create the value from the className
            String converterClassStr = converterMap.get(keyword);
            if (converterClassStr != null) {
                try {
                    converter = (DynamicConverter<E>) OptionHelper.instantiateByClassName(converterClassStr,
                            DynamicConverter.class, context);
                } catch (Exception e) {
                    addError("Failed to instantiate converter class [" + converterClassStr + "] for keyword [" + keyword
                            + "]", e);
                }
            } else {
                addError("There is no conversion registered for conversion word [" + keyword + "]");
            }
        }
        return converter;
    }

    /**
     * Attempt to create a converter using the information found in
     * 'compositeConverterMap'.
     *
     * @param cn
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    CompositeConverter<E> createCompositeConverter(CompositeNode cn) {
        CompositeConverter<E> converter = null;
        String keyword = (String) cn.getValue();
        Supplier<Converter<E>> supplier = converterSupplierMap.get(keyword);
        if (supplier != null) {
            // get the value from the supplier
            Converter<E> suppliedValue = supplier.get();
            if (suppliedValue instanceof CompositeConverter) {
                converter = (CompositeConverter<E>)suppliedValue;
            } else {
                addError("Failed to supply composite converter for keyword [" + keyword + "]");
            }
        } else {
            // create the value from the className
            String converterClassStr = converterMap.get(keyword);
            if (converterClassStr != null) {
                try {
                    converter = (CompositeConverter<E>) OptionHelper.instantiateByClassName(converterClassStr,
                            CompositeConverter.class, context);
                } catch (Exception e) {
                    addError("Failed to instantiate converter class [" + converterClassStr
                            + "] as a composite converter for keyword [" + keyword + "]", e);
                }
            } else {
                addError("There is no conversion registered for composite conversion word [" + keyword + "]");
            }
        }
        return converter;
    }

}