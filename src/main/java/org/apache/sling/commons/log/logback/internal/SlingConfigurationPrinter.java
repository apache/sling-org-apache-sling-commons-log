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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.CachingDateFormatter;

/**
 * The <code>SlingConfigurationPrinter</code> is an Apache Felix Web Console
 * plugin to display the currently configured log files.
 */
public class SlingConfigurationPrinter {
    private static final CachingDateFormatter dateFormatter = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss");
    protected static final String MODE_ZIP = "zip";
    private final LogConfigManager logConfigManager;

    public SlingConfigurationPrinter(@NotNull LogConfigManager logConfigManager) {
        this.logConfigManager = logConfigManager;
    }

    /**
     * org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter printWriter, String mode) {
        Collection<Appender<ILoggingEvent>> allAppenders = logConfigManager.getAllKnownAppenders().values();
        dumpLogFileSummary(printWriter, allAppenders);

        if (!MODE_ZIP.equals(mode)) {
            int numOfLines = getNumOfLines();
            Tailer tailer = new Tailer(printWriter, numOfLines);

            for (Appender<ILoggingEvent> appender : allAppenders) {
                if (appender instanceof FileAppender) {
                    final File file = new File(((FileAppender<ILoggingEvent>) appender).getFile());
                    if (file.exists()) {
                        printWriter.print("Log file ");
                        printWriter.println(file.getAbsolutePath());
                        printWriter.println("--------------------------------------------------");
                        if (numOfLines < 0) {
                            includeWholeFile(printWriter, file);
                        } else {
                            try {
                                tailer.tail(file);
                            } catch (IOException e) {
                                logConfigManager.internalFailure("Error occurred " +
                                        "while processing log file " + file, e);
                            }
                        }
                        printWriter.println();
                    }
                }
            }
        }

        dumpLogbackStatus(printWriter);
    }

    static void includeWholeFile(PrintWriter printWriter, File file) {
        try (FileReader fr = new FileReader(file)) {
            final char[] buffer = new char[512];
            int len;
            while ((len = fr.read(buffer)) != -1) {
                printWriter.write(buffer, 0, len);
            }
        } catch (IOException ignore) {
            // we just ignore this
        }
    }

    private void dumpLogFileSummary(PrintWriter pw, Collection<Appender<ILoggingEvent>> appenders) {
        pw.println("Summary");
        pw.println("=======");
        pw.println();
        int counter = 0;
        final String rootDir = logConfigManager.getRootDir();
        for (Appender<ILoggingEvent> appender : appenders) {
            if (appender instanceof FileAppender) {
                File file = new File(((FileAppender<ILoggingEvent>)appender).getFile());
                final File dir = file.getParentFile();
                final String baseName = file.getName();
                String absolutePath = dir.getAbsolutePath();
                String displayName = ((FileAppender<ILoggingEvent>)appender).getFile();
                if (absolutePath.startsWith(rootDir)) {
                    displayName = baseName;
                }
                pw.printf("%d. %s %n", ++counter, displayName);
                final File[] files = getRotatedFiles((FileAppender<ILoggingEvent>)appender, -1);
                for (File f : files) {
                    pw.printf("  - %s, %s, %s %n", f.getName(), humanReadableByteCount(f.length()), getModifiedDate(f));
                }
            }
        }
        pw.println();
    }

    /**
     * Attempts to determine all log files created even via rotation.
     * if some complex rotation logic is used where rotated file get different names
     * or get created in different directory then those files would not be
     * included
     *
     * org.apache.felix.webconsole.AttachmentProvider#getAttachments(String)
     */
    public @Nullable URL[] getAttachments(String mode) {
        // we only provide urls for mode zip
        if (MODE_ZIP.equals(mode)) {
            final List<URL> urls = new ArrayList<>();
            for (Appender<ILoggingEvent> appender : logConfigManager.getAllKnownAppenders().values()) {
                if (appender instanceof FileAppender) {
                    final File[] files = getRotatedFiles((FileAppender<ILoggingEvent>)appender, getMaxOldFileCount());
                    for (File f : files) {
                        maybeAddToUrlsList(urls, f);
                    }
                }
            }
            if (!urls.isEmpty()) {
                return urls.toArray(URL[]::new);
            }
        }
        return null;
    }

