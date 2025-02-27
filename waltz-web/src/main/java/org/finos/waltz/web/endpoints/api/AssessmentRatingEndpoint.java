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

package org.finos.waltz.web.endpoints.api;


import org.finos.waltz.service.assessment_definition.AssessmentDefinitionService;
import org.finos.waltz.service.assessment_rating.AssessmentRatingService;
import org.finos.waltz.service.user.UserRoleService;
import org.finos.waltz.web.NotAuthorizedException;
import org.finos.waltz.web.endpoints.Endpoint;
import org.finos.waltz.common.StringUtilities;
import org.finos.waltz.model.UserTimestamp;
import org.finos.waltz.model.assessment_definition.AssessmentDefinition;
import org.finos.waltz.model.assessment_rating.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.finos.waltz.web.WebUtilities.*;
import static org.finos.waltz.web.endpoints.EndpointUtilities.*;
import static org.finos.waltz.common.Checks.checkNotNull;


@Service
public class AssessmentRatingEndpoint implements Endpoint {

    private static final String BASE_URL = mkPath("api", "assessment-rating");

    private final AssessmentRatingService assessmentRatingService;
    private final AssessmentDefinitionService assessmentDefinitionService;
    private final UserRoleService userRoleService;


    @Autowired
    public AssessmentRatingEndpoint(AssessmentRatingService assessmentRatingService,
                                    AssessmentDefinitionService assessmentDefinitionService,
                                    UserRoleService userRoleService) {

        checkNotNull(assessmentRatingService, "assessmentRatingService cannot be null");
        checkNotNull(assessmentDefinitionService, "assessmentDefinitionService cannot be null");
        checkNotNull(userRoleService, "userRoleService cannot be null");

        this.assessmentRatingService = assessmentRatingService;
        this.assessmentDefinitionService = assessmentDefinitionService;
        this.userRoleService = userRoleService;
    }


    @Override
    public void register() {
        String findForEntityPath = mkPath(BASE_URL, "entity", ":kind", ":id");
        String findByEntityKindPath = mkPath(BASE_URL, "entity-kind", ":kind");
        String findByDefinitionPath = mkPath(BASE_URL, "definition-id", ":assessmentDefinitionId");
        String findByTargetKindForRelatedSelectorPath = mkPath(BASE_URL, "target-kind", ":targetKind", "selector");
        String modifyPath = mkPath(BASE_URL, "entity", ":kind", ":id", ":assessmentDefinitionId");
        String bulkUpdatePath = mkPath(BASE_URL, "bulk-update", ":assessmentDefinitionId");
        String bulkRemovePath = mkPath(BASE_URL, "bulk-remove", ":assessmentDefinitionId");

        getForList(findForEntityPath, this::findForEntityRoute);
        getForList(findByEntityKindPath, this::findByEntityKindRoute);
        getForList(findByDefinitionPath, this::findByDefinitionIdRoute);
        postForList(findByTargetKindForRelatedSelectorPath, this::findByTargetKindForRelatedSelectorRoute);
        postForDatum(bulkUpdatePath, this::bulkStoreRoute);
        postForDatum(bulkRemovePath, this::bulkRemoveRoute);
        postForDatum(modifyPath, this::storeRoute);
        deleteForDatum(modifyPath, this::removeRoute);
    }

    private List<AssessmentRating> findByTargetKindForRelatedSelectorRoute(Request request, Response response) throws IOException {
        return assessmentRatingService.findByTargetKindForRelatedSelector(
                getKind(request, "targetKind"),
                readIdSelectionOptionsFromBody(request));
    }


    private List<AssessmentRating> findForEntityRoute(Request request, Response response) {

        return assessmentRatingService.findForEntity(getEntityReference(request));
    }


    private List<AssessmentRating> findByEntityKindRoute(Request request, Response response) {
        return assessmentRatingService.findByEntityKind(getKind(request, "kind"));
    }

    private List<AssessmentRating> findByDefinitionIdRoute(Request request, Response response) {
        long assessmentDefinitionId = getLong(request, "assessmentDefinitionId");
        return assessmentRatingService.findByDefinitionId(assessmentDefinitionId);
    }

    private boolean storeRoute(Request request, Response z) throws IOException {
        SaveAssessmentRatingCommand command = mkCommand(request);
        verifyCanWrite(request, command.assessmentDefinitionId());
        return assessmentRatingService.store(command, getUsername(request));
    }

    private boolean bulkStoreRoute(Request request, Response z) throws IOException {
        long assessmentDefinitionId = getLong(request, "assessmentDefinitionId");
        verifyCanWrite(request, assessmentDefinitionId);
        BulkAssessmentRatingCommand[] commands = readBody(request, BulkAssessmentRatingCommand[].class);
        return assessmentRatingService.bulkStore(commands, assessmentDefinitionId, getUsername(request));
    }

    private boolean bulkRemoveRoute(Request request, Response z) throws IOException {
        long assessmentDefinitionId = getLong(request, "assessmentDefinitionId");
        verifyCanWrite(request, assessmentDefinitionId);
        BulkAssessmentRatingCommand[] commands = readBody(request, BulkAssessmentRatingCommand[].class);
        return assessmentRatingService.bulkDelete(commands, assessmentDefinitionId, getUsername(request));
    }

    private boolean removeRoute(Request request, Response z) throws IOException {
        String username = getUsername(request);
        UserTimestamp lastUpdate = UserTimestamp.mkForUser(username);
        RemoveAssessmentRatingCommand command = ImmutableRemoveAssessmentRatingCommand.builder()
                .entityReference(getEntityReference(request))
                .assessmentDefinitionId(getLong(request, "assessmentDefinitionId"))
                .lastUpdatedAt(lastUpdate.at())
                .lastUpdatedBy(lastUpdate.by())
                .build();

        verifyCanWrite(request, command.assessmentDefinitionId());
        return assessmentRatingService.remove(command, getUsername(request));
    }


    private SaveAssessmentRatingCommand mkCommand(Request request) throws IOException {
        String username = getUsername(request);

        Map<String, Object> body = readBody(request, Map.class);
        UserTimestamp lastUpdate = UserTimestamp.mkForUser(username);

        return ImmutableSaveAssessmentRatingCommand.builder()
                .entityReference(getEntityReference(request))
                .assessmentDefinitionId(getLong(request, "assessmentDefinitionId"))
                .ratingId(Long.valueOf(body.getOrDefault("ratingId", "").toString()))
                .comment(StringUtilities.mkSafe((String) body.get("comment")))
                .lastUpdatedAt(lastUpdate.at())
                .lastUpdatedBy(lastUpdate.by())
                .provenance("waltz")
                .build();
    }


    private void verifyCanWrite(Request request, long defId) {
        AssessmentDefinition def = assessmentDefinitionService.getById(defId);
        def.permittedRole().ifPresent(r -> requireRole(userRoleService, request, r));
        if (def.isReadOnly()) {
            throw new NotAuthorizedException("Assessment is read-only");
        }
    }

}

