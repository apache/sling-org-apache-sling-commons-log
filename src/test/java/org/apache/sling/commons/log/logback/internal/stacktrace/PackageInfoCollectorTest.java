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
package org.apache.sling.commons.log.logback.internal.stacktrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.commons.log.helpers.ReflectionTools;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextBuilder;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

/**
 *
 */
@ExtendWith(OsgiContextExtension.class)
class PackageInfoCollectorTest {
    protected final OsgiContext context = new OsgiContextBuilder().build();

    private PackageInfoCollector collector = new PackageInfoCollector();

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.PackageInfoCollector#weave(org.osgi.framework.hooks.weaving.WovenClass)}.
     */
    @Test
    void testWeave() {
        // mock a woven class
        Bundle bundle = newBundle("foo.bundle", "0.0.7");
        WovenClass wovenClass = newWovenClass(bundle, "com.example.Foo");

        collector.weave(wovenClass);
        assertEquals(1, collector.size());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.PackageInfoCollector#size()}.
     */
    @Test
    void testSize() {
        assertEquals(0, collector.size());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.PackageInfoCollector#add(org.osgi.framework.Bundle, java.lang.String)}.
     */
    @Test
    void testAdd() {
        Bundle bundle = newBundle("foo.bundle", "0.0.7");
        String className = "com.example.Foo";
        collector.add(bundle, className);
        assertEquals(1, collector.size());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.PackageInfoCollector#getBundleInfo(java.lang.String)}.
     */
    @Test
    void testGetBundleInfo() {
        Bundle bundle = newBundle("foo.bundle", "0.0.7");
        String className = "com.example.Foo";
        collector.add(bundle, className);
        assertEquals("foo.bundle:0.0.7", collector.getBundleInfo(className));
    }

    @Test
    void testGetBundleInfoForNotAddedClass() {
        String className = "com.example.Foo";
        assertNull(collector.getBundleInfo(className));
    }

    @Test
    void testGetBundleInfoForNull() {
        assertNull(collector.getBundleInfo(null));
    }

    /**
     * For case where same package is present in multiple bundles then no bundle info is provided
     */
    @Test
    void testGetBundleInfoForDuplicates() {
        Bundle bundle = newBundle("foo.bundle", "0.0.7");
        String className = "com.example.Foo";
        collector.add(bundle, className);

        // mock a second bundle with the same class name
        Bundle bundle2 = newBundle("bar.bundle", "0.0.6");
        collector.add(bundle2, className);

        assertNull(collector.getBundleInfo(className));
    }

    @Test
    void testGetBundleInfoForEmptyPkgInfoSet() {
        // mock the set being empty
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Set<String>> pkgInfoMapping = ReflectionTools.getFieldWithReflection(collector, "pkgInfoMapping", ConcurrentMap.class);
        pkgInfoMapping.put("org.apache.sling.commons.log.logback.internal.stacktrace", Collections.synchronizedSet(new HashSet<>()));

        String className = getClass().getName();
        assertNull(collector.getBundleInfo(className));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.stacktrace.PackageInfoCollector#getPackageName(java.lang.String)}.
     */
    @Test
    void testPackageName() throws Exception{
        assertEquals("com.foo", PackageInfoCollector.getPackageName("com.foo.TestClass"));
        assertEquals("", PackageInfoCollector.getPackageName("packageInDefaultClass"));
    }

    private Bundle newBundle(String name, String version){
        Bundle b = mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn(name);
        when(b.getVersion()).thenReturn(Version.parseVersion(version));
        return b;
    }

    private WovenClass newWovenClass(Bundle bundle, String className){
        WovenClass woven = mock(WovenClass.class);
        BundleWiring wiring = mock(BundleWiring.class);
        when(woven.getBundleWiring()).thenReturn(wiring);

        when(wiring.getBundle()).thenReturn(bundle);
        when(woven.getClassName()).thenReturn(className);
        return woven;
    }

}
