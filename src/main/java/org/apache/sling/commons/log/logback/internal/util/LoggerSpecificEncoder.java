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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

import org.apache.sling.commons.log.logback.internal.LogConfig;

public class LoggerSpecificEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
    private Map<String, Layout<ILoggingEvent>> layoutByCategory = new ConcurrentHashMap<String, Layout<ILoggingEvent>>();

    private final Layout<ILoggingEvent> defaultLayout;

    public LoggerSpecificEncoder(Layout<ILoggingEvent> defaultLayout) {
        this.defaultLayout = defaultLayout;
    }

    public byte[] encode(ILoggingEvent event) {
        String txt = getLayout(event.getLoggerName()).doLayout(event);
        return convertToBytes(txt);
    }

    private Layout<ILoggingEvent> getLayout(String loggerName) {
        String bestMatchLayoutKey = getBestMatchLayoutKey(loggerName);
        return layoutByCategory.getOrDefault(bestMatchLayoutKey, defaultLayout);
    }

    private String getBestMatchLayoutKey(String loggerName) {
        if (layoutByCategory.containsKey(loggerName)) {
            // fastpath for exact name match
            return loggerName;
        }
        String bestMatch = loggerName;
        int bestMatchLength = 0;
        for (String layoutKey : layoutByCategory.keySet()) {
            if (loggerName.startsWith(layoutKey) && loggerName.charAt(layoutKey.length()) == '.' && layoutKey.length() > bestMatchLength) {
                bestMatch = layoutKey;
                bestMatchLength = layoutKey.length();
            }
        }
        return bestMatch;
    }

    private byte[] convertToBytes(String s) {
        Charset charset = getCharset();
        if (charset == null) {
            return s.getBytes();
        } else {
            try {
                return s.getBytes(charset.name());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("An existing charset cannot possibly be unsupported.");
            }
        }
    }

    public void addLogConfig(LogConfig config) {
        Layout<ILoggingEvent> layout = config.createLayout();
        for (String category : config.getCategories()) {
            layoutByCategory.put(category, layout);
        }
    }
}
