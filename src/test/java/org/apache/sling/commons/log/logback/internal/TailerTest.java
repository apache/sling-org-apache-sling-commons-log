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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
class TailerTest {
    private StringWriter strWriter;
    private PrintWriter pw;
    private Path tempFile;

    @BeforeEach
    protected void beforeEach() throws IOException {
        strWriter = new StringWriter();
        pw = new PrintWriter(strWriter);
        tempFile = Files.createTempFile("tailWithCRLF", ".log");
    }

    @AfterEach
    protected void afterEach() throws IOException {
        Files.delete(tempFile);
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.Tailer#Tailer(org.apache.sling.commons.log.logback.internal.Tailer.TailerListener, int)}.
     */
    @Test
    void testTailWithTailerListener() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (int i = 1; i < 25; i++) {
                writer.append("log message ").append(Integer.toString(i)).append("\r\n");
            }
            writer.append("\r\n");
        }

        FilteringListener listener = new FilteringListener(pw, ".+");
        Tailer tailer = new Tailer(listener, 10);

        tailer.tail(tempFile.toFile());

        String output = strWriter.toString();
        assertFalse(output.contains("log message 4"));
        assertTrue(output.contains("log message 24"));
    }

    /**
     * Test method for {@link org.apache.sling.commons.log.logback.internal.Tailer#tail(java.io.File)}.
     */
    @Test
    void testTail() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (int i = 1; i < 25; i++) {
                writer.append("log message ").append(Integer.toString(i)).append("\r\n");
            }
            writer.append("\r\n");
        }

        Tailer tailer = new Tailer(pw, 10);

        tailer.tail(tempFile.toFile());

        String output = strWriter.toString();
        assertFalse(output.contains("log message 4"));
        assertTrue(output.contains("log message 24"));
    }

    @Test
    void testTailEmptyFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            // write nothing
        }

        Tailer tailer = new Tailer(pw, 10);

        tailer.tail(tempFile.toFile());

        String output = strWriter.toString();
        assertTrue(output.isEmpty());
    }

    @Test
    void testTailFullFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (int i = 1; i < 15; i++) {
                writer.append("log message ").append(Integer.toString(i)).append("\r\n");
            }
            writer.append("\r\n");
        }

        Tailer tailer = new Tailer(pw, 20);

        tailer.tail(tempFile.toFile());

        String output = strWriter.toString();
        assertTrue(output.contains("log message 1"));
        assertTrue(output.contains("log message 14"));
    }

    @Test
    void testTailNoNewlinesFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (int i = 1; i < 200; i++) {
                writer.append("log message ").append(Integer.toString(i)).append(" ");
            }
        }

        Tailer tailer = new Tailer(pw, 20);

        tailer.tail(tempFile.toFile());

        String output = strWriter.toString();
        assertTrue(output.contains("log message 1"));
        assertTrue(output.contains("log message 14"));
    }

    @Test
    void testTailWithCRLF() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (int i = 1; i < 15; i++) {
                writer.append("log message ").append(Integer.toString(i)).append("\r\n");
            }
            writer.append("log message 15").append("\r");
            writer.append("log message 16").append("\r\r");
            writer.append("log message 17").append("\n");
            writer.append("\r\n");
        }

        Tailer tailer = new Tailer(pw, 10);

        tailer.tail(tempFile.toFile());

        String output = strWriter.toString();
        assertTrue(output.contains("log message 1"));
        assertTrue(output.contains("log message 14"));
    }


    @Test
    void testEmpty() throws Exception{
        File f1 = tempFile.toFile();
        LineCollector listener = new LineCollector();
        Tailer t = new Tailer(listener, 10);
        t.tail(f1);
        assertThat(listener.lines, empty());
    }

    @Test
    void testLessAndMore() throws Exception{
        File f1 = tempFile.toFile();
        writeToFile(f1, asList("a", "b", "c", "d"));
        LineCollector listener = new LineCollector();
        new Tailer(listener, 2).tail(f1);
        assertThat(listener.lines, contains("c", "d"));

        listener.reset();
        new Tailer(listener, 10).tail(f1);
        assertThat(listener.lines, contains("a", "b", "c", "d"));
    }

    @Test
    void randomTest() throws Exception{
        File f1 = tempFile.toFile();
        List<String> lines = createRandomLines(Tailer.BUFFER_SIZE * 10);
        int numOfLines = lines.size();
        writeToFile(f1, lines);
        Random rnd = new Random();
        int n = rnd.nextInt(numOfLines/2);
        LineCollector listener = new LineCollector();
        new Tailer(listener, n).tail(f1);
        assertEquals(listener.lines, lines.subList(numOfLines - n, numOfLines));

    }

    private List<String> createRandomLines(int totalSize){

        List<String> result = new ArrayList<String>();
        int size = 0;
        Random rnd = new Random();
        while(true){
            int l = rnd.nextInt(100);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l; i++) {
                sb.append('x');
            }
            size += sb.length();
            result.add(sb.toString());

            if (size > totalSize){
                break;
            }
        }
        return result;
    }

    private void writeToFile(File f, List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean firstLine = true;
        for (String line : lines){
            if (firstLine){
                firstLine = false;
            } else {
                sb.append("\n");
            }
            sb.append(line);

        }
        FileUtils.write(f, sb, Charset.defaultCharset());
    }

    private static class LineCollector implements Tailer.TailerListener {
        final List<String> lines = new ArrayList<String>();
        @Override
        public void handle(String line) {
            lines.add(line);
        }

        public void reset(){
            lines.clear();
        }
    }


}
