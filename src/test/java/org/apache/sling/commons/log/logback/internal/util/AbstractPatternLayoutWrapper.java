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

package org.apache.sling.commons.log.logback.internal.util;

import java.util.Map;
import java.util.function.Supplier;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.PostCompileProcessor;
import ch.qos.logback.core.status.Status;

/**
 * Abstract wrapper for {@link PatternLayout} class. Can be extended to implement 'Decorator' design pattern.
 *
 * @apiNote This has probably no use outside of testing
 */
public abstract class AbstractPatternLayoutWrapper extends PatternLayout {

    protected final PatternLayout wrapped;

    protected AbstractPatternLayoutWrapper(final PatternLayout wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String doLayout(final ILoggingEvent event) {
        return wrapped.doLayout(event);
    }

    @Override
    public String getFileHeader() {
        return wrapped.getFileHeader();
    }

    @Override
    public String getPresentationHeader() {
        return wrapped.getPresentationHeader();
    }

    @Override
    public String getPresentationFooter() {
        return wrapped.getPresentationFooter();
    }

    @Override
    public String getFileFooter() {
        return wrapped.getFileFooter();
    }

    @Override
    public String getContentType() {
        return wrapped.getContentType();
    }

    @Override
    public void setContext(final Context context) {
        wrapped.setContext(context);
    }

    @Override
    public Context getContext() {
        return wrapped.getContext();
    }

    @Override
    public void addStatus(final Status status) {
        wrapped.addStatus(status);
    }

    @Override
    public void addInfo(final String s) {
        wrapped.addInfo(s);
    }

    @Override
    public void addInfo(final String msg, final Throwable ex) {
        wrapped.addInfo(msg, ex);
    }

    @Override
    public void addWarn(final String s) {
        wrapped.addWarn(s);
    }

    @Override
    public void addWarn(final String msg, final Throwable ex) {
        wrapped.addWarn(msg, ex);
    }

    @Override
    public void addError(final String s) {
        wrapped.addError(s);
    }

    @Override
    public void addError(final String msg, final Throwable ex) {
        wrapped.addError(msg, ex);
    }

    @Override
    public void start() {
        wrapped.start();
    }

    @Override
    public void stop() {
        wrapped.stop();
    }

    @Override
    public boolean isStarted() {
        return wrapped.isStarted();
    }

    @Deprecated
    @Override
    public Map<String, String> getDefaultConverterMap() {
        return wrapped.getDefaultConverterMap();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<String, Supplier<DynamicConverter>> getEffectiveConverterMap() {
        return wrapped.getEffectiveConverterMap();
    }

    @Override
    public void setPostCompileProcessor(PostCompileProcessor<ILoggingEvent> postCompileProcessor) {
        wrapped.setPostCompileProcessor(postCompileProcessor);
    }

    @Override
    public String getPattern() {
        return wrapped.getPattern();
    }

    @Override
    public void setPattern(String pattern) {
        wrapped.setPattern(pattern);
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<String, Supplier<DynamicConverter>> getInstanceConverterMap() {
        return wrapped.getInstanceConverterMap();
    }

    @Override
    public boolean isOutputPatternAsHeader() {
        return wrapped.isOutputPatternAsHeader();
    }

    @Override
    public void setOutputPatternAsHeader(boolean outputPatternAsHeader) {
        wrapped.isOutputPatternAsHeader();
    }
}
