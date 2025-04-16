[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-commons-log&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-commons-log)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-commons-log&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-commons-log)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.commons.log.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.commons.log) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Commons Log

This module is part of the [Apache Sling](https://sling.apache.org) project.

The "log" module provides some useful extension to the default [Logback][2] feature set to enable better integration with OSGi. 

## Installation

The Logging bundle should be installed as one of the first modules in the OSGi framework and - provided the framework supports start levels - be set to start at start level 1. This ensures the Logging bundle is loaded as early as possible thus providing services to the framework and preparing logging.

For more details refer to the [Logging Documentation][1]

## Getting Started

You can compile and package the jar using the following command:

    mvn package


[1]: https://sling.apache.org/documentation/development/logging.html
[2]: https://logback.qos.ch/
