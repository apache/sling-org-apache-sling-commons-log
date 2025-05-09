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

import java.io.PrintWriter;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.sling.commons.log.logback.internal.Tailer.TailerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation if TailerListener that filters the lines
 * based on matching a regex pattern
 */
class FilteringListener implements TailerListener {
    public static final String MATCH_ALL = "*";
    private final Pattern pattern;
    private final String regex;
    private final PrintWriter pw;

    /**
     * Constructs a FilteringListener which uses regex pattern to
     * determine which lines need to be included
     *
     * @param pw writer to write the tailed line
     * @param regex pattern used to filter line. If null or "*"
     *              then all lines would be included. Regex can be simple
     *              string also. In that case search would be done in a
     *              case insensitive way
     */
    public FilteringListener(@NotNull PrintWriter pw, @Nullable String regex) {
        this.pw = pw;
        this.regex = regex != null ? regex.toLowerCase(Locale.ENGLISH) : null;
        this.pattern = createPattern(regex);
    }

    /**
     * Writes the line to the print writer if {@link #include(String)}
     * returns true
     */
    @Override
    public void handle(@NotNull String line) {
        if (include(line)) {
            pw.println(line);
        }
    }

    /**
     * Checks if the line should be included in the output
     *
     * @param line the line to check
     * @return true to include the line, false otherwise
     */
    private boolean include(@NotNull String line) {
        if (pattern == null) {
            return true;
        }

        String lc = line.toLowerCase(Locale.ENGLISH);
        if (lc.contains(regex)) {
            return true;
        }
        return pattern.matcher(line).matches();
    }

    /**
     * Creates the pattern to match lines against
     *
     * @param regex the regex (null or * will match all)
     * @return
     */
    private static Pattern createPattern(@Nullable String regex) {
        if (regex == null || MATCH_ALL.equals(regex)) {
            return null;
        }
        return Pattern.compile(regex);
    }
}
