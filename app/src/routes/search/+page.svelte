<script lang="ts">
  import { createEventDispatcher, onMount } from 'svelte';
  import { invoke } from '@tauri-apps/api/core';
  import { fetchMCPCards } from '../../lib/data/mcp-api';
  import MCPCard from '../../lib/components/mcp-card.svelte';

  interface MCPCard {
    id: number;
    title: string;
    description: string;
    url: string;
    stars: number;
  }

  const dispatch = createEventDispatcher<{
    searchResults: { results: MCPCard[] }
  }>();

  let searchValue = '';
  let searchResults: MCPCard[] = [];
  let debounceTimeout: number | null = null;
  const DEBOUNCE_DELAY = 300;
  let isLoading = false;
  let error = '';

  async function invokeSearch() {
    if (debounceTimeout) {
      clearTimeout(debounceTimeout);
    }
    isLoading = true;
    error = '';

    try {
      const results = await fetchMCPCards(searchValue.trim() === '' ? undefined : searchValue.trim());
      searchResults = results;
      dispatch('searchResults', { results });
      console.log('Search results:', results);
    } catch (e) {
      console.error('Error invoking search:', e);
      error = `Search failed: ${e}`;
      searchResults = [];
      dispatch('searchResults', { results: [] });
    } finally {
      isLoading = false;
    }
  }

  function handleInput() {
    if (debounceTimeout) {
      clearTimeout(debounceTimeout);
    }
    debounceTimeout = window.setTimeout(() => {
      invokeSearch();
    }, DEBOUNCE_DELAY);
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
       if (debounceTimeout) {
         clearTimeout(debounceTimeout);
       }
       invokeSearch();
    }
  }

  onMount(() => {
    invokeSearch();
  });

</script>

<div class="flex flex-col space-y-2">
  <label class="input input-bordered rounded-[10px] flex items-center gap-2">
    <input
      type="search"
      class="grow"
      placeholder="MCP name or keyword search..."
      bind:value={searchValue}
      on:input={handleInput}
      on:keydown={handleKeyDown}
    />
    <kbd class="kbd kbd-sm">⌘</kbd>
    <kbd class="kbd kbd-sm">K</kbd>
  </label>

  {#if isLoading}
    <p>Searching...</p>
  {/if}

  {#if error}
    <p class="text-red-500">{error}</p>
  {/if}

  {#if searchResults.length > 0}
    <div class="mt-4 grid gap-4">
      {#each searchResults as result (result.id)}
        <MCPCard
          id={result.id}
          title={result.title}
          description={result.description}
          url={result.url}
          stars={result.stars}
          variant="default"
        />
      {/each}
    </div>
  {:else if !isLoading && !error}
    <p class="mt-4 text-center text-gray-500">검색 결과가 없습니다.</p>
  {/if}
</div>