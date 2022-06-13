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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
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
public class MaskingMessageUtil extends MessageConverter {

    /**
     * Set the message converter for the layout
     * @param pl The layout
     */
    public static void setMessageConverter(final PatternLayout pl) {
        // need to overwrite all converter for messages and exceptions
        // see https://logback.qos.ch/manual/layouts.html
        pl.getInstanceConverterMap().put("m", MaskingMessageConverter.class.getName());
        pl.getInstanceConverterMap().put("msg", MaskingMessageConverter.class.getName());
        pl.getInstanceConverterMap().put("message", MaskingMessageConverter.class.getName());

        pl.getInstanceConverterMap().put("ex", MaskingThrowableProxyConverter.class.getName());
        pl.getInstanceConverterMap().put("exception", MaskingThrowableProxyConverter.class.getName());
        pl.getInstanceConverterMap().put("rEx", MaskingRootCauseFirstThrowableProxyConverter.class.getName());
        pl.getInstanceConverterMap().put("rootException", MaskingRootCauseFirstThrowableProxyConverter.class.getName());
        pl.getInstanceConverterMap().put("throwable", MaskingThrowableProxyConverter.class.getName());

        pl.getInstanceConverterMap().put("xEx", MaskingExtendedThrowableProxyConverter.class.getName());
        pl.getInstanceConverterMap().put("xException", MaskingExtendedThrowableProxyConverter.class.getName());
        pl.getInstanceConverterMap().put("xThrowable", MaskingExtendedThrowableProxyConverter.class.getName());

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
        encoder.setPattern(LogConfigManager.LOG_PATTERN_DEFAULT);
        encoder.setContext(loggerContext);
        encoder.start();
        return encoder;
    }

    /**
     * Replace any carriage returns and line feeds with an underscore
     * @param msg The message
     * @return converted string
     */
    static String mask(final String msg) {
         if ( msg == null ) {
             return null;
         }
         return msg.replace('\n', '_').replace('\r', '_');
    }

    public static final class MaskingMessageConverter extends MessageConverter {
        @Override
        public String convert(final ILoggingEvent event) {
            return mask(super.convert(event));
        }
    }

    public static final class MaskingThrowableProxyConverter extends ThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(final IThrowableProxy tp) {
            return super.throwableProxyToString(new MaskingThrowableProxy(tp));
        }
    }

    public static final class MaskingRootCauseFirstThrowableProxyConverter extends RootCauseFirstThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(final IThrowableProxy tp) {
            return super.throwableProxyToString(new MaskingThrowableProxy(tp));
        }
    }

    public static class MaskingExtendedThrowableProxyConverter extends ExtendedThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(final IThrowableProxy tp) {
            return super.throwableProxyToString(new MaskingThrowableProxy(tp));
        }
    }

    public static final class MaskingThrowableProxy implements IThrowableProxy {
        private final IThrowableProxy proxied;

        public MaskingThrowableProxy(final IThrowableProxy proxied) {
            this.proxied = proxied;
        }

        @Override
        public String getMessage() {
            return mask(proxied.getMessage());
        }

        private IThrowableProxy getProxy(final IThrowableProxy p) {
            if ( p == null ) {
                return null;
            }
            if ( p == proxied || p == this ) {
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
            if ( result == null || result.length == 0 ) {
                return result;
            }
            final IThrowableProxy[] proxies = new IThrowableProxy[result.length];
            for(int i=0;i<proxies.length;i++) {
                proxies[i] = getProxy(result[i]);
            }
            return proxies;
        }
    }

    static final class MaskingEnsureExceptionHandling extends EnsureExceptionHandling {

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

    static final class MaskingPatternLayoutEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {

        @Override
        public void start() {
            PatternLayout patternLayout = new PatternLayout();
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

