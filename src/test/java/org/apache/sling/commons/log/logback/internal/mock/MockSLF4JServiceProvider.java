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
package org.apache.sling.commons.log.logback.internal.mock;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 *
 */
public class MockSLF4JServiceProvider implements SLF4JServiceProvider {

    private ILoggerFactory loggerFactory = null;

    @Override
    public ILoggerFactory getLoggerFactory() {
        if (loggerFactory == null) {
            loggerFactory = new MockLoggerFactory();
        }
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.12";
    }

    @Override
    public void initialize() {
        // no-op
    }
}
