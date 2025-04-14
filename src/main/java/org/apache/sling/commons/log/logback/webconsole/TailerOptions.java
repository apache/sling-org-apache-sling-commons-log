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
package org.apache.sling.commons.log.logback.webconsole;

/**
 * Encapsulates the options for tailing a log file
 */
public final class TailerOptions {
    private final int numOfLines;
    private final String regex;

    /**
     * Constructor
     *
     * @param numOfLines the number of lines to include (or a negative number for all)
     * @param regex pattern used to filter line. If null or "*"
     *              then all lines would be included. Regex can be simple
     *              string also. In that case search would be done in a
     *              case insensitive way
     */
    public TailerOptions(int numOfLines, String regex) {
        this.numOfLines = numOfLines;
        this.regex = regex;
    }

    /**
     * Returns if all lines should be included
     *
     * @return true to tail all lines or false otherwise
     */
    public boolean tailAll() {
        return numOfLines < 0;
    }

    /**
     * Get the number of lines to include
     *
     * @return the number of lines to include (or a negative number for all)
     */
    public int getNumOfLines() {
        return numOfLines;
    }

    /**
     * Gets the pattern to match
     *
     * @return pattern used to filter line. If null or "*"
     *              then all lines would be included. Regex can be simple
     *              string also. In that case search would be done in a
     *              case insensitive way
     */
    public String getRegex() {
        return regex;
    }
}
