<script>

 import Icon from "../../../../common/svelte/Icon.svelte";
 import _ from "lodash";

 export let items = [];
 export let defaultMessage = "Select an option";
 export let onSelect = () => console.log("Selecting");
 export let selectedItem = null;

 let showDropdown = false;

 function toggleDropdown() {
     showDropdown = !showDropdown
 }

 function selectItem(item) {
     selectedItem = item;
     showDropdown = false;
     onSelect(item)
 }

</script>


<div class="btn-group"
     style="width: 100%; outline: 1px solid #cccccc;">
    <div class:expanded={showDropdown}>
        <button on:click={() => toggleDropdown()}
                class="btn-skinny">
            {#if _.isNull(selectedItem)}
                <span class="force-wrap">
                    {defaultMessage}
                </span>
            {:else}
                <span class="force-wrap">
                    {selectedItem?.name}
                </span>
            {/if}
            <span class="pull-right force-wrap">
                {#if showDropdown}
                    <Icon name="caret-up"/>
                {:else}
                    <Icon name="caret-down"/>
                {/if}
            </span>
        </button>
    </div>
    <div>
        {#if showDropdown}
            <ul>
                {#if selectedItem}
                    <li>
                        <div class="text-muted clickable force-wrap"
                             on:click={() => selectItem(null)}>
                            {defaultMessage}
                        </div>
                    </li>
                {/if}
                {#each items as item}
                    <li>
                        <button class="btn-skinny"
                                on:click={() => selectItem(item)}>
                            <span class="force-wrap">{item?.name}</span>
                        </button>
                    </li>
                {/each}
            </ul>
        {/if}
    </div>
</div>

<style type="text/scss">
    ul {
        padding: 0;
        margin: 0;
        outline: 1px solid #cccccc;
        list-style: none;
    }

    li {
        padding-top: 0;

        &:hover {
             background: #f3f9ff;
         }

        div {
            width: 100%;
            padding: 0.5em;
            text-align: left;
        }
    }

    button {
        width: 100%;
        padding: 0.5em;
        text-align: left;
    }

    .expanded {
        background: #eee;
    }
</style>