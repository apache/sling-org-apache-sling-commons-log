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

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.InputStream;
import java.net.URL;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Constants;

/**
 * Have to use a separate class due to TinyBundles having issue and recommends put bundle
 * creation logic in separate class
 */
public class PackagingDataTestUtil {

    public static final String TEST_BUNDLE_VERSION = "0.0.7";
    public static final String TEST_BUNDLE_NAME = "packagedatatest";

    public static InputStream createTestBundle() {
        //Avoid referring to test bundle classes otherwise they get loaded in 2 bundles i.e.
        //pax exam probe bundle and our packagedatatest. So we refer only by class name strings
        String activatorClassName = "org.apache.sling.commons.log.logback.integration.bundle.PackageDataActivator";
        // not needed and causes problems when running with Java 11 since the version of bndlib is too old
        TinyBundle tb = bundle()
                .set(Constants.BUNDLE_ACTIVATOR, activatorClassName)
                .set("-noee", Boolean.TRUE.toString())
                .set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE_NAME)
                .set(Constants.BUNDLE_VERSION, TEST_BUNDLE_VERSION);
        add(tb, "org.apache.sling.commons.log.logback.integration.bundle.TestRunnable");
        add(tb, activatorClassName);
        return tb.build(withBnd());
    }

    private static void add(TinyBundle tb, String className) {
        String name = asResource(className);
        tb.add(name, asResourceURL(name));
    }

    private static String asResource( String klass ) {
        return klass.replace( '.', '/' ) + ".class";
    }

    private static URL asResourceURL(String klass ) {
        URL u = PackagingDataTestUtil.class.getResource("/" + klass);
        if (u == null) {
            throw new RuntimeException("No resource found for "+klass);
        }
        return u;
    }
}
