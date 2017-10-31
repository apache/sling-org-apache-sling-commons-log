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

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageInfoCollectorTest {
    private PackageInfoCollector collector = new PackageInfoCollector();

    @Test
    public void packageName() throws Exception{
        assertEquals("com.foo", PackageInfoCollector.getPackageName("com.foo.TestClass"));
        assertEquals("", PackageInfoCollector.getPackageName("packageInDefaultClass"));
    }

    @Test
    public void getBundleInfo() throws Exception{
        Bundle bundle = newBundle("foo.bundle", "0.0.7");
        collector.weave(newWovenClass(bundle, "com.example.Foo"));

        assertEquals("foo.bundle:0.0.7",collector.getBundleInfo("com.example.Foo"));
        assertEquals("foo.bundle:0.0.7",collector.getBundleInfo("com.example.Bar"));
        assertNull(collector.getBundleInfo("com.example2.Bar"));
    }

    /**
     * For case where same package is present in multiple bundles then no bundle info is provided
     */
    @Test
    public void duplicates() throws Exception{
        collector.weave(newWovenClass(newBundle("foo.bundle", "0.0.7"), "com.example.Foo"));
        collector.weave(newWovenClass(newBundle("foo.bundle2", "0.0.8"), "com.example.Bar"));

        assertNull(collector.getBundleInfo("com.example.Bar"));
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