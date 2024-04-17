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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

/**
 *
 */
class MaskingThrowableProxyTest {

    private IThrowableProxy tp;
    private MaskingThrowableProxy mtp;
    private Exception cause;
    private Exception throwable;

    @BeforeEach
    protected void beforeEach() {
        cause = new Exception("Cause of exception");
        throwable = new Exception("Something happened", cause);
        tp = new ThrowableProxy(throwable);
        mtp = new MaskingThrowableProxy(tp);
    }

    @Test
    void testMaskingThrowableProxyWithNullThrowable() {
        IThrowableProxy tp = Mockito.mock(IThrowableProxy.class);
        MaskingThrowableProxy mtp2 = new MaskingThrowableProxy(tp);
        assertNull(mtp2.getCause());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#getMessage()}.
     */
    @Test
    void testGetMessage() {
        assertEquals("Something happened", mtp.getMessage());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#getCause()}.
     */
    @Test
    void testGetCause() {
        IThrowableProxy c = mtp.getCause();
        assertTrue (c instanceof MaskingThrowableProxy);
        assertEquals("Cause of exception", c.getMessage());
    }
    @Test
    void testGetCauseWithNullThrowableCause() {
        throwable = new Exception("Something happened");
        tp = new ThrowableProxy(throwable);
        mtp = new MaskingThrowableProxy(tp);

        IThrowableProxy c = mtp.getCause();
        assertNull(c);
    }
    @Test
    void testGetCauseWithProxiedCauseIsSelf() {
        throwable = new Exception("Something happened");
        tp = Mockito.spy(new ThrowableProxy(throwable));
        // not sure how this would happen in the wild, but mock
        //  the cause being itself?
        Mockito.doReturn(tp).when(tp).getCause();
        mtp = new MaskingThrowableProxy(tp);

        IThrowableProxy c = mtp.getCause();
        assertSame(mtp, c);
    }
    @Test
    void testGetCauseWithProxiedCauseIsWrapper() {
        throwable = new Exception("Something happened");
        tp = Mockito.spy(new ThrowableProxy(throwable));
        mtp = new MaskingThrowableProxy(tp);
        // not sure how this would happen in the wild, but mock
        //  the cause being itself?
        Mockito.doReturn(mtp).when(tp).getCause();

        IThrowableProxy c = mtp.getCause();
        assertSame(mtp, c);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#getClassName()}.
     */
    @Test
    void testGetClassName() {
        assertEquals("java.lang.Exception", mtp.getClassName());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#getStackTraceElementProxyArray()}.
     */
    @Test
    void testGetStackTraceElementProxyArray() {
        assertNotNull(mtp.getStackTraceElementProxyArray());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#getCommonFrames()}.
     */
    @Test
    void testGetCommonFrames() {
        assertEquals(0, mtp.getCommonFrames());
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#getSuppressed()}.
     */
    @Test
    void testGetSuppressed() {
        IThrowableProxy[] suppressed = mtp.getSuppressed();
        assertArrayEquals(new IThrowableProxy[0], suppressed);
    }
    @Test
    void testGetSuppressedWithNullSuppressed() {
        tp = Mockito.spy(new ThrowableProxy(throwable));
        Mockito.doReturn(null).when(tp).getSuppressed();
        mtp = new MaskingThrowableProxy(tp);

        IThrowableProxy[] suppressed = mtp.getSuppressed();
        assertNull(suppressed);
    }
    @Test
    void testGetSuppressedWithNotEmptySuppressed() {
        tp = Mockito.spy(new ThrowableProxy(throwable));
        IThrowableProxy[] tosuppress = new IThrowableProxy[] {tp.getCause()};
        Mockito.doReturn(tosuppress).when(tp).getSuppressed();
        mtp = new MaskingThrowableProxy(tp);

        IThrowableProxy[] suppressed = mtp.getSuppressed();
        assertEquals(1, suppressed.length);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.MaskingMessageUtil.MaskingThrowableProxy#isCyclic()}.
     */
    @Test
    void testIsCyclic() {
        assertFalse(mtp.isCyclic());
    }

}
