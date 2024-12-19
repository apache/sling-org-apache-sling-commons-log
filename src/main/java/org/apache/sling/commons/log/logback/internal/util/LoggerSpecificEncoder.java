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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.commons.log.logback.internal.LogConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

/**
 * Pattern layout encoder for specific loggers
 */
public class LoggerSpecificEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
    private Map<String, Layout<ILoggingEvent>> layoutByCategory = new ConcurrentHashMap<>();
    private final Layout<ILoggingEvent> defaultLayout;

    /**
     * Constructor
     *
     * @param defaultLayout the default layout the encoder is for if no better match is found
     */
    public LoggerSpecificEncoder(Layout<ILoggingEvent> defaultLayout) {
        this.defaultLayout = defaultLayout;
    }

    /**
     * Encodes the text for the event
     *
     * @param event the event to encode
     */
    @Override
    public byte[] encode(ILoggingEvent event) {
        String txt = getLayout(event.getLoggerName()).doLayout(event);
        return convertToBytes(txt);
    }

    /**
     * Get layout for the supplied logger
     *
     * @param loggerName the name of the logger to match
     * @return the found layout or the default layout if not found
     */
    private @NotNull Layout<ILoggingEvent> getLayout(@NotNull String loggerName) {
        String bestMatchLayoutKey = getBestMatchLayoutKey(loggerName);
        return layoutByCategory.getOrDefault(bestMatchLayoutKey, defaultLayout);
    }

    /**
     * Get the best match layout for the supplied logger
     *
     * @param loggerName the name of the logger to match
     * @return the key for the best match or the original loggerName if not found
     */
    private @NotNull String getBestMatchLayoutKey(@NotNull String loggerName) {
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

    /**
     * Covert the string to bytes using the current charset
     *
     * @param s the string to convert
     * @return the string as bytes
     */
    private byte[] convertToBytes(String s) {
        Charset charset = getCharset();
        if (charset == null) {
            return s.getBytes();
        } else {
            return s.getBytes(charset);
        }
    }

    /**
     * Add LogConfig which associates all the categories with
     * the layout
     *
     * @param config the config to process
     */
    public void addLogConfig(LogConfig config) {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Layout<ILoggingEvent> layout = config.createLayout(loggerContext);
        for (String category : config.getCategories()) {
            layoutByCategory.put(category, layout);
        }
    }

}
