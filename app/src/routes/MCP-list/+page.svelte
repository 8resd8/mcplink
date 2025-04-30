<script lang="ts">
  import { onMount } from 'svelte';
  import Search from '../../lib/components/search.svelte';
  import MCPCard from '../../lib/components/mcp-card.svelte';
  import { fetchMCPCards } from '../../lib/data/mcp-api';
  
  // define data type to receive from backend
  import type { MCPCard as MCPCardType } from '../../lib/data/mcp-api';
  
  // MCP card data
  let mcpCards: MCPCardType[] = [];
  
  // data loading state
  let loading = true;
  
  // get data when component is mounted
  onMount(async () => {
    try {
      // get MCP card data from Tauri backend
      loading = true;
      mcpCards = await fetchMCPCards();
      loading = false;
    } catch (error) {
      console.error('MCP 데이터를 가져오는 중 오류 발생:', error);
      loading = false;
    }
  });
  
  // Search event handler
  function handleSearchEvent(event: CustomEvent<{ value: string }>) {
    // Implement server-side search in the future
    // When the server is implemented, call the server API here to get the search results
    console.log('검색어:', event.detail.value);
    // Server API call example:
    // searchMCPs(event.detail.value).then(results => {
    //   mcpCards = results;
    // });
  }
  
</script>

<div class="p-8">
  <div class="flex flex-col gap-6">
    <!-- search area -->
      
      <div class="w-full max-w-xl rounded-[10px]">
        <Search on:search={handleSearchEvent} />
      </div>

    
    <!-- card list area -->

      <p class="text-xl font-semibold mb-2">Search results ({mcpCards.length} MCPs)</p>
      
      {#if loading}
        <div class="flex justify-center items-center h-64">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>
      {:else if mcpCards.length === 0}
        <div class="flex justify-center items-center h-64">
          <p class="text-gray-500">No search results found.</p>
        </div>
      {:else}
        <div class="grid grid-cols-1 lg:grid-cols-1 gap-2">
          {#each mcpCards as card (card.id)}
            <MCPCard 
              id={card.id}
              title={card.title}
              description={card.description}
            />
          {/each}
        </div>
      {/if}
  </div>
</div> 