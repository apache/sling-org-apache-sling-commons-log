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

import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.util.IEscapeUtil;
import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import ch.qos.logback.core.spi.ScanException;

// ~=lambda
// E = TE|T

// Left factorization
// E = T(E|~)
// Eopt = E|~
// replace E|~ with Eopt in E
// E = TEopt

// T = LITERAL | '%' C | '%' FORMAT_MODIFIER C
// C = SIMPLE_KEYWORD OPTION | COMPOSITE_KEYWORD COMPOSITE
// OPTION = {...} | ~
// COMPOSITE = E ')' OPTION

public class ParserOsgi<E> extends Parser<E> {

    ParserOsgi(TokenStream ts) throws ScanException {
        super(ts);
    }

    public ParserOsgi(String pattern) throws ScanException {
        this(pattern, new RegularEscapeUtil());
    }

    public ParserOsgi(String pattern, IEscapeUtil escapeUtil) throws ScanException {
        super(pattern, escapeUtil);
    }

    /**
     * When the parsing step is done, the Node list can be transformed into a
     * converter chain.
     *
     * @param top
     * @param converterMap
     * @param converterSupplierMap
     * @return
     */
    public Converter<E> compile(final Node top, Map<String, String> converterMap,
            Map<String, Supplier<Converter<E>>> converterSupplierMap) {
        CompilerOsgi<E> compiler = new CompilerOsgi<>(top, converterMap, converterSupplierMap);
        compiler.setContext(context);
        // compiler.setStatusManager(statusManager);
        return compiler.compile();
    }

}