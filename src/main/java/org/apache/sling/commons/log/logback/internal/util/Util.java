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

import java.io.Closeable;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

/**
 * General utilities
 */
public class Util {

    private Util() {
        // to hide public ctor
    }

    /**
     * Close the stream for the input source
     * 
     * @param is the input source to close
     */
    public static void close(@Nullable InputSource is) {
        if (is != null) {
            Closeable c = is.getByteStream();
            if (c == null) {
                c = is.getCharacterStream();
            }
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

}
