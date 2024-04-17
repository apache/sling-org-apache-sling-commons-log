/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.log.logback.internal;

import static ch.qos.logback.core.spi.ConfigurationEvent.newConfigurationEndedEvent;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.sling.commons.log.logback.internal.AppenderTracker.AppenderInfo;
import org.apache.sling.commons.log.logback.internal.config.ConfigAdminSupport;
import org.apache.sling.commons.log.logback.internal.config.ConfigurationException;
import org.apache.sling.commons.log.logback.internal.joran.JoranConfiguratorWrapper;
import org.apache.sling.commons.log.logback.internal.stacktrace.OSGiAwareExceptionHandling;
import org.apache.sling.commons.log.logback.internal.stacktrace.PackageInfoCollector;
import org.apache.sling.commons.log.logback.internal.util.LoggerSpecificEncoder;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.apache.sling.commons.log.logback.internal.util.SlingStatusPrinter;
import org.apache.sling.commons.log.logback.spi.DefaultConfigurator;
import org.apache.sling.commons.log.logback.webconsole.LogPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextAwareBase;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelUtil;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusListenerAsList;
import ch.qos.logback.core.status.StatusUtil;

/**
 * Logging configuration customization manager
 */
public class LogConfigManager extends LoggerContextAwareBase implements LogbackResetListener, LogConfig.LogWriterProvider {

    /**
     * The name for the logger context
     */
    private static final String CONTEXT_NAME = "sling";

    /**
     * The bundle context supplied to the ctor
     */
    private BundleContext bundleContext;

    /**
     * Reference to the current LoggerContext
     */
    private final LoggerContext loggerContext;

    // map of log writers indexed by configuration PID
    private final Map<String, LogWriter> writerByPid;

    // map of log writers indexed by (absolute) file name. This map does
    // not contain writers writing to standard out
    private final Map<String, LogWriter> writerByFileName;

    /**
     * Map to track which custom appenders have been added. The key is the origin
     * where the appender was defined and the value is a map whose key is the appender
     * name and the value is a set of logger names that the appender was added to.
     */
    private Map<AppenderOrigin, Map<String, Set<String>>> appendersByOrigin = new ConcurrentHashMap<>();

    // map of log configurations by configuration PID
    private final Map<String, LogConfig> configByPid;

    // map of log configurations by the categories they are configured with
    private final Map<String, LogConfig> configByCategory;

    /**
     * the root folder to make relative writer paths absolute
     */
    private File rootDir;

    /**
     * global default configuration (from BundleContext properties)
     */
    private Dictionary<String, String> defaultConfiguration;

    /**
     * Helper to register the configadmin related services
     */
    private final ConfigAdminSupport configAdminSupport;

    /**
     * The logger for this class
     */
    private final org.slf4j.Logger logger;

    /**
     * The (optional) xml file that contains the logback configuration to start with
     */
    private File logbackConfigFile;

    /**
     * The current value of the {@link #LOG_PACKAGING_DATA} configuration value
     */
    private boolean packagingDataEnabled;

    /**
     * The current value of the {@link #LOG_MAX_CALLER_DEPTH} configuration value
     */
    private int maxCallerDataDepth;

    /**
     * The current value of the {@link #PRINTER_MAX_INCLUDED_FILES} configuration value
     */
    private int maxOldFileCount;

    /**
     * The current value of the {@link #PRINTER_NUM_OF_LINES} configuration value
     */
    private int numOfLines;

    /**
     * List of reset handlers to invoke when resetting the logging configuration
     */
    private final List<LogbackResetListener> resetListeners = new ArrayList<>();

    /**
     * Acts as a bridge between Logback and OSGi
     */
    private final LoggerContextListener osgiIntegrationListener;

    /**
     * Flag to indicate if debug is enabled
     */
    private final boolean debug;

    /**
     * Flag to indicate if log config has been started
     */
    private boolean started;

    /**
     * Lock for ensureing no concurrent configuration reload
     */
    private final Semaphore resetLock = new Semaphore(1);

    /**
     * Lock object for reload locking
     */
    private final Object configChangedFlagLock = new Object();

    /**
     * Flag to indicate if the config has changed and reload is needed
     */
    private boolean configChanged = false;

    /**
     * Tracker for Appender services
     */
    private AppenderTracker appenderTracker;

    /**
     * Tracker for ConfigSource services
     */
    private ConfigSourceTracker configSourceTracker;

    /**
     * Tracker for Filter services
     */
    private FilterTracker filterTracker;

    /**
     * Tracker for TurboFilter services
     */
    private TurboFilterTracker turboFilterTracker;

    /**
     * Reference to service registrations that should be unregistered during {@link #stop()}
     */
    private final List<ServiceRegistration<?>> registrations = new ArrayList<>();

    /**
     * References to service trackers that should be closed during {@link #stop()}
     */
    private final List<ServiceTracker<?, ?>> serviceTrackers = new ArrayList<>();

    /**
     * Flag to track if the SLF4JBridgeHandler was installed or not
     */
    boolean bridgeHandlerInstalled = false;

    /**
     * The active package info WeavingHook to support the {@link #LOG_PACKAGING_DATA} capability
     */
    private final PackageInfoCollector packageInfoCollector = new PackageInfoCollector();

    /**
     * Time at which reset started. Used as the threshold for logging error
     * messages from status printer
     */
    volatile long resetStartTime;

    /**
     * Helper to convert the config options to the expected types
     */
    private Converter converter;

    /**
     * Logs a message an optional stack trace to error output. This method is
     * used by the logging system in case of errors writing to the correct
     * logging output.
     */
    public void internalFailure(@NotNull String message, @Nullable Throwable t) {
        if (t != null) {
            addError(message, t);
        } else {
            addError(message);
        }

        // log the message to error stream also
        System.err.println(message); // NOSONAR
        if (t != null) {
            t.printStackTrace();
        }
    }

    /**
     * Constructor
     *
     * @param context the bundle context
     */
    public LogConfigManager(@NotNull BundleContext context) {
        this.bundleContext = context;
        this.logger = LoggerFactory.getLogger(getClass());
        this.debug = Boolean.parseBoolean(bundleContext.getProperty(LogConstants.DEBUG));

        this.converter = Converters.standardConverter();

        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.setName(CONTEXT_NAME);
        setLoggerContext(loggerContext);

        writerByPid = new ConcurrentHashMap<>();
        writerByFileName = new ConcurrentHashMap<>();
        configByPid = new ConcurrentHashMap<>();
        configByCategory = new ConcurrentHashMap<>();

        // populate the rootDir variable
        getRootDir();

        this.osgiIntegrationListener = new OsgiIntegrationListener(this);
        this.configAdminSupport = new ConfigAdminSupport();
    }

