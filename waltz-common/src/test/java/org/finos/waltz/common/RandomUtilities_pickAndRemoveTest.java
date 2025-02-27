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

import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.finos.waltz.common.ListUtilities.asList;
import static org.finos.waltz.common.RandomUtilities.pickAndRemove;
import static org.junit.jupiter.api.Assertions.*;

public class RandomUtilities_pickAndRemoveTest {

    @Test
    public void cannotPickAndRemoveFromNull() {
        assertThrows(IllegalArgumentException.class,
                ()-> pickAndRemove(null));
    }


    @Test
    public void cannotPickAndRemoveFromEmpty() {

        assertThrows(IllegalArgumentException.class,
                ()-> pickAndRemove(Collections.emptyList()));
    }


    @Test
    public void pickFromSingletonListReturnsElementAndEmptyList() {
        Tuple2<String, List<String>> r = pickAndRemove(asList("a"));
        assertEquals(r.v1, "a");
        assertTrue(r.v2.isEmpty());
    }


    @Test
    public void pickFromMultiValuedListReturnsElementAndListWithoutThatElement() {
        Tuple2<String, List<String>> r = pickAndRemove(asList("a", "b"));
        String picked = r.v1;
        Collection<String> remainder = r.v2;

        assertTrue(picked.equals("a") || picked.equals("b"));
        assertFalse(remainder.isEmpty());
        assertEquals(remainder.size(), 1);
        assertFalse(remainder.contains(picked));
    }


}
