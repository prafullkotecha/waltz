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

package org.finos.waltz.model.complexity;

import org.finos.waltz.model.tally.ImmutableTally;
import org.finos.waltz.model.tally.Tally;
import org.finos.waltz.web.WebUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class ComplexityUtilitiesTest {

    private final Tally<Long> tally = ImmutableTally.<Long>builder().id(1L).count(10).build();


    @Test
    public void mustSupplyATally() {
        assertThrows(IllegalArgumentException.class,
                ()-> ComplexityUtilities.tallyToComplexityScore(ComplexityType.CONNECTION, null, 10));
    }


    @Test
    public void negativeMaximumsAreIllegal() {
        assertThrows(IllegalArgumentException.class,
                ()-> ComplexityUtilities.tallyToComplexityScore(ComplexityType.CONNECTION, tally, -1));

    }


    @Test
    public void maxOfZeroGivesAComplexityOfZero() {
        ComplexityScore complexityScore = ComplexityUtilities.tallyToComplexityScore(ComplexityType.CONNECTION, tally, 0);
        assertEquals(0, complexityScore.score(), 0);
    }


    @Test
    public void aTallyEqualToMaxShouldGiveComplexityScoreOfOne() {
        ComplexityScore complexityScore = ComplexityUtilities.tallyToComplexityScore(ComplexityType.CONNECTION, tally, 10);
        assertEquals(1, complexityScore.score(), 0);
    }


    @Test
    public void aTallyEqualToHalfOfMaxShouldGiveComplexityScoreOfPointFive() {
        ComplexityScore complexityScore = ComplexityUtilities.tallyToComplexityScore(
                ComplexityType.CONNECTION,
                tally,
                20);
        assertEquals(0.5, complexityScore.score(), 0);
    }


    @Test
    public void negativeCountGivesNegativeScore() {
        Tally<Long> negativeTally = ImmutableTally.<Long>builder().id(1L).count(-10).build();
        ComplexityScore complexityScore = ComplexityUtilities.tallyToComplexityScore(ComplexityType.CONNECTION, negativeTally, 10);
        assertEquals(-1, complexityScore.score(), 0);
    }

}