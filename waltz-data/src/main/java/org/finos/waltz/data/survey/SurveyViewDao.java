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

package org.finos.waltz.data.survey;

import org.finos.waltz.data.InlineSelectFieldFactory;
import org.finos.waltz.model.EntityKind;
import org.finos.waltz.model.HierarchyQueryScope;
import org.finos.waltz.model.IdSelectionOptions;
import org.finos.waltz.model.ReleaseLifecycleStatus;
import org.finos.waltz.model.survey.*;
import org.finos.waltz.schema.tables.records.SurveyInstanceRecord;
import org.finos.waltz.schema.tables.records.SurveyRunRecord;
import org.jooq.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.finos.waltz.common.Checks.checkNotNull;
import static org.finos.waltz.common.ListUtilities.newArrayList;
import static org.finos.waltz.common.StringUtilities.splitThenMap;
import static org.finos.waltz.data.JooqUtilities.maybeReadRef;
import static org.finos.waltz.model.EntityReference.mkRef;
import static org.finos.waltz.schema.Tables.*;

@Repository
public class SurveyViewDao {

    private static final Field<String> ENTITY_NAME_FIELD = InlineSelectFieldFactory.mkNameField(
                SURVEY_INSTANCE.ENTITY_ID,
                SURVEY_INSTANCE.ENTITY_KIND,
                newArrayList(EntityKind.APPLICATION, EntityKind.CHANGE_INITIATIVE))
            .as("entity_name");

    private static final Field<String> QUALIFIER_NAME_FIELD = InlineSelectFieldFactory.mkNameField(
                SURVEY_INSTANCE.ENTITY_QUALIFIER_ID,
                SURVEY_INSTANCE.ENTITY_QUALIFIER_KIND,
                newArrayList(EntityKind.MEASURABLE))
            .as("qualifier_entity_name");

    private static final Field<String> EXTERNAL_ID_FIELD = InlineSelectFieldFactory.mkExternalIdField(
            SURVEY_INSTANCE.ENTITY_ID,
            SURVEY_INSTANCE.ENTITY_KIND,
            newArrayList(EntityKind.APPLICATION, EntityKind.CHANGE_INITIATIVE))
            .as("external_id");

    private static final String ID_SEPARATOR = ";";

    private static final Condition IS_ORIGINAL_INSTANCE_CONDITION = SURVEY_INSTANCE.ORIGINAL_INSTANCE_ID.isNull();

    private static final RecordMapper<Record, SurveyInstanceInfo> TO_DOMAIN_MAPPER = r -> {
        SurveyInstanceRecord instanceRecord = r.into(SURVEY_INSTANCE);
        ImmutableSurveyInstance surveyInstance = ImmutableSurveyInstance.builder()
                .id(instanceRecord.getId())
                .surveyRunId(instanceRecord.getSurveyRunId())
                .surveyEntity(mkRef(
                        EntityKind.valueOf(instanceRecord.getEntityKind()),
                        instanceRecord.getEntityId(),
                        r.getValue(ENTITY_NAME_FIELD)))
                .surveyEntityExternalId(r.getValue(EXTERNAL_ID_FIELD))
                .status(SurveyInstanceStatus.valueOf(instanceRecord.getStatus()))
                .dueDate(instanceRecord.getDueDate().toLocalDate())
                .approvalDueDate(instanceRecord.getApprovalDueDate().toLocalDate())
                .submittedAt(ofNullable(instanceRecord.getSubmittedAt()).map(Timestamp::toLocalDateTime).orElse(null))
                .submittedBy(instanceRecord.getSubmittedBy())
                .approvedAt(ofNullable(instanceRecord.getApprovedAt()).map(Timestamp::toLocalDateTime).orElse(null))
                .approvedBy(instanceRecord.getApprovedBy())
                .originalInstanceId(instanceRecord.getOriginalInstanceId())
                .owningRole(instanceRecord.getOwningRole())
                .name(instanceRecord.getName())
                .qualifierEntity(maybeReadRef(
                        r,
                        SURVEY_INSTANCE.ENTITY_QUALIFIER_KIND,
                        SURVEY_INSTANCE.ENTITY_QUALIFIER_ID,
                        QUALIFIER_NAME_FIELD)
                        .orElse(null))
                .build();

        SurveyRunRecord runRecord = r.into(SURVEY_RUN);
        ImmutableSurveyRun run = ImmutableSurveyRun.builder()
                .id(runRecord.getId())
                .surveyTemplateId(runRecord.getSurveyTemplateId())
                .name(runRecord.getName())
                .description(runRecord.getDescription())
                .selectionOptions(IdSelectionOptions.mkOpts(
                        mkRef(
                                EntityKind.valueOf(runRecord.getSelectorEntityKind()),
                                runRecord.getSelectorEntityId()),
                        HierarchyQueryScope.valueOf(runRecord.getSelectorHierarchyScope())))
                .involvementKindIds(splitThenMap(
                        runRecord.getInvolvementKindIds(),
                        ID_SEPARATOR,
                        Long::valueOf))
                .issuedOn(ofNullable(runRecord.getIssuedOn()).map(Date::toLocalDate))
                .dueDate(runRecord.getDueDate().toLocalDate())
                .approvalDueDate(runRecord.getApprovalDueDate().toLocalDate())
                .issuanceKind(SurveyIssuanceKind.valueOf(runRecord.getIssuanceKind()))
                .ownerId(runRecord.getOwnerId())
                .contactEmail(runRecord.getContactEmail())
                .status(SurveyRunStatus.valueOf(runRecord.getStatus()))
                .build();

        return ImmutableSurveyInstanceInfo.builder()
                .surveyInstance(surveyInstance)
                .surveyRun(run)
                .surveyTemplateRef(mkRef(
                        EntityKind.SURVEY_TEMPLATE,
                        r.get(SURVEY_TEMPLATE.ID),
                        r.get(SURVEY_TEMPLATE.NAME),
                        r.get(SURVEY_TEMPLATE.DESCRIPTION),
                        r.get(SURVEY_TEMPLATE.EXTERNAL_ID)))
                .build();
    };

