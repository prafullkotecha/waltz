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

package org.finos.waltz.model.physical_flow;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.finos.waltz.model.Criticality;
import org.finos.waltz.model.DescriptionProvider;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableFlowAttributes.class)
@JsonDeserialize(as = ImmutableFlowAttributes.class)
public abstract class FlowAttributes implements DescriptionProvider {

    public abstract TransportKindValue transport();
    public abstract FrequencyKind frequency();
    public abstract Criticality criticality();
    public abstract int basisOffset();

}
