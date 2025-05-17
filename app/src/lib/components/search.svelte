<script lang="ts">
  import { createEventDispatcher, onMount } from "svelte"

  // Create event dispatcher
  const dispatch = createEventDispatcher<{
    search: { value: string }
  }>()

  // START: Add initialValue prop
  export let initialValue: string = ""
  export let placeholder: string = "MCP name or keyword search..."
  export let customClass: string = "input input-bordered rounded-[10px]"
  // END: Add initialValue prop

  // Search value
  let searchValue = ""

  // Debounce timer
  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  // Debounce delay (milliseconds)
  const DEBOUNCE_DELAY = 300

  // START: Set searchValue with initialValue in onMount
  onMount(() => {
    if (initialValue) {
      searchValue = initialValue
    }
  })
  // END: Set searchValue with initialValue in onMount

  // Debounced search function
  function debouncedSearch() {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      dispatch("search", { value: searchValue })
    }, DEBOUNCE_DELAY)
  }

  // Search event handler
  function handleSearch() {
    dispatch("search", { value: searchValue })
  }

  // Keyboard event handler (Enter key pressed to search)
  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === "Enter") {
      if (debounceTimer) clearTimeout(debounceTimer)
      handleSearch()
    }
  }
</script>

<label class="{customClass} flex items-center">
  <input type="search" class="grow" placeholder="{placeholder}" bind:value={searchValue} on:input={debouncedSearch} on:keydown={handleKeyDown} />
</label>

<style>
  label {
    width: 100%; /* Control width from parent element if necessary */
  }
</style>
