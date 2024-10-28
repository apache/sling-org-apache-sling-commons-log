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

import java.util.Map;
import java.util.function.Supplier;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayoutOsgi;
import ch.qos.logback.classic.pattern.EnsureExceptionHandling;
import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

/**
 * Converter util to mask certain characters in messages
 */
public class MaskingMessageUtil {

    private MaskingMessageUtil() {
        // hide the public ctor
    }

    /**
     * Set the message converter for the layout
     * @param pl The layout
     */
    public static void setMessageConverter(final PatternLayoutOsgi pl) {
        // need to overwrite all converter for messages and exceptions
        // see https://logback.qos.ch/manual/layouts.html
        Map<String, Supplier<Converter<ILoggingEvent>>> instanceConverterSupplierMap = pl.getInstanceConverterSupplierMap();
        instanceConverterSupplierMap.put("m", MaskingMessageConverter::new);
        instanceConverterSupplierMap.put("msg", MaskingMessageConverter::new);
        instanceConverterSupplierMap.put("message", MaskingMessageConverter::new);

        instanceConverterSupplierMap.put("ex", MaskingThrowableProxyConverter::new);
        instanceConverterSupplierMap.put("exception", MaskingThrowableProxyConverter::new);
        instanceConverterSupplierMap.put("rEx", MaskingRootCauseFirstThrowableProxyConverter::new);
        instanceConverterSupplierMap.put("rootException", MaskingRootCauseFirstThrowableProxyConverter::new);
        instanceConverterSupplierMap.put("throwable", MaskingThrowableProxyConverter::new);

        instanceConverterSupplierMap.put("xEx", MaskingExtendedThrowableProxyConverter::new);
        instanceConverterSupplierMap.put("xException", MaskingExtendedThrowableProxyConverter::new);
        instanceConverterSupplierMap.put("xThrowable", MaskingExtendedThrowableProxyConverter::new);

        // override post processor for ensuring exception handling
        pl.setPostCompileProcessor(new MaskingEnsureExceptionHandling());
    }

    /**
     * Create an encoder with the default pattern
     * @param loggerContext Logging context
     * @return The encoder
     */
    public static Encoder<ILoggingEvent> getDefaultEncoder(final Context loggerContext) {
        final PatternLayoutEncoderBase<ILoggingEvent> encoder = new MaskingPatternLayoutEncoder();
        encoder.setPattern(LogConstants.LOG_PATTERN_DEFAULT);
        encoder.setContext(loggerContext);
        encoder.start();
        return encoder;
    }

    /**
     * Replace any carriage returns and line feeds with an underscore
     *
     * @param msg The message
     * @return converted string
     */
    static String mask(final String msg) {
         if ( msg == null ) {
             return null;
         }
         return msg.replace('\n', '_').replace('\r', '_');
    }

    /**
     * Override the MessageConverter to provide masking of the output
     */
    @SuppressWarnings("java:S110")
    public static final class MaskingMessageConverter extends MessageConverter {
        @Override
        public String convert(final ILoggingEvent event) {
            return mask(super.convert(event));
        }
    }

    /**
     * Override the ThrowableProxyConverter to provide masking of the output
     */
    @SuppressWarnings("java:S110")
    public static final class MaskingThrowableProxyConverter extends ThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(final IThrowableProxy tp) {
            return super.throwableProxyToString(new MaskingThrowableProxy(tp));
        }
    }

    /**
     * Override the RootCauseFirstThrowableProxyConverter to provide masking of the output
     */
    @SuppressWarnings("java:S110")
    public static final class MaskingRootCauseFirstThrowableProxyConverter extends RootCauseFirstThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(final IThrowableProxy tp) {
            return super.throwableProxyToString(new MaskingThrowableProxy(tp));
        }
    }

    /**
     * Override the ExtendedThrowableProxyConverter to provide masking of the output
     */
    @SuppressWarnings("java:S110")
    public static class MaskingExtendedThrowableProxyConverter extends ExtendedThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(final IThrowableProxy tp) {
            return super.throwableProxyToString(new MaskingThrowableProxy(tp));
        }
    }

    /**
     * An implementation of IThrowableProxy to provide masking of the output
     */
    public static final class MaskingThrowableProxy implements IThrowableProxy {
        private final IThrowableProxy proxied;

        /**
         * Constructor
         *
         * @param proxied the original throwable proxy object
         */
        public MaskingThrowableProxy(final IThrowableProxy proxied) {
            this.proxied = proxied;
        }

        @Override
        public String getMessage() {
            return mask(proxied.getMessage());
        }

        private IThrowableProxy getProxy(final IThrowableProxy p) {
            if (p == null) {
                return null;
            }
            if (p == proxied || p == this) {
                return this;
            }
            return new MaskingThrowableProxy(p);
        }

        @Override
        public IThrowableProxy getCause() {
            return getProxy(proxied.getCause());
        }

        @Override
        public String getClassName() {
            return proxied.getClassName();
        }

        @Override
        public StackTraceElementProxy[] getStackTraceElementProxyArray() {
            return proxied.getStackTraceElementProxyArray();
        }

        @Override
        public int getCommonFrames() {
            return proxied.getCommonFrames();
        }

        @Override
        public IThrowableProxy[] getSuppressed() {
            final IThrowableProxy[] result = proxied.getSuppressed();
            if (result == null || result.length == 0) {
                return result;
            }
            final IThrowableProxy[] proxies = new IThrowableProxy[result.length];
            for (int i = 0; i < proxies.length; i++) {
                proxies[i] = getProxy(result[i]);
            }
            return proxies;
        }

        @Override
        public boolean isCyclic() {
            return proxied.isCyclic();
        }
    }

    /**
     * Override the EnsureExceptionHandling to provide masking of the output
     */
    static final class MaskingEnsureExceptionHandling extends EnsureExceptionHandling {

        @Override
        public void process(Context context, Converter<ILoggingEvent> head) {
            if (head == null) {
                // this should never happen
                throw new IllegalArgumentException("cannot process empty chain");
            }
            if (!chainHandlesThrowable(head)) {
                Converter<ILoggingEvent> tail = ConverterUtil.findTail(head);
                Converter<ILoggingEvent> exConverter = null;
                LoggerContext loggerContext = (LoggerContext) context;
                if (loggerContext.isPackagingDataEnabled()) {
                    exConverter = new MaskingExtendedThrowableProxyConverter();
                } else {
                    exConverter = new MaskingThrowableProxyConverter();
                }
                tail.setNext(exConverter);
            }
        }
    }

    /**
     * Override the PatternLayoutEncoderBase to provide masking of the output
     */
    static final class MaskingPatternLayoutEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {

        @Override
        public void start() {
            PatternLayoutOsgi patternLayout = new PatternLayoutOsgi();
            patternLayout.setContext(context);
            patternLayout.setPattern(getPattern());
            patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
            MaskingMessageUtil.setMessageConverter(patternLayout);
            patternLayout.start();
            this.layout = patternLayout;
            super.start();
        }
    }

}

