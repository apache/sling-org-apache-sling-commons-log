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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.apache.sling.commons.log.logback.internal.util.SlingContextUtil;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The <code>LogWriter</code> class encapsulates the OSGi configuration for a
 * log writer and provides methods to access these to create an Appender.
 */
public class LogWriter {

    /**
     * Special fileName for which Console Appender would be created
     */
    public static final String FILE_NAME_CONSOLE = "CONSOLE";

    private static final long FACTOR_KB = 1024;

    private static final long FACTOR_MB = 1024 * FACTOR_KB;

    private static final long FACTOR_GB = 1024 * FACTOR_MB;

    /**
     * Regular expression matching a maximum file size specification. This
     * pattern case-insensitively matches a number and an optional factor
     * specifier of the forms k, kb, m, mb, g, or gb.
     */
    private static final Pattern SIZE_SPEC = Pattern.compile("([\\d]+)([kmg]b?)?", Pattern.CASE_INSENSITIVE);

    /**
     * The PID of the configuration from which this instance has been
     * configured. If this is <code>null</code> this instance is an implicitly
     * created instance which is not tied to any configuration.
     */
    private final String configurationPID;

    private final String fileName;

    private final int logNumber;

    private final String logRotation;

    private final String appenderName;

    private final boolean bufferedLogging;

    public LogWriter(
            @Nullable String configurationPID,
            @NotNull String appenderName,
            int logNumber,
            @Nullable String logRotation,
            @Nullable String fileName,
            boolean bufferedLogging) {
        this.appenderName = appenderName;
        if (fileName == null || fileName.length() == 0) {
            fileName = FILE_NAME_CONSOLE;
        }

        if (logNumber < 0) {
            logNumber = LogConstants.LOG_FILE_NUMBER_DEFAULT;
        }

        if (logRotation == null || logRotation.length() == 0) {
            logRotation = LogConstants.LOG_FILE_SIZE_DEFAULT;
        }

        this.configurationPID = configurationPID;
        this.fileName = fileName;
        this.logNumber = logNumber;
        this.logRotation = logRotation;
        this.bufferedLogging = bufferedLogging;
    }

    public LogWriter(
            @NotNull String appenderName, @Nullable String fileName, int logNumber, @Nullable String logRotation) {
        this(null, appenderName, logNumber, logRotation, fileName, false);
    }

    public @Nullable String getConfigurationPID() {
        return configurationPID;
    }

    public @NotNull String getImplicitConfigPID() {
        return LogConstants.PID;
    }

    public @NotNull String getFileName() {
        return fileName;
    }

    public @NotNull String getAppenderName() {
        return appenderName;
    }

    public int getLogNumber() {
        return logNumber;
    }

    public @NotNull String getLogRotation() {
        return logRotation;
    }

    public boolean isImplicit() {
        return configurationPID == null;
    }

    public @NotNull Appender<ILoggingEvent> createAppender(
            @NotNull final Context context, @NotNull final Encoder<ILoggingEvent> encoder) {
        SlingContextUtil ctxUtil = new SlingContextUtil(context, this);
        OutputStreamAppender<ILoggingEvent> appender;
        if (FILE_NAME_CONSOLE.equals(fileName)) {
            appender = new ConsoleAppender<>();
            appender.setName(FILE_NAME_CONSOLE);
        } else {
            ctxUtil.addInfo("Configuring appender " + getFileName());

            SlingRollingFileAppender<ILoggingEvent> rollingAppender = new SlingRollingFileAppender<>();
            rollingAppender.setAppend(true);
            rollingAppender.setFile(getFileName());

            Matcher sizeMatcher = SIZE_SPEC.matcher(getLogRotation());
            if (sizeMatcher.matches()) {
                // group 1 is the base size and is an integer number
                final long baseSize = Long.parseLong(sizeMatcher.group(1));

                // this will take the final size value
                final long maxSize;

                // group 2 is optional and is the size spec. If not null it is
                // at least one character long and the first character is enough
                // for use to know (the second is of no use here)
                String factorString = sizeMatcher.group(2);
                if (factorString == null) {
                    // no factor define, hence no multiplication
                    factorString = "d"; // triggers the 'default' case
                }
                switch (factorString.charAt(0)) {
                    case 'k':
                    case 'K':
                        maxSize = baseSize * FACTOR_KB;
                        break;
                    case 'm':
                    case 'M':
                        maxSize = baseSize * FACTOR_MB;
                        break;
                    case 'g':
                    case 'G':
                        maxSize = baseSize * FACTOR_GB;
                        break;
                    default:
                        // no factor define, hence no multiplication
                        maxSize = baseSize;
                }

                SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
                triggeringPolicy.setMaxFileSize(FileSize.valueOf(String.valueOf(maxSize)));
                triggeringPolicy.setContext(context);
                triggeringPolicy.start();
                rollingAppender.setTriggeringPolicy(triggeringPolicy);

                FixedWindowRollingPolicy pol = new FixedWindowRollingPolicy() {
                    @Override
                    protected int getMaxWindowSize() {
                        return Integer.MAX_VALUE;
                    }
                };
                pol.setMinIndex(1);
                pol.setMaxIndex(getLogNumber());
                pol.setFileNamePattern(getFileName() + "%i");
                pol.setContext(context);
                pol.setParent(rollingAppender);
                pol.start();
                rollingAppender.setRollingPolicy(pol);
            } else {
                TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
                String fileNamePattern = createFileNamePattern(getFileName(), getLogRotation());
                policy.setFileNamePattern(fileNamePattern);
                policy.setMaxHistory(getLogNumber());
                policy.setContext(context);
                policy.setParent(rollingAppender);
                policy.start();
                rollingAppender.setTriggeringPolicy(policy);

                ctxUtil.addInfo("Configured TimeBasedRollingPolicy with pattern " + fileNamePattern);
            }

            rollingAppender.setLogWriter(this);
            rollingAppender.setName(getAppenderName());

            appender = rollingAppender;
        }

        if (bufferedLogging && encoder instanceof LayoutWrappingEncoder) {
            ((LayoutWrappingEncoder<ILoggingEvent>) encoder).setImmediateFlush(false);
            ctxUtil.addInfo("Setting immediateFlush to false");
        } else {
            ctxUtil.addInfo("immediateFlush property not modified. Defaults to true");
        }

        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.start();

        ctxUtil.addInfo("Completed configuring appender with name " + getFileName());

        return appender;
    }

    public static @NotNull String createFileNamePattern(@NotNull String fileName, @NotNull String pattern) {
        // Default file name pattern "'.'yyyy-MM-dd"
        // http://sling.apache.org/site/logging.html#Logging-ScheduledRotation
        if (pattern.startsWith("'.'")) {
            pattern = pattern.substring(3); // 3 = '.' length
            pattern = ".%d{" + pattern + "}";
        }

        // Legacy pattern which does not start with '.' Just wrap them
        if (!pattern.contains("%d{")) {
            pattern = "%d{" + pattern + "}";
        }
        return fileName + pattern;
    }

    @Override
    public @NotNull String toString() {
        return "LogWriter{" + "configurationPID='" + configurationPID + '\'' + ", fileName='" + fileName + '\''
                + ", logNumber=" + logNumber + ", logRotation='" + logRotation + '\'' + '}';
    }
}
