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
package org.apache.sling.commons.log.logback.internal.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.InputSource;

/**
 *
 */
class UtilTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.Util#close(org.xml.sax.InputSource)}.
     */
    @Test
    void testClose() throws IOException {
        assertDoesNotThrow(() -> Util.close(null));

        // mock an InputSource
        InputSource source = Mockito.mock(InputSource.class);
        InputStream in = Mockito.mock(InputStream.class);
        Mockito.doReturn(in).when(source).getByteStream();

        assertDoesNotThrow(() -> Util.close(source));
    }

    @Test
    void testCloseWithIgnoredException() throws IOException {
        // mock an InputSource that throws IOException during close
        InputSource source = Mockito.mock(InputSource.class);
        InputStream in = Mockito.mock(InputStream.class);
        Mockito.doReturn(in).when(source).getByteStream();
        Mockito.doThrow(IOException.class).when(in).close();

        assertDoesNotThrow(() -> Util.close(source));
    }

}