    /**
     * Start the custom configuration tracking and apply the
     * initial customizations
     */
    public void start() {
        // initial setup using framework properties
        setDefaultConfiguration(getBundleConfiguration(bundleContext));

        if (!SLF4JBridgeHandler.isInstalled()) {
            bridgeHandlerInstalled = maybeInstallSlf4jBridgeHandler(bundleContext);
        }

        configAdminSupport.start(bundleContext, this);

        // enable the LevelChangePropagator during any reset
        // http://logback.qos.ch/manual/configuration.html#LevelChangePropagator
        resetListeners.add(new LevelChangePropagatorChecker(() -> this.bridgeHandlerInstalled));
        resetListeners.add(this);

        try {
            appenderTracker = new AppenderTracker(bundleContext, this);
            appenderTracker.open(true);
            serviceTrackers.add(appenderTracker);
            resetListeners.add(appenderTracker);
        } catch (InvalidSyntaxException e) {
            logger.error("Failed to open the appender tracker", e);
        }

        try {
            configSourceTracker = new ConfigSourceTracker(bundleContext, this);
            configSourceTracker.open(true);
            serviceTrackers.add(configSourceTracker);
            resetListeners.add(configSourceTracker);
        } catch (InvalidSyntaxException e) {
            logger.error("Failed to open the config source tracker", e);
        }

        try {
            filterTracker = new FilterTracker(bundleContext, this);
            filterTracker.open(true);
            serviceTrackers.add(filterTracker);
            resetListeners.add(filterTracker);
        } catch (InvalidSyntaxException e) {
            logger.error("Failed to open the filter tracker", e);
        }

        turboFilterTracker = new TurboFilterTracker(bundleContext);
        turboFilterTracker.open(true);
        serviceTrackers.add(turboFilterTracker);
        resetListeners.add(turboFilterTracker);

        resetListeners.add(new RootLoggerListener()); //Should be invoked at last

        loggerContext.addListener(osgiIntegrationListener);
        registerWebConsoleSupport();
        registerEventHandler();

        // initial configuration must be done synchronously (aka immediately)
        addInfo("LogbackManager: BEGIN initial configuration");
        failSafeConfigure();
        addInfo("LogbackManager: END initialconfiguration");

        // now open the gate for regular configuration
        started = true;

        //Now check once if any other config was added while we were starting
        checkForNewConfigsWhileStarting(loggerContext);
    }

    /**
     * Stop the custom configuration tracking and remove the
     * customizations that were previously applied
     */
    public void stop() {
        // close the gate for regular configuration
        started = false;

        loggerContext.removeListener(osgiIntegrationListener);

        configAdminSupport.stop();

        for (ServiceTracker<?, ?> tracker : serviceTrackers){
            tracker.close();
        }
        serviceTrackers.clear();
        appenderTracker = null;
        configSourceTracker = null;
        filterTracker = null;
        turboFilterTracker = null;

        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
        registrations.clear();

        if (bridgeHandlerInstalled) {
            // restore the JUL to the original state
            SLF4JBridgeHandler.uninstall();
            bridgeHandlerInstalled = false;
        }

        // remove any appenders we added
        clearAllAppenders();

        writerByPid.clear();
        writerByFileName.clear();
        configByPid.clear();
        configByCategory.clear();

        // Reset and reload the default configuration to attach 
        //   a console appender to handle logging until we configure one.
        loggerContext.reset();
        DefaultConfigurator defaultConfigurator = new DefaultConfigurator();
        defaultConfigurator.setContext(loggerContext);
        defaultConfigurator.configure(loggerContext);
    }

    /**
     * Get the tracker for Appender services
     *
     * @return the tracker
     */
    public @Nullable AppenderTracker getAppenderTracker() {
        return appenderTracker;
    }

    public @Nullable ConfigSourceTracker getConfigSourceTracker() {
        return configSourceTracker;
    }

    /**
     * Clear all the known appenders
     */
    private void clearAllAppenders() {
        for (Entry<AppenderOrigin, Map<String, Set<String>>> entry : appendersByOrigin.entrySet()) {
            clearAppenders(entry.getKey(), entry.getValue());
        }
        appendersByOrigin.clear();
    }

    /**
     * Clear any appenders that we know were added
     *  
     * @param origin the place where the appender was declared
     * @param map the map of appender names to logger names
     */
    private void clearAppenders(@NotNull AppenderOrigin origin, @NotNull Map<String, Set<String>> map) {
        Map<String, Set<String>> tempMap = new HashMap<>(map);
        for (Entry<String, Set<String>> entry : tempMap.entrySet()) {
            String appenderName = entry.getKey();
            // make a copy to avoid ConcurrentModificationException
            String [] copyOfValue = entry.getValue().toArray(String[]::new);
            for (String loggerName : copyOfValue) {
                maybeDetachAppender(origin, appenderName, (Logger)LoggerFactory.getLogger(loggerName));
            }
        }
        map.clear();
    }

    //~-------------------------------------------------- Slf4j Bridge Handler Support

