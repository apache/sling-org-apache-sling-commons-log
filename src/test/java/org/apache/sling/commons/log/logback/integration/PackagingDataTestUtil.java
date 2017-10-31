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

package org.apache.sling.commons.log.logback.integration;

import java.io.InputStream;

import org.apache.sling.commons.log.logback.integration.bundle.PackageDataActivator;
import org.apache.sling.commons.log.logback.integration.bundle.TestRunnable;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.osgi.framework.Constants;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * Have to use a separate class due to TinyBundles having issue and recommends put bundle
 * creation logic in separate class
 */
public class PackagingDataTestUtil {

    public static final String TEST_BUNDLE_VERSION = "0.0.7";
    public static final String TEST_BUNDLE_NAME = "packagedatatest";

    public static InputStream createTestBundle() {
        return bundle()
                .set(Constants.BUNDLE_ACTIVATOR, PackageDataActivator.class.getName())
                .set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE_NAME)
                .set(Constants.BUNDLE_VERSION, TEST_BUNDLE_VERSION)
                .add(PackageDataActivator.class, InnerClassStrategy.NONE)
                .add(TestRunnable.class, InnerClassStrategy.NONE)
                .build(withBnd());
    }
}
