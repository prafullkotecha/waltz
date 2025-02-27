<script>

    import ReportGridPicker from "./ReportGridPicker.svelte";
    import NoData from "../../../common/svelte/NoData.svelte";
    import {ownedReportIds, selectedGrid} from "./report-grid-store";
    import {reportGridKinds} from "./report-grid-utils";
    import ReportGridEditor from "./ReportGridEditor.svelte";
    import {reportGridMemberStore} from "../../../svelte-stores/report-grid-member-store";
    import {reportGridStore} from "../../../svelte-stores/report-grid-store";
    import toasts from "../../../svelte-stores/toast-store";
    import Icon from "../../../common/svelte/Icon.svelte";
    import _ from "lodash";

    export let onGridSelect = () => console.log("selecting grid");

    const Modes = {
        VIEW: "VIEW",
        EDIT: "EDIT",
        REMOVE: "REMOVE"
    };

    let activeMode = Modes.VIEW;
    let grids = [];

    $: reportGridCall = reportGridStore.findForUser(true);
    $: grids = $reportGridCall.data;

    $: gridOwnersCall = $selectedGrid?.definition?.id && reportGridMemberStore.findByGridId($selectedGrid?.definition?.id);
    $: gridOwners = $gridOwnersCall?.data || [];

    function selectGrid(grid, isNew = false) {
        $selectedGrid = null;
        activeMode = Modes.VIEW;
        onGridSelect(grid, isNew);
    }

    function create(grid) {
        const createCmd = {
            name: grid.name,
            description: grid.description,
            kind: grid.kind
        };

        let savePromise = reportGridStore.create(createCmd);
        Promise.resolve(savePromise)
            .then(r => {
                toasts.success("Grid created successfully");
                selectGrid(r.data, true);
                reportGridCall = reportGridStore.findForUser(true);
            })
            .catch(e => toasts.error("Could not create report grid. " + e.error));
    }

    function update(grid){
        const updateCmd = {
            name: grid.name,
            description: grid.description,
            kind: grid.kind
        }

        let savePromise = reportGridStore.update(grid.id, updateCmd);
        Promise.resolve(savePromise)
            .then(r => {
                toasts.success("Grid updated successfully")
                selectGrid(r.data);
                reportGridCall = reportGridStore.findForUser(true);
            })
            .catch(e => toasts.error("Could not update grid. " + e.error));
    }

    function remove(grid){

        let rmPromise = reportGridStore.remove(grid.id);
        Promise.resolve(rmPromise)
            .then(r => {
                toasts.success("Grid removed successfully")
                selectGrid(null);
                reportGridCall = reportGridStore.findForUser(true);
            })
            .catch(e => toasts.error("Could not remove grid"));
    }

    function saveReportGrid(grid) {
        if(grid.id){
            update(grid);
        } else {
            create(grid);
        }
    }

    function createGrid() {

        const workingGrid = {
            name: null,
            description: null,
            externalId: null,
            kind: reportGridKinds.PRIVATE.key,
        }

        $selectedGrid = { definition: workingGrid };
        activeMode = Modes.EDIT
    }


    function cancel(){
        activeMode = Modes.VIEW;
        $selectedGrid = $selectedGrid.definition.id ? $selectedGrid : null;
    }

    $: gridOwnerNames = _.map(gridOwners, d => d.userId);

</script>

<div class="row">
    <div class="col-sm-5">
        <ReportGridPicker grids={grids}
                          onCreate={createGrid}
                          onGridSelect={selectGrid}/>
    </div>
    <div class="col-sm-7">

        {#if activeMode === Modes.EDIT}
            <h4>Editing report grid:</h4>
            <ReportGridEditor grid={$selectedGrid?.definition}
                              doSave={saveReportGrid}
                              doCancel={cancel}/>
        {:else if activeMode === Modes.VIEW || activeMode === Modes.REMOVE}
            {#if $selectedGrid?.definition?.id}
                <h4>{$selectedGrid?.definition?.name}</h4>
                <table class="table table-condensed small">
                    <tbody>
                    <tr>
                        <td>Kind</td>
                        <td>{_.get(reportGridKinds[$selectedGrid?.definition?.kind], 'name', 'Unknown Kind')}</td>
                    </tr>
                    <tr>
                        <td>Owners</td>
                        <td>
                            {#if !_.isEmpty(gridOwners)}
                                <ul>
                                    {#each _.orderBy(gridOwners, d => d.userId) as owner}
                                    <li>
                                        {owner.userId}
                                    </li>
                                    {/each}
                                </ul>
                            {:else}
                                <span class="text-muted">None defined</span>
                            {/if}
                        </td>
                    </tr>
                    </tbody>
                </table>
                <div class:text-muted={!$selectedGrid?.definition?.description}
                     style="padding-bottom: 2em;">
                    {$selectedGrid?.definition?.description || "No description provided"}
                </div>
                {#if _.includes($ownedReportIds, $selectedGrid.definition?.id)}
                    {#if activeMode === Modes.REMOVE}
                        <h4>Please confirm you would like to delete this grid?</h4>
                        <ul>
                            <li>It cannot be restored</li>
                            <li>It will be deleted for all users</li>
                            <li>It will be deleted across all views in Waltz (Org Units, App Groups, People etc.)</li>
                        </ul>
                        <button class="btn-danger btn btn-sm"
                                on:click={() => remove($selectedGrid.definition)}>
                            Yes, delete this grid
                        </button>
                        <button class="btn-primary btn btn-sm"
                                on:click={() => activeMode = Modes.VIEW}>
                            Cancel
                        </button>
                    {/if}
                    {#if activeMode === Modes.VIEW}
                        <button class="btn btn-sm btn-primary"
                                on:click={() => activeMode = Modes.EDIT}>
                            <Icon name="pencil"/>Edit Grid
                        </button>
                        <button class="btn btn-sm btn-danger"
                                on:click={() => activeMode = Modes.REMOVE}>
                            <Icon name="trash"/>Delete Grid
                        </button>
                        <div class="help-block small">
                            <Icon name="info-circle"/>To edit the columns for the grid use the 'Column Editor' tab above.
                        </div>
                    {/if}
                {:else}
                    <div class="help-block small">
                        <Icon name="info-circle"/>You cannot edit this grid as you are not an owner.
                    </div>
                {/if}
            {:else}
                <NoData>Waiting for grid selection</NoData>
            {/if}
        {/if}
    </div>
</div>

<style>
     ul {
         padding-left: 1em
     }
</style>