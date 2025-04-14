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

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringWriter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * Utilities to help processing XML
 */
public class XmlUtil {

    /**
     * Constructor
     */
    private XmlUtil() {
        // to hide public ctor
    }

    /**
     * Return a pretty string representation of the xml supplied
     *
     * @param is the source for the xml content
     * @return the pretty formatted xml
     */
    public static @NotNull String prettyPrint(@NotNull InputSource is) {
        try (StringWriter strWriter = new StringWriter()) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // to protect a javax.xml.transform.TransformerFactory from XXE
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(strWriter);
            Source source = new SAXSource(is);
            transformer.transform(source, result);
            return strWriter.toString();
        } catch (Exception e) {
            // Catch generic error as panel should still work if xml apis are
            // not
            // resolved
            LoggerFactory.getLogger(XmlUtil.class).warn("Error occurred while transforming xml", e);
        } finally {
            Util.close(is);
        }

        return "Source not found";
    }

    /**
     * Escape the special characters of the input to be safe as an xml string
     *
     * @param input the input to process
     * @return the input with the xml special characters replaced with entities
     */
    public static @Nullable String escapeXml(@Nullable final String input) {
        if (input == null) {
            return null;
        }

        final StringBuilder b = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '&') {
                b.append("&amp;");
            } else if (c == '<') {
                b.append("&lt;");
            } else if (c == '>') {
                b.append("&gt;");
            } else if (c == '"') {
                b.append("&quot;");
            } else if (c == '\'') {
                b.append("&apos;");
            } else {
                b.append(c);
            }
        }
        return b.toString().replace("$", "&#37;");
    }
}
