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

import java.io.IOException;
import java.io.PrintWriter;

import org.osgi.annotation.versioning.ProviderType;


/**
 * Interface for the Felix Web Console plugin that displays the
 * current active log bundle configuration
 */
@ProviderType
public interface LogPanel {
    /**
     * Request param name to control number of lines to include in the log
     */
    String PARAM_TAIL_NUM_OF_LINES = "tail";
    /**
     * Request param name for appender name
     */
    String PARAM_APPENDER_NAME = "name";

    /**
     * Request param capturing the regular expression to search
     */
    String PARAM_TAIL_GREP = "grep";
    /**
     * Let the path end with extension. In that case WebConsole logic would by pass this request's
     * response completely
     */
    String PATH_TAILER = "tailer.txt";

    /**
     * The app root
     */
    String APP_ROOT = "slinglog";

    /**
     * The base location for UI resources
     */
    String RES_LOC = APP_ROOT + "/res/ui";

    /**
     * Tails the content of the given appender to the supplied print writer
     *
     * @param pw the print writer to render to
     * @param appenderName the name of the appender to tail
     * @param options the options for what content to include in the output
     * @throws IOException if any failure rendering the tailed log file
     */
    void tail(PrintWriter pw, String appenderName, TailerOptions options) throws IOException;

    /**
     * Renders the logging configuration details to the supplied print writer
     *
     * @param pw the print writer to render to
     * @param consoleAppRoot the app root
     * @throws IOException if any failure rendering the logging configuration
     */
    void render(PrintWriter pw, String consoleAppRoot) throws IOException;

    /**
     * Deletes a logger configuration
     *
     * @param pid the pid of the configuration to delete
     */
    void deleteLoggerConfig(String pid);

    /**
     * Creates a logger configuration
     *
     * @param config the configuration details
     * @throws IOException if any failure applying the logger configuration
     */
    void createLoggerConfig(LoggerConfig config) throws IOException;
}