    /**
     * add the URL of the file to the list if it is not 
     * a malformed url
     *
     * @param urls the list to add to
     * @param f the file to process
     */
    protected void maybeAddToUrlsList(final List<URL> urls, File f) {
        try {
            urls.add(f.toURI().toURL());
        } catch (MalformedURLException mue) {
            // we just ignore this file then
        }
    }

    /**
     * @param app appender instance
     * @param maxOldFileCount -1 if all files need to be included. Otherwise max
     *                        old files to include
     * @return sorted array of files generated by passed appender
     */
    protected File[] getRotatedFiles(FileAppender<ILoggingEvent> app, int maxOldFileCount) {
        final File file = new File(app.getFile());

        //If RollingFileAppender then make an attempt to list files
        //This might not work in all cases if complex rolling patterns
        //are used in Logback
        if (app instanceof RollingFileAppender) {
            final File dir = file.getParentFile();
            final String baseName = file.getName();
            File[] result = dir.listFiles((d, name)-> name.startsWith(baseName));

            //Sort the files in reverse
            Arrays.sort(result, Collections.reverseOrder(Comparator.comparing(File::lastModified)));

            if (maxOldFileCount > 0) {
                int maxCount = Math.min(getMaxOldFileCount(), result.length);
                if (maxCount < result.length) {
                    File[] resultCopy = new File[maxCount];
                    System.arraycopy(result, 0, resultCopy, 0, maxCount);
                    return resultCopy;
                }
            }
            return result;
        }

        //Not a RollingFileAppender then just return the actual file
        return new File[]{file};
    }

    private int getNumOfLines(){
        return logConfigManager.getNumOfLines();
    }

    private int getMaxOldFileCount(){
        return logConfigManager.getMaxOldFileCount();
    }

    private void dumpLogbackStatus(PrintWriter pw) {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        List<Status> statusList = loggerContext.getStatusManager().getCopyOfStatusList();
        pw.println("Logback Status");
        pw.println("--------------------------------------------------");
        for (Status s : statusList) {
            pw.printf("%s *%s* %s - %s %n",
                    dateFormatter.format(s.getTimestamp()),
                    statusLevelAsString(s),
                    abbreviatedOrigin(s),
                    s.getMessage());
            if (s.getThrowable() != null) {
                s.getThrowable().printStackTrace(pw);
            }
        }

        pw.println();
    }

    static String abbreviatedOrigin(Status s) {
        Object o = s.getOrigin();
        if (o == null) {
            return null;
        }
        String fqClassName = o.getClass().getName();
        int lastIndex = fqClassName.lastIndexOf(CoreConstants.DOT);
        if (lastIndex != -1) {
            return fqClassName.substring(lastIndex + 1, fqClassName.length());
        } else {
            return fqClassName;
        }
    }

    protected String statusLevelAsString(Status s) {
        String statusLevel;
        int effectiveLevel = s.getEffectiveLevel();
        switch (effectiveLevel) {
            case Status.INFO:
                statusLevel = "INFO";
                break;
            case Status.WARN:
                statusLevel = "WARN";
                break;
            case Status.ERROR:
                statusLevel = "ERROR";
                break;
             default:
                 statusLevel = null;
                 break;
        }
        return statusLevel;
    }

    /**
     * Returns a human-readable version of the file size, where the input represents
     * a specific number of bytes. Based on http://stackoverflow.com/a/3758880/1035417
     */
    protected String humanReadableByteCount(long bytes) {
        if (bytes < 0) {
            return "0";
        }
        int unit = 1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "kMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    protected String getModifiedDate(File f) {
        long modified = f.lastModified();
        if (modified == 0) {
            return "UNKNOWN";
        }
        return dateFormatter.format(modified);
    }
}
