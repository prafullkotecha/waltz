/*
 *  Waltz
 * Copyright (c) David Watkins. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 *
 */

import _ from "lodash";
import {
    loadAuthSources,
    loadChangeLog,
    loadDatabases,
    loadDataFlows,
    loadDataFlowDecorators,
    loadDataTypeUsages,
    loadInvolvements,
    loadServers,
    loadSoftwareCatalog,
    loadSourceDataRatings
} from "./data-load";
import {mkAppRatingsGroup, calculateHighestRatingCount} from "../ratings/directives/common";


const initialState = {
    aliases: [],
    appAuthSources: [],
    appCapabilities: [],
    capabilities: [],
    complexity: [],
    databases: [],
    dataArticles: [],
    dataTypes: [],
    dataTypeUsages: [],
    explicitTraits: [],
    flows: [],
    log: [],
    ouAuthSources: [],
    organisationalUnit: null,
    peopleInvolvements: [],
    processes: [],
    ratings: null,
    servers: [],
    softwareCatalog: [],
    sourceDataRatings: [],
    tags: [],
    visibility: {}
};


function controller($q,
                    $state,
                    app,
                    appViewStore,
                    aliasStore,
                    authSourcesStore,
                    changeLogStore,
                    complexityStore,
                    databaseStore,
                    dataFlowStore,
                    dataFlowDecoratorStore,
                    dataTypeUsageStore,
                    involvementStore,
                    orgUnitStore,
                    perspectiveStore,
                    physicalDataArticleStore,
                    physicalDataFlowStore,
                    processStore,
                    ratingStore,
                    serverInfoStore,
                    softwareCatalogStore,
                    sourceDataRatingStore) {

    const { id, organisationalUnitId } = app;
    const entityRef = { id, kind: 'APPLICATION' };
    const vm = Object.assign(this, initialState, { app });

    const goToAppFn = d => $state.go('main.app.view', { id: d.id });
    vm.flowTweakers = {
        source: {
            onSelect: goToAppFn
        },
        target: {
            onSelect: goToAppFn
        }
    };

    vm.entityRef = entityRef;

    const perspectiveCode = 'BUSINESS';

    $q.all([
        appViewStore.getById(id),
        perspectiveStore.findByCode(perspectiveCode),
        ratingStore.findByParent('APPLICATION', id)
    ]).then(([appView, perspective, ratings]) => {
        Object.assign(vm, appView);
        const appRef = { id: id, kind: 'APPLICATION', name: app.name};
        const group = mkAppRatingsGroup(appRef, perspective.measurables, appView.capabilities, ratings);

        vm.ratings = {
            highestRatingCount: calculateHighestRatingCount([group]),
            tweakers: {
                subjectLabel: {
                    enter: (selection) => selection.on(
                        'click',
                        (d) => $state.go('main.capability.view', { id: d.subject.id }))
                }
            },
            group
        };
    });


    const promises = [
        loadDataFlows(dataFlowStore, id, vm),
        loadInvolvements($q, involvementStore, id, vm),
        loadAuthSources(authSourcesStore, orgUnitStore, id, organisationalUnitId, vm),
        loadServers(serverInfoStore, id, vm),
        loadSoftwareCatalog(softwareCatalogStore, id, vm),
        loadDatabases(databaseStore, id, vm),
        loadDataTypeUsages(dataTypeUsageStore, id, vm),
        loadDataFlowDecorators(dataFlowDecoratorStore, id, vm)
    ];

    $q.all(promises)
        .then(() => loadChangeLog(changeLogStore, id, vm))
        .then(() => loadSourceDataRatings(sourceDataRatingStore, vm));

    complexityStore
        .findByApplication(id)
        .then(c => vm.complexity = c);

    processStore
        .findForApplication(id)
        .then(ps => vm.processes = ps);

    vm.saveAliases = (aliases) => {
        const aliasValues = _.map(aliases, 'text');
        return aliasStore
            .update(entityRef, aliasValues)
            .then(() => vm.aliases = aliasValues);
    };

    physicalDataArticleStore
        .findByAppId(id)
        .then(xs => vm.physicalDataArticles = xs);

    physicalDataFlowStore
        .findByEntityReference(entityRef)
        .then(xs => vm.physicalDataFlows = xs);
}


controller.$inject = [
    '$q',
    '$state',
    'app',
    'ApplicationViewStore',
    'AliasStore',
    'AuthSourcesStore',
    'ChangeLogDataService',
    'ComplexityStore',
    'DatabaseStore',
    'DataFlowDataStore',
    'DataFlowDecoratorStore',
    'DataTypeUsageStore',
    'InvolvementStore',
    'OrgUnitStore',
    'PerspectiveStore',
    'PhysicalDataArticleStore',
    'PhysicalDataFlowStore',
    'ProcessStore',
    'RatingStore',
    'ServerInfoStore',
    'SoftwareCatalogStore',
    'SourceDataRatingStore'
];


export default  {
    template: require('./app-view.html'),
    controller,
    controllerAs: 'ctrl'
};

