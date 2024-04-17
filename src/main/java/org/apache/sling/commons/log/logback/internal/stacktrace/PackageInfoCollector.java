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
package org.apache.sling.commons.log.logback.internal.stacktrace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * WeavingHook to keep track of which bundle contains
 * the java package for each woven class
 */
public class PackageInfoCollector implements WeavingHook {
    /**
     * package to bundle mapping where the key is the package name and
     * the value is the set of bundles that contain the package
     */
    private final ConcurrentMap<String, Set<String>> pkgInfoMapping = new ConcurrentHashMap<>();

    @Override
    public void weave(@NotNull WovenClass wovenClass) {
        add(wovenClass.getBundleWiring().getBundle(), wovenClass.getClassName());
    }

    /**
     * Returns the number of packages that have been encountered
     * 
     * @return the number of packages
     */
    public int size() {
        return pkgInfoMapping.size();
    }

    /**
     * Add the package mapping for the class name
     *
     * @param bundle the bundle the class came from
     * @param className the name of the class
     */
    void add(@NotNull Bundle bundle, @NotNull String className) {
        String packageName = getPackageName(className);

        Set<String> infos = pkgInfoMapping.computeIfAbsent(packageName, k -> Collections.synchronizedSet(new HashSet<>()));
        infos.add(getInfo(bundle));
    }

    /**
     * Gets the bundle info for the supplied classname
     *
     * @param className the class name to lookup
     * @return the bundle info string or null if not found or ambiguous
     */
    @Nullable String getBundleInfo(@Nullable String className) {
        if (className == null) {
            return null;
        }
        String packageName = getPackageName(className);
        Set<String> infos = pkgInfoMapping.get(packageName);

        //If multiple infos are found then we cannot determine the exact version
        //so better not to provide any info
        if (infos == null || infos.size() > 1 || infos.isEmpty()) {
            return null;
        }
        return infos.stream().findFirst().orElse(null);
    }

    /**
     * Create an info string for the bundle
     *
     * @param bundle the bundle to process
     * @return information string describing the bundle
     */
    private static @NotNull String getInfo(@NotNull Bundle bundle) {
        return bundle.getSymbolicName() + ":" + bundle.getVersion();
    }

    /**
     * Calculate the package name for the class name
     *
     * @param className the class name to process
     * @return the name of the package (or empty string of the root package)
     */
    static @NotNull String getPackageName(@NotNull String className) {
        int lastIndexOfDot = className.lastIndexOf('.');
        String result = "";
        if (lastIndexOfDot > 0) {
            result = className.substring(0, lastIndexOfDot);
        }
        return result;
    }

}