    private final DSLContext dsl;


    @Autowired
    public SurveyViewDao(DSLContext dsl) {
        checkNotNull(dsl, "dsl cannot be null");

        this.dsl = dsl;
    }


    public SurveyInstanceInfo getById(long instanceId) {
        return dsl
                .select(SURVEY_INSTANCE.fields())
                .select(SURVEY_RUN.fields())
                .select(SURVEY_TEMPLATE.NAME,
                        SURVEY_TEMPLATE.ID,
                        SURVEY_TEMPLATE.DESCRIPTION,
                        SURVEY_TEMPLATE.EXTERNAL_ID)
                .select(ENTITY_NAME_FIELD)
                .select(QUALIFIER_NAME_FIELD)
                .select(EXTERNAL_ID_FIELD)
                .from(SURVEY_INSTANCE)
                .innerJoin(SURVEY_RUN).on(SURVEY_INSTANCE.SURVEY_RUN_ID.eq(SURVEY_RUN.ID))
                .innerJoin(SURVEY_TEMPLATE).on(SURVEY_RUN.SURVEY_TEMPLATE_ID.eq(SURVEY_TEMPLATE.ID))
                .where(SURVEY_INSTANCE.ID.eq(instanceId))
                .fetchOne(TO_DOMAIN_MAPPER);
    }


    public Set<SurveyInstanceInfo> findForRecipient(long personId) {
        return dsl
                .select(SURVEY_INSTANCE.fields())
                .select(SURVEY_RUN.fields())
                .select(SURVEY_TEMPLATE.NAME,
                        SURVEY_TEMPLATE.ID,
                        SURVEY_TEMPLATE.DESCRIPTION,
                        SURVEY_TEMPLATE.EXTERNAL_ID)
                .select(ENTITY_NAME_FIELD)
                .select(QUALIFIER_NAME_FIELD)
                .select(EXTERNAL_ID_FIELD)
                .from(SURVEY_INSTANCE)
                .innerJoin(SURVEY_INSTANCE_RECIPIENT)
                .on(SURVEY_INSTANCE_RECIPIENT.SURVEY_INSTANCE_ID.eq(SURVEY_INSTANCE.ID))
                .innerJoin(SURVEY_RUN).on(SURVEY_INSTANCE.SURVEY_RUN_ID.eq(SURVEY_RUN.ID))
                .innerJoin(SURVEY_TEMPLATE).on(SURVEY_RUN.SURVEY_TEMPLATE_ID.eq(SURVEY_TEMPLATE.ID))
                .where(SURVEY_INSTANCE_RECIPIENT.PERSON_ID.eq(personId))
                .and(IS_ORIGINAL_INSTANCE_CONDITION)
                .and(SURVEY_INSTANCE.STATUS.ne(SurveyInstanceStatus.WITHDRAWN.name()))
                .and(SURVEY_TEMPLATE.STATUS.eq(ReleaseLifecycleStatus.ACTIVE.name()))
                .fetchSet(TO_DOMAIN_MAPPER);
    }



    public Set<SurveyInstanceInfo> findForOwner(Long personId) {
        return dsl.select(SURVEY_INSTANCE.fields())
                .select(SURVEY_RUN.fields())
                .select(SURVEY_TEMPLATE.NAME,
                        SURVEY_TEMPLATE.ID,
                        SURVEY_TEMPLATE.DESCRIPTION,
                        SURVEY_TEMPLATE.EXTERNAL_ID)
                .select(ENTITY_NAME_FIELD)
                .select(QUALIFIER_NAME_FIELD)
                .select(EXTERNAL_ID_FIELD)
                .from(SURVEY_INSTANCE)
                .innerJoin(SURVEY_INSTANCE_OWNER)
                .on(SURVEY_INSTANCE_OWNER.SURVEY_INSTANCE_ID.eq(SURVEY_INSTANCE.ID))
                .innerJoin(SURVEY_RUN).on(SURVEY_INSTANCE.SURVEY_RUN_ID.eq(SURVEY_RUN.ID))
                .innerJoin(SURVEY_TEMPLATE).on(SURVEY_RUN.SURVEY_TEMPLATE_ID.eq(SURVEY_TEMPLATE.ID))
                .where(SURVEY_INSTANCE_OWNER.PERSON_ID.eq(personId))
                .and(IS_ORIGINAL_INSTANCE_CONDITION)
                .and(SURVEY_INSTANCE.STATUS.ne(SurveyInstanceStatus.WITHDRAWN.name()))
                .and(SURVEY_TEMPLATE.STATUS.eq(ReleaseLifecycleStatus.ACTIVE.name()))
                .fetchSet(TO_DOMAIN_MAPPER);
    }
}
