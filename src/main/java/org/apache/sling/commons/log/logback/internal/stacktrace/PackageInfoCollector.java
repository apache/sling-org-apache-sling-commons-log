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

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

public class PackageInfoCollector implements WeavingHook{
    private final ConcurrentMap<String, Set<String>> pkgInfoMapping = new ConcurrentHashMap<>();

    @Override
    public void weave(WovenClass wovenClass) {
        add(wovenClass.getBundleWiring().getBundle(), wovenClass.getClassName());
    }

    public int size() {
        return pkgInfoMapping.size();
    }

    void add(Bundle bundle, String className) {
        String packageName = getPackageName(className);

        Set<String> infos = pkgInfoMapping.computeIfAbsent(packageName, k -> Collections.synchronizedSet(new HashSet<>()));
        infos.add(getInfo(bundle));
    }

    String getBundleInfo(String className) {
        if (className == null) {
            return null;
        }
        String packageName = getPackageName(className);
        Set<String> infos = pkgInfoMapping.get(packageName);

        //If multiple infos are found then we cannot determine the exact version
        //so better not to provide any info
        if (infos == null || infos.size() > 1 || infos.size() == 0) {
            return null;
        }
        return infos.iterator().next();
    }

    private static String getInfo(Bundle bundle) {
        return bundle.getSymbolicName() + ":" + bundle.getVersion();
    }

    static String getPackageName(String className) {
        int lastIndexOfDot = className.lastIndexOf('.');
        String result = "";
        if (lastIndexOfDot > 0){
            result = className.substring(0, lastIndexOfDot);
        }
        return result;
    }
}
