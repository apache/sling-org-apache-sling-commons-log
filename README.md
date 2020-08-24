[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/) [![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-commons-log/job/master/test/?width=800&height=600) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-commons-log&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-commons-log) [![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-commons-log&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-commons-log) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.commons.log/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.commons.log%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.commons.log.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.commons.log) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Commons Log

This module is part of the [Apache Sling](https://sling.apache.org) project.

The "log" project packages the [Logback][2] library to manage logging
in OSGi environment. It provide some useful extension to the default
Logback feature set to enable better integration with OSGi. The SLF4j
API bundle must be installed along with this bundle to provide full SLF4J
logging support.
  
The Logging bundle should be installed as one of the first modules in
the OSGi framework and - provided the framework supports start levels -
be set to start at start level 1. This ensures the Logging bundle is
loaded as early as possible thus providing services to the framework
and preparing logging.

For more details refer to the [Logging Documentation][1]

Getting Started
===============

You can compile and package the jar using the following command:

    mvn package -Pide,coverage

It would build the module and also produce a test coverage report also
prepare bundle jar which is suitable to be used to run integration test
from within IDE.

[1]: http://sling.apache.org/documentation/development/logging.html
[2]: http://logback.qos.ch/
