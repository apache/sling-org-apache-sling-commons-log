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
package org.apache.sling.commons.log.logback.internal.util;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;

import ch.qos.logback.classic.Level;
import org.apache.sling.commons.log.helpers.LogCapture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 */
class XmlUtilTest {

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.XmlUtil#prettyPrint(InputSource)}.
     */
    @Test
    void testPrettyPrintWithCaughtException() throws Exception {
        String source = "<hello><world>Text Here</world>";
        TestUtils.doWorkWithoutRootConsoleAppender(() -> {
            try (Reader reader = new StringReader(source)) {
                // verify that the msg was logged
                try (LogCapture capture = new LogCapture(XmlUtil.class.getName(), true)) {
                    assertEquals("Source not found", XmlUtil.prettyPrint(new InputSource(reader)));

                    // verify the msg was logged
                    capture.assertContains(Level.WARN, "Error occurred while transforming xml");
                }
            }
            return null;
        });
    }

    @Test
    void testPrettyPrintWithInputStreamSource() throws Exception {
        InputSource source = Mockito.mock(InputSource.class);
        ByteArrayInputStream byteStream =
                new ByteArrayInputStream("<hello><world>Text Here</world></hello>".getBytes());
        Mockito.doReturn(byteStream).when(source).getByteStream();
        assertEquals(
                MessageFormat.format(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello>{0}"
                                + "  <world>Text Here</world>{0}"
                                + "</hello>{0}"
                                + "",
                        System.lineSeparator()),
                XmlUtil.prettyPrint(source));
    }

    @Test
    void testPrettyPrintWithReaderSource() throws Exception {
        InputSource source = Mockito.mock(InputSource.class);
        Reader reader = new StringReader("<hello><world>Text Here</world></hello>");
        Mockito.doReturn(reader).when(source).getCharacterStream();
        assertEquals(
                MessageFormat.format(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello>{0}"
                                + "  <world>Text Here</world>{0}"
                                + "</hello>{0}"
                                + "",
                        System.lineSeparator()),
                XmlUtil.prettyPrint(source));
    }

    @Test
    void testPrettyPrintWithNullSource() throws Exception {
        TestUtils.doWorkWithoutRootConsoleAppender(() -> {
            InputSource source = Mockito.mock(InputSource.class);
            assertEquals("Source not found", XmlUtil.prettyPrint(source));
            return null;
        });
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.util.XmlUtil#escapeXml(java.lang.String)}.
     */
    @Test
    void testEscapeXml() {
        assertNull(XmlUtil.escapeXml(null));
        assertEquals("&lt;p&gt;Hello&lt;/p&gt;", XmlUtil.escapeXml("<p>Hello</p>"));
        assertEquals("Hello &amp; World", XmlUtil.escapeXml("Hello & World"));
        assertEquals("Hello &quot;World&quot;", XmlUtil.escapeXml("Hello \"World\""));
        assertEquals("Hello &apos;World&apos;", XmlUtil.escapeXml("Hello 'World'"));
    }
}
