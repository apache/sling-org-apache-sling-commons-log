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

/**
 *
 */
public class LogConstants {

    private LogConstants() {
        // to hide public ctor
    }

    /**
     * Framework property specifying if debug is enabled
     */
    public static final String DEBUG = "org.apache.sling.commons.log.debug";

    /**
     * The service vendor value for the OSGi services registered here
     */
    public static final String ASF_SERVICE_VENDOR = "Apache Software Foundation";

    /**
     * The service description value for the package info collector service
     */
    public static final String PACKAGE_INFO_COLLECTOR_DESC = "Sling Log Package Info Collector";

    /**
     * The event topic that receives a request to reset the log configuration
     */
    public static final String RESET_EVENT_TOPIC = "org/apache/sling/commons/log/RESET";

    /**
     * The last segment of the webconsole address for the Sling Log Configuration Printer
     */
    public static final String PRINTER_URL = "slinglogs";

    /**
     * System property key for JUL config class
     */
    public static final String SYSPROP_JAVA_UTIL_LOGGING_CONFIG_CLASS = "java.util.logging.config.class";

    /**
     * System property key for JUL config file
     */
    public static final String SYSPROP_JAVA_UTIL_LOGGING_CONFIG_FILE = "java.util.logging.config.file";

    /**
     * Framework property specifying the root location used to resolve relative paths
     */
    public static final String SLING_LOG_ROOT = "sling.log.root";

    /**
     * Framework property specifying the sling home location used to resolve relative paths
     */
    public static final String SLING_HOME = "sling.home";

    /**
     * Configuration key to specify if the SLF4JBridgeHandler should be installed
     */
    public static final String JUL_SUPPORT = "org.apache.sling.commons.log.julenabled";

    /**
     * Configuration key to specify if the logback packaging data should be enabled or not
     */
    public static final String LOG_PACKAGING_DATA = "org.apache.sling.commons.log.packagingDataEnabled";

    /**
     * Configuration key to specify the max stack data depth computed during caller data extraction
     */
    public static final String LOG_MAX_CALLER_DEPTH = "org.apache.sling.commons.log.maxCallerDataDepth";

    /**
     * Configuration key to specify the max number of rolled over files to included in the webconsole
     * configuration printer
     */
    public static final String PRINTER_MAX_INCLUDED_FILES = "org.apache.sling.commons.log.maxOldFileCountInDump";

    /**
     * Default value for the {@link #PRINTER_MAX_INCLUDED_FILES} configuration when no value is supplied
     */
    public static final int PRINTER_MAX_INCLUDED_FILES_DEFAULT = 3;

    /**
     * Configuration key to specify the number of log file lines to tail in the webconsole
     * configuration printer
     */
    public static final String PRINTER_NUM_OF_LINES = "org.apache.sling.commons.log.numOfLines";

    /**
     * Default value for the {@link #PRINTER_NUM_OF_LINES} configuration when no value is supplied
     */
    public static final int PRINTER_NUM_OF_LINES_DEFAULT = 1000;

    /**
     * Configuration key to specify if the log file writing is buffered
     */
    public static final String LOG_FILE_BUFFERED = "org.apache.sling.commons.log.file.buffered";

    /**
     * Configuration key for the pattern to apply for the logging output
     */
    public static final String LOG_PATTERN = "org.apache.sling.commons.log.pattern";

    /**
     * Configuration key for the file path of the logging output
     */
    public static final String LOG_FILE = "org.apache.sling.commons.log.file";

    /**
     * Configuration key for the file path of the logback xml file
     */
    public static final String LOGBACK_FILE = "org.apache.sling.commons.log.configurationFile";

    /**
     * Configuration key for the logging level to apply to the specified loggers
     */
    public static final String LOG_LEVEL = "org.apache.sling.commons.log.level";

    /**
     * Configuration key for the maximum number of archive files to keep
     */
    public static final String LOG_FILE_NUMBER = "org.apache.sling.commons.log.file.number";

    /**
     * Configuration key for the name of the rolled-over (archived) log files
     */
    public static final String LOG_FILE_SIZE = "org.apache.sling.commons.log.file.size";

    /**
     * Configuration key for the logging loggers to apply to configuration to
     */
    public static final String LOG_LOGGERS = "org.apache.sling.commons.log.names";

    /**
     * Configuration key for specifying if other appenders should log the message too
     */
    public static final String LOG_ADDITIV = "org.apache.sling.commons.log.additiv";

    /**
     * Appender name for the console appender that is active during a config reset
     */
    public static final String DEFAULT_CONSOLE_APPENDER_NAME = "org.apache.sling.commons.log.CONSOLE";

    /**
     * Configuration LOG_FILE value for sending the output to the console
     * instead of to a file
     */
    public static final String FILE_NAME_CONSOLE = "CONSOLE";

    /**
     * Default value for the LOG_LEVEL if no configuration is supplied
     */
    public static final String LOG_LEVEL_DEFAULT = "INFO";

    /**
     * {@link #LOG_LEVEL} value that indicates the log level should be set back to the default value
     */
    public static final String LOG_LEVEL_RESET_TO_DEFAULT = "DEFAULT";

    /**
     * Default value for {@link #LOG_FILE_NUMBER} when no specific value is supplied
     */
    public static final int LOG_FILE_NUMBER_DEFAULT = 5;

    /**
     * Default format for the LOG_FILE_SIZE value if no configuration is supplied
     */
    public static final String LOG_FILE_SIZE_DEFAULT = "'.'yyyy-MM-dd";

    /**
     * Default value for the LOG_PATTERN if no configuration is supplied
     */
    public static final String LOG_PATTERN_DEFAULT = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %msg%n";

    /**
     * Service PID for tracking the official (global) configuration
     */
    public static final String PID = "org.apache.sling.commons.log.LogManager";

    /**
     * Service PID for the factory that is tracking the other logger configurations
     */
    public static final String FACTORY_PID_CONFIGS = PID + ".factory.config";

    /**
     * Service PID for the factory that is tracking the other log writers
     */
    public static final String FACTORY_PID_WRITERS = PID + ".factory.writer";

    /**
     * Key for the LoggerContext prop value that contains the config pids set
     * for the last time the configuration was reset
     */
    public static final String CONFIG_PID_SET = "org.apache.sling.commons.log.ConfigPids";
}
