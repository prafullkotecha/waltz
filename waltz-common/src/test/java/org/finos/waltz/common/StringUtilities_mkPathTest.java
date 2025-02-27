/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific
 *
 */

package org.finos.waltz.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.waltz.common.StringUtilities.firstChar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringUtilities_mkPathTest {

    @Test
    public void mkPathWithNoSegmentsReturnsEmptyString() {
        assertEquals("", StringUtilities.mkPath());
    }

    @Test
    public void mkPathWithSegmentsReturnsSlashDelimitedConcatenation() {
        assertEquals("hello/world", StringUtilities.mkPath("hello", "world"));
    }


    @Test
    public void mkPathWithNonStringSegmentsReturnsToStringEquivalent() {
        assertEquals("hello/23/world", StringUtilities.mkPath("hello", 23, "world"));
    }


    @Test
    public void mkPathWithOneSegmentsReturnsBasicString() {
        assertEquals("test", StringUtilities.mkPath("test"));
    }


    @Test
    public void mkPathWithEmbeddedSegmentsPreservesSlashes() {
        assertEquals("test/one", StringUtilities.mkPath("test/one"));
    }


    @Test
    public void mkPathWithEmbeddedSegmentsReducesSlashes() {
        assertEquals("test/one", StringUtilities.mkPath("test///one"));
    }


    @Test
    public void mkPathWithNullSegmentsIsNotAllowed() {
        assertThrows(IllegalArgumentException.class,
                ()-> StringUtilities.mkPath("test", null));
    }


    @Test
    public void mkPathWithEmptySegmentsIsNotAllowed() {

        assertThrows(IllegalArgumentException.class,
                ()-> StringUtilities.mkPath("test", ""));
    }


}