    /**
     * Installs the Slf4j BridgeHandler to route the JUL logs through Slf4j if the
     * {@link #JUL_SUPPORT} framework property is set to true
     *
     * @return true only if the BridgeHandler is installed.
     */
    private boolean maybeInstallSlf4jBridgeHandler(@NotNull BundleContext bundleContext){
        // SLING-2373
        boolean julSupport = converter.convert(bundleContext.getProperty(LogConstants.JUL_SUPPORT))
                .defaultValue(false)
                .to(Boolean.TYPE);
        if (julSupport) {
            // make sure configuration is empty unless explicitly set
            if (System.getProperty(LogConstants.SYSPROP_JAVA_UTIL_LOGGING_CONFIG_FILE) == null
                    && System.getProperty(LogConstants.SYSPROP_JAVA_UTIL_LOGGING_CONFIG_CLASS) == null) {
                // reset the JUL logging configuration to empty
                java.util.logging.LogManager.getLogManager().reset();
                logger.debug("The JUL logging configuration was reset to empty");
            } else {
                logger.debug("The JUL logging configuration was not reset to empty as JUL config system properties were set");
            }

            // enable the JUL handling
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST); // Root logger, for example.
        }
        return julSupport;
    }

    // ---------- Logback reset listener

    public URL getDefaultConfigURL() {
        return getClass().getResource("/logback-empty.xml");
    }

    private @Nullable LogConfig getDefaultConfig() {
        return configByPid.get(LogConstants.PID);
    }

    private @NotNull Layout<ILoggingEvent> getDefaultLayout() {
        return getDefaultConfig().createLayout(loggerContext);
    }

    public @NotNull Appender<ILoggingEvent> getDefaultAppender() {
        OutputStreamAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setName(LogConstants.DEFAULT_CONSOLE_APPENDER_NAME);
        appender.setContext(loggerContext);

        appender.setEncoder(MaskingMessageUtil.getDefaultEncoder(loggerContext));

        appender.start();
        return appender;
    }

    /**
     * Handle clearing out the old state when reset is starting
     */
    @Override
    public void onResetStart(@NotNull LoggerContext context) {
        clearAllAppenders();
    }

    /**
     * Handle restoring state when reset has completed
     */
    @Override
    public void onResetComplete(@NotNull LoggerContext context) {
        //The OSGi config based appenders are attached on reset complete as by that time Logback config
        // would have been parsed and various appenders and logger configured. Now we use the OSGi config
        // 1. If an appender with same name as one defined by OSGi config is found then it takes precedence
        // 2. If no existing appender is found then we create one
        Map<String, Appender<ILoggingEvent>> appendersByName = new HashMap<>();

        // build a map of all the appenders that were declared in the logback xml file
        Map<String, Appender<ILoggingEvent>> configuredAppenders = new HashMap<>();
        configuredAppenders.putAll(getKnownAppenders(AppenderOrigin.JORAN));
        configuredAppenders.putAll(getKnownAppenders(AppenderOrigin.JORAN_OSGI));

        final Map<Appender<ILoggingEvent>, LoggerSpecificEncoder> encoders = new HashMap<>();

        Set<String> configPids = new HashSet<>();
        for (LogConfig config : getLogConfigs()) {
            configPids.add(config.getConfigPid());
            Appender<ILoggingEvent> appender = null;
            if (config.isAppenderDefined()) {
                LogWriter lw = config.getLogWriter();

                final String appenderName = lw.getAppenderName();
                appender = appendersByName.get(appenderName);

                if (appender == null) {
                    appender = configuredAppenders.get(appenderName);
                    if (appender != null){
                        addInfo(String.format("Found overriding configuration for appender %s in Logback config. OSGi config would be ignored", appenderName));
                    }
                }

                if (appender == null) {
                    LoggerSpecificEncoder encoder = new LoggerSpecificEncoder(getDefaultLayout());
                    appender = lw.createAppender(loggerContext, encoder);
                    encoders.put(appender, encoder);
                    appendersByName.put(appenderName, appender);
                }

                if (encoders.containsKey(appender)) {
                    encoders.get(appender).addLogConfig(config);
                }
            }

            for (String category : config.getCategories()) {
                ch.qos.logback.classic.Logger targetLogger = loggerContext.getLogger(category);
                if (config.isResetToDefault()) {
                    targetLogger.setLevel(null);
                    targetLogger.setAdditive(true); //Reset additivity
                } else {
                    targetLogger.setLevel(config.getLogLevel());
                    if (appender != null) {
                        targetLogger.setAdditive(config.isAdditive());
                        targetLogger.addAppender(appender);
                        addInfo(String.format("Registering appender %s(%s) with logger %s", appender.getName(),
                                appender.getClass(), targetLogger.getName()));
                    }
                }
            }
            if (appender != null) {
                // remember what loggers we attached the appender to
                addedAppenderRef(AppenderOrigin.CONFIGSERVICE, appender.getName(), config.getCategories());
            }
        }

        //Record the config pids which have been picked up in this reset cycle
        context.putObject(LogConstants.CONFIG_PID_SET, configPids);
    }

    // ---------- Configuration support

    /**
     * Sets and applies the default configuration used by the
     * {@link #updateGlobalConfiguration(java.util.Dictionary)} method if no
     * configuration is supplied.
     */
    public void setDefaultConfiguration(@NotNull Dictionary<String, String> defaultConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
        try {
            updateGlobalConfiguration(defaultConfiguration);
        } catch (ConfigurationException ce) {
            internalFailure("Unexpected Configuration Problem", ce);
        }
    }

    /**
     * Retrieves the configuration options supplied by framework properties
     *
     * @param bundleContext the bundle context to get the properties from
     * @return the configuration options
     */
    protected @NotNull Dictionary<String, String> getBundleConfiguration(@NotNull BundleContext bundleContext) {
        Dictionary<String, String> config = new Hashtable<>(); // NOSONAR

        final String[] props = {
                LogConstants.LOG_LEVEL, LogConstants.LOG_FILE, LogConstants.LOG_FILE_NUMBER,
                LogConstants.LOG_FILE_SIZE, LogConstants.LOG_PATTERN, LogConstants.LOGBACK_FILE,
                LogConstants.LOG_PACKAGING_DATA
        };
        for (String prop : props) {
            String value = bundleContext.getProperty(prop);
            if (value != null) {
                config.put(prop, value);
            }
        }

        return config;
    }

    /**
     * Returns the <code>logFileName</code> argument converted into an absolute
     * path name. If <code>logFileName</code> is already absolute it is returned
     * unmodified. Otherwise it is made absolute by resolving it relative to the
     * SLING_LOG_ROOT or SLING_HOME directory.
     *
     * @param logFileName the value to resolve from
     * @return the resolved absolute path
     */
    protected @NotNull String getAbsoluteFilePath(@NotNull String logFileName) {
        // ensure proper separator in the path (esp. for systems, which do
        // not use "slash" as a separator, e.g Windows)
        logFileName = logFileName.replace('/', File.separatorChar);

        // create a file instance and check whether this is absolute. If not
        // create a new absolute file instance with the root dir and get
        // the absolute path name from that
        File logFile = new File(logFileName);
        if (!logFile.isAbsolute()) {
            logFile = Paths.get(rootDir.toURI()).resolve(logFileName).toFile();
            logFileName = logFile.getAbsolutePath();
        }

        // return the correct log file name
        return logFileName;
    }

    /**
     * Verify that the file is valid to read as a logback xml file
     *
     * @param file the file to check
     * @return true if valid, false otherwise
     */
    protected boolean isLogbackFileValid(@NotNull File file) {
        boolean valid = false;
        if (!file.exists()) {
            logger.warn("Logback configuration file [{}] does not exist", file.getAbsolutePath());
        } else if (!file.isFile()) {
            logger.warn("Logback configuration file [{}] is not a file", file.getAbsolutePath());
        } else if (!file.canRead()) {
            logger.warn("Logback configuration file [{}] cannot be read", file.getAbsolutePath());
        } else {
            valid = true;
        }
        return valid;
    }

    private void processGlobalConfig(@NotNull Dictionary<String, String> configuration) {
        String fileName = converter.convert(configuration.get(LogConstants.LOGBACK_FILE))
                .defaultValue("")
                .to(String.class);
        if (!fileName.isEmpty()) {
            // attempt to load the configuration from the logback xml file
            File file = new File(getAbsoluteFilePath(fileName));
            if (isLogbackFileValid(file)) {
                this.logbackConfigFile = file;
            }
        }

        //Process packaging data
        this.packagingDataEnabled = converter.convert(configuration.get(LogConstants.LOG_PACKAGING_DATA))
                .defaultValue(false) // Defaults to false i.e. disabled in OSGi env
                .to(Boolean.TYPE);

        maxCallerDataDepth = converter.convert(configuration.get(LogConstants.LOG_MAX_CALLER_DEPTH))
            .defaultValue(ClassicConstants.DEFAULT_MAX_CALLEDER_DATA_DEPTH)
            .to(Integer.TYPE);
        maxOldFileCount = converter.convert(configuration.get(LogConstants.PRINTER_MAX_INCLUDED_FILES))
                .defaultValue(LogConstants.PRINTER_MAX_INCLUDED_FILES_DEFAULT)
                .to(Integer.TYPE);
        numOfLines = converter.convert(configuration.get(LogConstants.PRINTER_NUM_OF_LINES))
                .defaultValue(LogConstants.PRINTER_NUM_OF_LINES_DEFAULT)
                .to(Integer.TYPE);
    }

    public void updateGlobalConfiguration(@Nullable Dictionary<String, String> configuration)
            throws ConfigurationException {
        logger.info("updating global configuration for {}", LogConstants.PID);

        // fallback to start default settings when the config is deleted
        if (configuration == null) {
            configuration = defaultConfiguration;
        }

        processGlobalConfig(configuration);

        // set the logger name to a special value to indicate the global
        // (ROOT) logger setting (SLING-529)
        configuration.put(LogConstants.LOG_LOGGERS, org.slf4j.Logger.ROOT_LOGGER_NAME);

        // normalize logger file (might be console
        final String logFile = configuration.get(LogConstants.LOG_FILE);
        if (logFile == null || logFile.trim().length() == 0) {
            configuration.put(LogConstants.LOG_FILE, LogConstants.FILE_NAME_CONSOLE);
        }

        // update the default log writer and logger configuration
        updateLogWriter(LogConstants.PID, configuration, false);
        updateLoggerConfiguration(LogConstants.PID, configuration, false);

        configChanged();
    }

    /**
     * Updates or removes the log writer configuration identified by the
     * <code>pid</code>. In case of log writer removal, any logger configuration
     * referring to the removed log writer is modified to now log to the default
     * log writer.
     * <p>
     * The configuration object is expected to contain the following properties:
     * <dl>
     * <dt>{@link LogConfigManager#LOG_FILE}</dt>
     * <dd>The relative of absolute path/name of the file to log to. If this
     * property is missing or an empty string, the writer writes to standard
     * output</dd>
     * <dt>{@link LogConfigManager#LOG_FILE_SIZE}</dt>
     * <dd>The maximum size of the log file to write before rotating the log
     * file. This property must be a number of be convertible to a number. The
     * actual value may also be suffixed by a size indicator <code>k</code>,
     * <code>kb</code>, <code>m</code>, <code>mb</code>, <code>g</code> or
     * <code>gb</code> representing the respective factors of kilo, mega and
     * giga.If this property is missing or cannot be converted to a number, the
     * default value {@link LogConfigManager#LOG_FILE_SIZE_DEFAULT}
     * is assumed. If the writer writes standard output this property is
     * ignored.</dd>
     * <dt>{@link LogConfigManager#LOG_FILE_NUMBER}</dt>
     * <dd>The maximum number of rotated log files to keep. This property must
     * be a number of be convertible to a number. If this property is missing or
     * cannot be converted to a number, the default value
     * {@link LogConfigManager#LOG_FILE_NUMBER_DEFAULT} is assumed.
     * If the writer writes standard output this property is ignored.</dd>
     * </dl>
     *
     * @param pid The identifier of the log writer to update or remove
     * @param configuration New configuration setting for the log writer or
     *            <code>null</code> to indicate to remove the log writer.
     * @throws ConfigurationException If another log writer already exists for
     *             the same file as configured for the given log writer or if
     *             configuring the log writer fails.
     */
    public void updateLogWriter(@NotNull String pid, @Nullable Dictionary<?, ?> configuration,
            boolean performRefresh) throws ConfigurationException {

        if (configuration != null) {
            LogWriter oldWriter = writerByPid.get(pid);

            // get the log file parameter and normalize empty string to null
            String logFileName = (String) configuration.get(LogConstants.LOG_FILE);

            // Null logFileName is treated as Console Appender
            if (logFileName == null || logFileName.trim().isEmpty()) {
                logFileName = LogWriter.FILE_NAME_CONSOLE;
            }

            // if we have a file name, make it absolute and correct for our
            // environment and verify there is no other writer already existing
            // for the same file
            if (!LogWriter.FILE_NAME_CONSOLE.equals(logFileName)) {

                // ensure absolute path
                logFileName = getAbsoluteFilePath(logFileName);

                // ensure unique configuration of the log writer
                LogWriter existingWriterByFileName = writerByFileName.get(logFileName);
                if (existingWriterByFileName != null
                        && !existingWriterByFileName.getConfigurationPID().equals(pid)) {

                    // this file is already configured by another LOG_PID
                    throw new ConfigurationException(LogConstants.LOG_FILE, "LogFile " + logFileName
                        + " already configured by configuration " + existingWriterByFileName.getConfigurationPID());
                }
            }

            // get number of files and ensure minimum and default
            Object fileNumProp = configuration.get(LogConstants.LOG_FILE_NUMBER);
            int fileNum = -1;
            if (fileNumProp instanceof Number) {
                fileNum = ((Number) fileNumProp).intValue();
            } else if (fileNumProp != null) {
                try {
                    fileNum = Integer.parseInt(fileNumProp.toString());
                } catch (NumberFormatException nfe) {
                    // don't care
                }
            }

            // get the log file size
            Object fileSizeProp = configuration.get(LogConstants.LOG_FILE_SIZE);
            String fileSize = null;
            if (fileSizeProp != null) {
                fileSize = fileSizeProp.toString();
            }

            boolean bufferedLogging = converter.convert(configuration.get(LogConstants.LOG_FILE_BUFFERED))
                    .defaultValue(false)
                    .to(Boolean.TYPE);

            LogWriter newWriter = new LogWriter(pid, getAppenderName(logFileName), fileNum,
                    fileSize, logFileName, bufferedLogging);
            if (oldWriter != null) {
                writerByFileName.remove(oldWriter.getFileName());
            }

            writerByFileName.put(newWriter.getFileName(), newWriter);
            writerByPid.put(newWriter.getConfigurationPID(), newWriter);

        } else {
            final LogWriter logWriter = writerByPid.remove(pid);

            if (logWriter != null) {
                writerByFileName.remove(logWriter.getFileName());
            }
        }

        if (performRefresh) {
            configChanged();
        }
    }

    /**
     * Updates or removes the logger configuration indicated by the given
     * <code>pid</code>. If the case of modified categories or removal of the
     * logger configuration, existing loggers will be modified to reflect the
     * correct logger configurations available.
     * <p>
     * The configuration object is expected to contain the following properties:
     * <dl>
     * <dt>{@link LogConfigManager#LOG_PATTERN}</dt>
     * <dd>The <code>MessageFormat</code> pattern to apply to format the log
     * message before writing it to the log writer. If this property is missing
     * or the empty string the default pattern
     * {@link LogConfigManager#LOG_PATTERN_DEFAULT} is used.</dd>
     * <dt>{@link LogConfigManager#LOG_LEVEL}</dt>
     * <dd>The log level to use for log message limitation. The supported values
     * are <code>off</code>, <code>trace</code>, <code>debug</code>, <code>info</code>,
     * <code>warn</code> and <code>error</code>. Case does not matter. If this
     * property is missing a <code>ConfigurationException</code> is thrown and
     * this logger configuration is not used.</dd>
     * <dt>{@link LogConfigManager#LOG_LOGGERS}</dt>
     * <dd>The logger names to which this configuration applies. As logger names
     * form a hierarchy like Java packages, the listed names also apply to
     * "child names" unless more specific configuration applies for such
     * children. This property may be a single string, an array of strings or a
     * collection of strings. Each string may itself be a comma-separated list
     * of logger names. If this property is missing a
     * <code>ConfigurationException</code> is thrown.</dd>
     * <dt>{@link LogConfigManager#LOG_FILE}</dt>
     * <dd>The name of the log writer to use. This may be the name of a log file
     * configured for any log writer or it may be the configuration PID of such
     * a writer. If this property is missing or empty or does not refer to an
     * existing log writer configuration, the default log writer is used.</dd>
     *</dl>
     * @param pid The name of the configuration to update or remove.
     * @param configuration The configuration object.
     * @throws ConfigurationException If the log level and logger names
     *             properties are not configured for the given configuration.
     */
    public void updateLoggerConfiguration(@NotNull final String pid, @Nullable final Dictionary<?, ?> configuration,
            final boolean performRefresh) throws ConfigurationException {

        if (configuration != null) {
            String pattern = converter.convert(configuration.get(LogConstants.LOG_PATTERN))
                    .defaultValue(LogConstants.LOG_PATTERN_DEFAULT)
                    .to(String.class);
            String level = converter.convert(configuration.get(LogConstants.LOG_LEVEL))
                    .defaultValue(LogConstants.LOG_LEVEL_DEFAULT)
                    .to(String.class);
            final Level logLevel;
            final boolean resetToDefault;
            if (LogConstants.LOG_LEVEL_RESET_TO_DEFAULT.equalsIgnoreCase(level)){
                resetToDefault = true;
                logLevel = null;
            } else {
                logLevel = Level.toLevel(level, null);
                if (logLevel == null) {
                    throw new ConfigurationException(LogConstants.LOG_LEVEL, "Not a valid value");
                }
                resetToDefault = false;
            }

            String fileName = converter.convert(configuration.get(LogConstants.LOG_FILE))
                    .defaultValue(null)
                    .to(String.class);
            // Map empty fileName to console logger
            // null fileName is for scenario where intention is just to change the log level
            if (fileName != null && fileName.trim().isEmpty()) {
                fileName = LogConstants.FILE_NAME_CONSOLE;
            }

            // FileName being just null means that we want to change the
            // LogLevel
            if (fileName != null && !LogConstants.FILE_NAME_CONSOLE.equals(fileName)) {
                fileName = getAbsoluteFilePath(fileName);
            }

            @SuppressWarnings("unchecked")
            Set<String> categories = converter.convert(configuration.get(LogConstants.LOG_LOGGERS))
                    .to(Set.class);
            // verify categories
            if (categories.isEmpty()) {
                throw new ConfigurationException(LogConstants.LOG_LOGGERS, "Missing categories in configuration "
                    + pid);
            }

            boolean additiv = converter.convert(configuration.get(LogConstants.LOG_ADDITIV))
                    // If an appender is explicitly defined then set additive to false
                    // to be compatible with earlier Sling Logging behavior
                   .defaultValue(false)
                   .to(Boolean.TYPE);

            // verify no other configuration has any of the categories
            for (final String cat : categories) {
                final LogConfig cfg = configByCategory.get(cat);
                if (cfg != null && !pid.equals(cfg.getConfigPid())) {
                    throw new ConfigurationException(LogConstants.LOG_LOGGERS,
                            String.format("Category %s already defined by configuration %s", cat, cfg.getConfigPid()));
                }
            }

            // create or modify existing configuration object
            final LogConfig newConfig = new LogConfig(this, pattern, categories, logLevel, fileName, additiv,
                    pid, resetToDefault);
            if (packagingDataEnabled) {
                newConfig.setPostProcessor(new OSGiAwareExceptionHandling(getPackageInfoCollector()));
            }
            LogConfig oldConfig = configByPid.get(pid);
            if (oldConfig != null) {
                configByCategory.keySet().removeAll(oldConfig.getCategories());
            }

            // relink categories
            for (String cat : categories) {
                configByCategory.put(cat, newConfig);
            }

            configByPid.put(pid, newConfig);
        } else {
            // configuration deleted if null

            // remove configuration from pid list
            LogConfig config = configByPid.remove(pid);

            if (config != null) {
                // remove all configured categories
                configByCategory.keySet().removeAll(config.getCategories());
            }
        }

        if (performRefresh) {
            configChanged();
        }
    }

    public void checkForNewConfigsWhileStarting(@NotNull LoggerContext context){
        @SuppressWarnings("unchecked")
        Set<String> configPids = (Set<String>) context.getObject(LogConstants.CONFIG_PID_SET);
        if (configPids == null) {
            addWarn("Did not find any configPid set");
            return;
        }
        if (!configPids.equals(configByPid.keySet())) {
            addInfo("Config change detected post start. Scheduling config reload");
            configChanged();
        } else {
            addInfo("Configured the Logback with " + configPids.size() + " configs");
        }
    }

    /**
     * Returns the current value of the {@link #LOG_PACKAGING_DATA} configuration
     *
     * @return true if packaging data is enabled, false otherwise
     */
    public boolean isPackagingDataEnabled() {
        return packagingDataEnabled;
    }

    /**
     * Returns the current value of the {@link #LOG_MAX_CALLER_DEPTH} configuration
     *
     * @return the max stack data depth computed during caller data extraction
     */
    public int getMaxCallerDataDepth() {
        return maxCallerDataDepth;
    }

    /**
     * Returns the current value of the {@link #PRINTER_MAX_INCLUDED_FILES} configuration
     *
     * @return the max number of rolled over files to included in the webconsole
     *      configuration printer
     */
    public int getMaxOldFileCount() {
        return maxOldFileCount;
    }

    /**
     * Maximum number of lines from a log files to be included in txt mode dump
     */
    public int getNumOfLines() {
        return numOfLines;
    }

    /**
     * Add or update an appender to the supplied loggers
     *
     * @param origin the place where the appender was declared
     * @param appenderName the name of the appender to configure
     * @param appender the appender to add or update
     * @param loggers collection of logger names to apply the appender to
     */
    protected void addOrUpdateAppender(@NotNull AppenderOrigin origin, @NotNull String appenderName, @NotNull Appender<ILoggingEvent> appender,
            @NotNull Collection<String> loggers) {
        // detach any values from a previous call so we don't get duplicates
        Map<String, Set<String>> appenderNameToLoggerNamesMap = appendersByOrigin.getOrDefault(origin, Collections.emptyMap());
        Set<String> loggerNamesSet = new HashSet<>(appenderNameToLoggerNamesMap.getOrDefault(appenderName, Collections.emptySet()));
        for (String loggerName : loggerNamesSet) {
            Logger targetLogger = (Logger)LoggerFactory.getLogger(loggerName);
            maybeDetachAppender(origin, appenderName, targetLogger);
        }

        appender.setContext(loggerContext);
        appender.setName(appenderName);
        appender.start();

        for (String loggerName : loggers) {
            Logger targetLogger = (Logger) LoggerFactory.getLogger(loggerName);

            addInfo(String.format("attaching appender %s for %s", appenderName, targetLogger.getName()));

            targetLogger.addAppender(appender);
        }

        // remember what loggers we attached the appender to
        addedAppenderRef(origin, appenderName, loggers);
    }

    /**
     * Add or update an appender with the supplied configuration
     *
     * @param origin the place where the appender was declared
     * @param appenderName the name of the appender to configure
     * @param config the configuration options to apply
     */
    protected void addOrUpdateAppender(@NotNull AppenderOrigin origin, @NotNull String appenderName, @NotNull Dictionary<String, ?> config) {
        // detach any values from a previous call so we don't get duplicates
        Map<String, Set<String>> appenderNameToLoggerNamesMap = appendersByOrigin.getOrDefault(origin, Collections.emptyMap());
        Set<String> loggerNamesSet = new HashSet<>(appenderNameToLoggerNamesMap.getOrDefault(appenderName, Collections.emptySet()));
        for (String loggerName : loggerNamesSet) {
            Logger targetLogger = (Logger)LoggerFactory.getLogger(loggerName);
            maybeDetachAppender(origin, appenderName, targetLogger);
        }

        String pattern = converter.convert(config.get(LogConstants.LOG_PATTERN))
                .defaultValue(LogConstants.LOG_PATTERN_DEFAULT)
                .to(String.class);
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern(pattern);
        ple.setContext(loggerContext);
        ple.start();

        String file = converter.convert(config.get(LogConstants.LOG_FILE))
                .defaultValue("")
                .to(String.class);
        OutputStreamAppender<ILoggingEvent> appender;
        if (file.isEmpty() || LogConstants.FILE_NAME_CONSOLE.equals(file)) {
            appender = new ConsoleAppender<>();
        } else {
            // resolve the path relative to the sling home folder
            file = getAbsoluteFilePath(file);
            RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
            fileAppender.setFile(file);
            fileAppender.setAppend(true);

            String fileNamePattern = LogWriter.createFileNamePattern(file, 
                    converter.convert(config.get(LogConstants.LOG_FILE_SIZE))
                        .defaultValue(LogConstants.LOG_FILE_SIZE_DEFAULT)
                        .to(String.class));

            // resolve the path relative to the sling home folder
            fileNamePattern = getAbsoluteFilePath(fileNamePattern);

            TimeBasedRollingPolicy<Object> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setParent(fileAppender);
            rollingPolicy.setFileNamePattern(fileNamePattern);
            int fileNumber = converter.convert(config.get(LogConstants.LOG_FILE_NUMBER))
                    .defaultValue(5)
                    .to(Integer.TYPE);
            rollingPolicy.setMaxHistory(fileNumber);
            rollingPolicy.start();

            fileAppender.setRollingPolicy(rollingPolicy);

            appender = fileAppender;
        }
        appender.setName(appenderName);
        appender.setEncoder(ple);
        appender.setContext(loggerContext);
        appender.start();

        Level level = Level.valueOf(converter.convert(config.get(LogConstants.LOG_LEVEL))
                .defaultValue(LogConstants.LOG_LEVEL_DEFAULT)
                .to(String.class));
        boolean additive = converter.convert(config.get(LogConstants.LOG_ADDITIV))
                 // If an appender is explicitly defined then set additive to false
                 // to be compatible with earlier Sling Logging behavior
                .defaultValue(false)
                .to(Boolean.TYPE);
        String [] loggers = converter.convert(config.get(LogConstants.LOG_LOGGERS))
                .defaultValue(org.slf4j.Logger.ROOT_LOGGER_NAME)
                .to(String[].class);
        for (String loggerName : loggers) {
            Logger targetLogger = (Logger)LoggerFactory.getLogger(loggerName);

            addInfo(String.format("attaching appender %s for %s", appenderName, targetLogger.getName()));

            targetLogger.setLevel(level);
            targetLogger.setAdditive(additive); /* set to true if root should log too */
            targetLogger.addAppender(appender);
        }

        // remember what loggers we attached the appender to
        addedAppenderRef(origin, appenderName, List.of(loggers));
    }

    /**
     * Detach the appender from the logger if it exists
     *
     * @param origin the place where the appender was declared
     * @param appenderName the name of the appender to detach
     * @param targetLogger logger to detach the appender from
     */
    protected void maybeDetachAppender(@NotNull AppenderOrigin origin, @NotNull String appenderName, @NotNull Logger targetLogger) {
        Appender<ILoggingEvent> appender = targetLogger.getAppender(appenderName);
        if (appender != null) {
            String loggerName = targetLogger.getName();
            addInfo(String.format("detaching appender %s for %s", appenderName, loggerName));
            targetLogger.detachAppender(appenderName);

            // notify the filter tracker about the appender so it
            //  possibly detach the filter as well
            if (filterTracker != null) {
                filterTracker.detachedAppender(appender);
            }

            // forget what loggers we detached the appender from
            synchronized (appendersByOrigin) {
                Map<String, Set<String>> map = appendersByOrigin.getOrDefault(origin, Collections.emptyMap());
                Set<String> loggerNamesSet = map.get(appenderName);
                if (loggerNamesSet != null) {
                    loggerNamesSet.remove(loggerName);
                    if (loggerNamesSet.isEmpty()) {
                        map.remove(appenderName);
                    }
                }
            }
        }
    }

    /**
     * Return a map of all the appenders that were attached by this manager
     * 
     * @return map of known appenders where the key is the appender name and the value is the appender
     */
    @NotNull Map<String, Appender<ILoggingEvent>> getAllKnownAppenders() {
        Map<String, Appender<ILoggingEvent>> all = new HashMap<>();
        for (AppenderOrigin origin : appendersByOrigin.keySet()) {
            all.putAll(getKnownAppenders(origin));
        }
        return Collections.unmodifiableMap(all);
    }

    /**
     * Return a map of the appenders that were attached by this manager with the specified origin
     * 
     * @param origin the place where the appender was declared
     * @return map of known appenders where the key is the appender name and the value is the appender
     */
    @NotNull Map<String, Appender<ILoggingEvent>> getKnownAppenders(@NotNull AppenderOrigin origin) {
        Map<String, Appender<ILoggingEvent>> appendersMap = new HashMap<>();

        Map<String, Set<String>> map = appendersByOrigin.getOrDefault(origin, Collections.emptyMap());
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            String pid = entry.getKey();
            appendersMap.computeIfAbsent(pid, k-> firstAppenderFromLoggers(pid, entry.getValue()));
        }

        return Collections.unmodifiableMap(appendersMap);
    }

    /**
     * Return the names of the loggers that the known appender is attached to
     *
     * @param origin the place where the appender was declared
     * @param appenderName the appender name to lookup
     * @return the set of logger names for the appender or an empty set if none
     */
    public @NotNull Set<String> getLoggerNamesForKnownAppender(@NotNull AppenderOrigin origin, @NotNull String appenderName) {
        Map<String, Set<String>> map = appendersByOrigin.getOrDefault(origin, Collections.emptyMap());
        return map.getOrDefault(appenderName, Collections.emptySet());
    }

    /**
     * Locate the first appender with the specified name that is attached to any of the specified loggers
     *
     * @param name the name of the appender
     * @param loggerNames the names of the loggers to inspect
     * @return the found appender or null if not found
     */
    @Nullable Appender<ILoggingEvent> firstAppenderFromLoggers(@NotNull String name, @NotNull Collection<String> loggerNames) {
        Appender<ILoggingEvent> appender = null;
        for (String loggerName : loggerNames) {
            Logger targetLogger = (Logger)LoggerFactory.getLogger(loggerName);
            appender = targetLogger.getAppender(name);
            if (appender != null) {
                 // found one so stop looking
                break;
            }
        }
        return appender;
    }

    /**
     * This is a callback from the logback xml config file loading that is intended to keep
     * track of what appenders are referenced by which loggers.
     *
     * @param origin the place where the appender was declared
     * @param appenderName the appender name
     * @param loggerName the logger name
     */
    public void addedAppenderRef(@NotNull AppenderOrigin origin, @NotNull String appenderName, @NotNull String loggerName) {
        addedAppenderRef(origin, appenderName, List.of(loggerName));
    }
    public void addedAppenderRef(@NotNull AppenderOrigin origin, @NotNull String appenderName, @NotNull Collection<String> loggerNames) {
        synchronized (appendersByOrigin) {
            Map<String, Set<String>> appenderNameToLoggerNamesMap = appendersByOrigin.computeIfAbsent(origin, k -> new HashMap<>());
            Set<String> set = appenderNameToLoggerNamesMap.computeIfAbsent(appenderName, k -> new HashSet<>());
            set.addAll(loggerNames);
        }

        // consult any filter trackers that want to participate
        if (filterTracker != null) {
            Appender<ILoggingEvent> appender = firstAppenderFromLoggers(appenderName, loggerNames);
            if (appender != null) {
                filterTracker.attachedAppender(appender);
            }
        }
    }

    /**
     * Adds substitution properties that may be used in logback xml files
     *
     * @param propContainer the property container to add the substitution on
     */
    public void addSubsitutionProperties(@NotNull PropertyContainer propContainer) {
        propContainer.addSubstitutionProperty(LogConstants.SLING_HOME, rootDir.getAbsolutePath());
    }


    //-------------------------------------- Config reset handling ----------

    public void configChanged() {
        if (!started) {
            logger.debug("LoggerContext is not started so skipping reset handling");
            return;
        }

        /*
        Logback reset cannot be done concurrently. So when Logback is being reset
        we note down any new request for reset. Later when the thread which performs
        reset finishes, then it checks if any request for reset pending. if yes
        then it again tries to reschedules a job to perform reset in rescheduleIfConfigChanged

        Logback reset is done under a lock 'resetLock' so that Logback
        is not reconfigured concurrently. Only the thread which acquires the
        'resetLock' can submit the task for reload (actual reload done async)

        Once the reload is done the lock is released in LoggerReconfigurer#run

        The way locking works is any thread which changes config
        invokes configChanged. Here two things are possible

        1. Log reset in progress i.e. resetLock already acquired
           In this case the thread would just set the 'configChanged' flag to true

        2. No reset in progress. Thread would acquire the  resetLock and submit the
          job to reset Logback


        Any such change is synchronized with configChangedFlagLock such that a request
         for config changed is not missed
        */

        synchronized (configChangedFlagLock) {
            if (resetLock.tryAcquire()) {
                configChanged = false;
                scheduleConfigReload();
            } else {
                configChanged = true;
                addInfo("LoggerContext reset in progress. Marking config changed to true");
            }
        }
    }

    protected boolean rescheduleIfConfigChanged() {
        boolean rescheduled = false;
        synchronized (configChangedFlagLock) {
            //If config changed then only acquire a lock
            //and proceed to reload
            if (configChanged && resetLock.tryAcquire()) {
                configChanged = false;
                scheduleConfigReload();
                rescheduled = true;

                //else some other thread acquired the resetlock
                //and reset is in progress. That job would
                //eventually call rescheduleIfConfigChanged again
                //and configChanged request would be taken care of
            }
        }
        return rescheduled;
    }

    @SuppressWarnings("rawtypes")
    protected @NotNull Future scheduleConfigReload() {
        return loggerContext.getExecutorService().submit(() -> {
            try {
                failSafeConfigure();
            } finally {
                resetLock.release();
                addInfo("Re configuration done");
                rescheduleIfConfigChanged();
            }
        });
    }

    protected void failSafeConfigure(){
        try {
            addInfo("Performing configuration");

            long startTime = System.currentTimeMillis();
            StatusListener statusListener = new StatusListenerAsList() {
                @Override
                public boolean isResetResistant() {
                    // ensure that after a reset the statusListenerAsList does not get
                    // removed as a listener
                    return true;
                }
            };
            if (debug) {
                OnConsoleStatusListener onConsoleStatusListener = new OnConsoleStatusListener();
                // ensure that after a reset the statusListenerAsList does not get
                // removed as a listener
                onConsoleStatusListener.setResetResistant(true);
                statusListener = onConsoleStatusListener;
            }

            getStatusManager().add(statusListener);
            addInfo("Resetting context: " + getLoggerContext().getName());

            JoranConfigurator configurator = new JoranConfiguratorWrapper(this);
            configurator.setContext(loggerContext);
            final Model failsafeTop = configurator.recallSafeConfiguration();

            loggerContext.reset();

            final long threshold = System.currentTimeMillis();
            boolean success = false;
            try {
                if (logbackConfigFile != null) {
                    // use the configured file
                    configurator.doConfigure(logbackConfigFile);
                } else {
                    // fallback to the empty configuration
                    configurator.doConfigure(getDefaultConfigURL());
                }

                // e.g. IncludeAction will add a status regarding XML parsing errors but no exception will reach here
                StatusUtil statusUtil = new StatusUtil(loggerContext);
                if (!statusUtil.hasXMLParsingErrors(threshold)) {
                    addInfo("Context: " + getLoggerContext().getName() + " reloaded.");
                    success = true;
                }
            } catch (Throwable t) { // NOSONAR
                //Need to catch any error as Logback must work in all scenarios
                //The error would be dumped to sysout in later call to Status printer
                addError("Error occurred while configuring Logback", t);
            } finally {
                if (!success) {
                    fallbackConfiguration(loggerContext, failsafeTop);
                }
                getStatusManager().remove(statusListener);
                SlingStatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext, resetStartTime, startTime, success);
            }

            fireResetCompleteListeners();
        } catch (Exception e) {
            logger.warn("Error occurred while re-configuring logger", e);
            addError("Error occurred while re-configuring logger", e);
        }
    }

    /**
     * Logic based on ch.qos.logback.classic.joran.ReconfigureOnChangeTask
     */
    private void fallbackConfiguration(LoggerContext lc, Model failsafeTop) {
        JoranConfigurator joranConfigurator = new JoranConfiguratorWrapper(this);
        joranConfigurator.setContext(context);
        ConfigurationWatchList oldCWL = ConfigurationWatchListUtil.getConfigurationWatchList(context);
        ConfigurationWatchList newCWL = oldCWL.buildClone();

        if (failsafeTop == null) {
            addWarn("No previous configuration to fall back on.");
        } else {
            addWarn("Given previous errors, falling back to previously registered safe configuration.");
            addInfo("Safe model " + failsafeTop);
            try {
                lc.reset();
                ConfigurationWatchListUtil.registerConfigurationWatchList(context, newCWL);
                ModelUtil.resetForReuse(failsafeTop);
                joranConfigurator.processModel(failsafeTop);
                addInfo("Re-registering previous fallback configuration once more as a fallback configuration point");
                joranConfigurator.registerSafeConfiguration(failsafeTop);
                context.fireConfigurationEvent(newConfigurationEndedEvent(this));
                addInfo("after registerSafeConfiguration");
            } catch (Exception e) {
                addError("Unexpected exception thrown by a configuration considered safe.", e);
            }
        }
    }

    String getRootDir() {
        if (rootDir == null) {
            String slingLogRoot = bundleContext.getProperty(LogConstants.SLING_LOG_ROOT);
            if (slingLogRoot == null) {
                slingLogRoot = bundleContext.getProperty(LogConstants.SLING_HOME);
                if (slingLogRoot == null) {
                    slingLogRoot = Paths.get("").toAbsolutePath().toString();
                }
            }
            addInfo("Using rootDir as " + slingLogRoot);
            rootDir = new File(slingLogRoot);
        }
        return rootDir.toString();
    }

    /**
     * Called during reset to store the timestamp when the last reset occurred
     */
    void updateResetStartTime() {
        resetStartTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.toMillis(1);
    }

    /**
     * Invokes the reset start callbacks for each reset complete listener
     */
    public void fireResetStartListeners(){
        for (LogbackResetListener listener : resetListeners) {
            addInfo("Firing reset listener - onResetStart " + listener.getClass());
            listener.onResetStart(loggerContext);
        }
    }

    /**
     * Invokes the reset complete callbacks for each reset complete listener
     */
    public void fireResetCompleteListeners(){
        for (LogbackResetListener listener : resetListeners) {
            addInfo("Firing reset listener - onResetComplete " + listener.getClass());
            listener.onResetComplete(loggerContext);
        }
    }

    public @NotNull PackageInfoCollector getPackageInfoCollector() {
        return packageInfoCollector;
    }

    // ---------- Internal helpers ---------------------------------------------

    private @NotNull String getAppenderName(@NotNull String filePathAbsolute) {
        String rootDirPath = rootDir.getAbsolutePath();

        if (filePathAbsolute.startsWith(rootDirPath)) {
            //Make the fileName relative to the absolute dir
            //Normalize the name to use '/'
            return filePathAbsolute.substring(rootDirPath.length()).replace('\\','/');
        }
        return filePathAbsolute;
    }

    private @NotNull LogWriter createImplicitWriter(@NotNull String logWriterName) {
        LogWriter defaultWriter = getDefaultWriter();
        if (defaultWriter == null) {
            throw new IllegalStateException("Default logger configuration must have been configured by now");
        }
        return new LogWriter(getAppenderName(logWriterName),logWriterName,
                defaultWriter.getLogNumber(), defaultWriter.getLogRotation());
    }

    public @Nullable LogWriter getDefaultWriter() {
        return writerByPid.get(LogConstants.PID);
    }

    /**
     * Intended to be used only by tests
     *
     * @param pid the pid to check
     * @return true if a writer has the pid
     */
    public boolean hasWriterByPid(String pid) {
        return writerByPid.containsKey(pid);
    }

    /**
     * Intended to be used only by tests
     *
     * @param logWriterName the nameto check
     * @return true if a writer has the name
     */
    public boolean hasWriterByName(String logWriterName) {
        return writerByFileName.containsKey(logWriterName);
    }

    /**
     * Intended to be used only by tests
     *
     * @param pid the pid to check
     * @return true if a config has the pid
     */
    boolean hasConfigByPid(String pid) {
        return configByPid.containsKey(pid);
    }

    /**
     * Intended to be used only by tests
     *
     * @param loggerName the logger name check
     * @return true if a config has the loggerName
     */
    boolean hasConfigByName(String loggerName) {
        return configByCategory.containsKey(loggerName);
    }

    // ---------- SlingLogPanel support

    @Override
    public @NotNull LogWriter getLogWriter(@NotNull String logWriterName) {
        LogWriter lw = writerByFileName.get(logWriterName);
        if (lw == null) {
            lw = createImplicitWriter(logWriterName);
        }
        return lw;
    }

    public File getLogbackConfigFile() {
        return logbackConfigFile;
    }

    public Iterable<LogConfig> getLogConfigs() {
        return configByPid.values();
    }

    // ~ ----------------------------------------------WebConsole Support

    public LoggerStateContext determineLoggerState() {
        final List<Logger> loggers = loggerContext.getLoggerList();
        final LoggerStateContext ctx = new LoggerStateContext(loggers, packageInfoCollector);

        //Distinguish between Logger configured via
        //1. OSGi Config - The ones configured via ConfigAdmin
        //2. Other means - Configured via Logback config or any other means
        for (LogConfig lc : getLogConfigs()) {
            for (String category : lc.getCategories()) {
                ctx.osgiConfiguredLoggers.put(category, lc);
            }
        }

        for (Logger targetLogger : loggers) {
            boolean hasOnlySlingRollingAppenders = true;
            Iterator<Appender<ILoggingEvent>> itr = targetLogger.iteratorForAppenders();
            while (itr.hasNext()) {
                Appender<ILoggingEvent> a = itr.next();
                if (a.getName() != null && !ctx.appenders.containsKey(a.getName())) {
                    ctx.appenders.put(a.getName(), a);
                }

                if (!(a instanceof SlingRollingFileAppender)) {
                    hasOnlySlingRollingAppenders = false;
                }
            }

            if (targetLogger.getLevel() == null) {
                continue;
            }

            boolean configuredViaOSGiConfig =
                    ctx.osgiConfiguredLoggers.containsKey(targetLogger.getName());
            if (!configuredViaOSGiConfig || !hasOnlySlingRollingAppenders) {
                ctx.nonOSgiConfiguredLoggers.add(targetLogger);
            }
        }

        return ctx;
    }

    public class LoggerStateContext {
        final List<Logger> allLoggers;

        /**
         * List of logger which have explicitly defined level or appenders set
         */
        final List<Logger> nonOSgiConfiguredLoggers = new ArrayList<>();

        final Map<String,LogConfig> osgiConfiguredLoggers = new HashMap<>();

        final Map<String, Appender<ILoggingEvent>> appenders = new HashMap<>();

        final Map<Appender<ILoggingEvent>, AppenderInfo> dynamicAppenders =
                new HashMap<>();

        final Map<ServiceReference<TurboFilter>,TurboFilter> turboFilters;

        final PackageInfoCollector packageInfoCollector;

        LoggerStateContext(List<Logger> allLoggers, PackageInfoCollector packageInfoCollector) {
            this.allLoggers = allLoggers;
            this.packageInfoCollector = packageInfoCollector;
            if (appenderTracker != null) {
                for (AppenderTracker.AppenderInfo ai : appenderTracker.getAppenderInfos()) {
                    dynamicAppenders.put(ai.appender, ai);
                }
            }
            this.turboFilters = turboFilterTracker == null ? Collections.emptyMap() : turboFilterTracker.getFilters();
        }

        int getNumberOfLoggers() {
            return allLoggers.size();
        }

        int getNumOfDynamicAppenders() {
            return appenderTracker.getAppenderInfos().size();
        }

        int getNumOfAppenders() {
            return appenders.size();
        }

        boolean isDynamicAppender(Appender<ILoggingEvent> a) {
            return dynamicAppenders.containsKey(a);
        }

        ServiceReference<TurboFilter> getTurboFilterRef(TurboFilter tf) {
            for (Map.Entry<ServiceReference<TurboFilter>,TurboFilter> e : turboFilters.entrySet()) {
                if (e.getValue().equals(tf)) {
                    return e.getKey();
                }
            }
            return null;
        }

        Collection<Appender<ILoggingEvent>> getAllAppenders() {
            return appenders.values();
        }

        Map<String,Appender<ILoggingEvent>> getAppenderMap() {
            return Collections.unmodifiableMap(appenders);
        }
    }

    private void registerWebConsoleSupport() {
        Dictionary<String,Object> panelProps = new Hashtable<>(); // NOSONAR
        panelProps.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        panelProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Panel Support");
        registrations.add(bundleContext.registerService(LogPanel.class.getName(),
                new SlingLogPanel(this, bundleContext), panelProps));

        Dictionary<String,Object> printerProps = new Hashtable<>(); // NOSONAR
        printerProps.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        printerProps.put(Constants.SERVICE_DESCRIPTION, "Sling Log Configuration Printer");
        printerProps.put("felix.webconsole.label", LogConstants.PRINTER_URL);
        printerProps.put("felix.webconsole.title", "Log Files");
        printerProps.put("felix.webconsole.configprinter.modes", "always");

        // TODO need to see to add support for Inventory Feature
        registrations.add(bundleContext.registerService(SlingConfigurationPrinter.class,
            new SlingConfigurationPrinter(this), printerProps));
    }

    private void registerEventHandler() {
        Dictionary<String,Object> props = new Hashtable<>(); // NOSONAR
        props.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
        props.put(Constants.SERVICE_DESCRIPTION, "Sling Log Reset Event Handler");
        props.put("event.topics", new String[] {
                LogConstants.RESET_EVENT_TOPIC
        });

        registrations.add(bundleContext.registerService("org.osgi.service.event.EventHandler",
                new ConfigResetRequestHandler(this), props));
    }

    /**
     * Register the PackageInfoCollector as an OSGi service
     */
    void registerPackageInfoCollector() {
        //Weaving hook once registered would not be removed upon config changed
        if (packagingDataEnabled) {
            Dictionary<String,Object> props = new Hashtable<>(); // NOSONAR
            props.put(Constants.SERVICE_VENDOR, LogConstants.ASF_SERVICE_VENDOR);
            props.put(Constants.SERVICE_DESCRIPTION, LogConstants.PACKAGE_INFO_COLLECTOR_DESC);

            registrations.add(bundleContext.registerService(WeavingHook.class.getName(),
                    packageInfoCollector, props));
        }
    }
}
