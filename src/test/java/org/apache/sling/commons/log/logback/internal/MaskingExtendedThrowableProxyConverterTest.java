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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingExtendedThrowableProxyConverter;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

/**
 *
 */
class MaskingExtendedThrowableProxyConverterTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingExtendedThrowableProxyConverter#throwableProxyToString(ch.qos.logback.classic.spi.IThrowableProxy)}.
     */
    @Test
    void testThrowableProxyToStringIThrowableProxy() {
        MaskingExtendedThrowableProxyConverter t = new MaskingExtendedThrowableProxyConverter();
        IThrowableProxy tp = new ThrowableProxy(new Exception("Something happened"));
        assertEquals("java.lang.Exception: Something happened\n", t.throwableProxyToString(tp));
    }

}
