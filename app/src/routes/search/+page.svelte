<script lang="ts">
  import { onMount } from "svelte"
  import Search from "../../lib/components/search.svelte"
  import MCPCard from "../../lib/components/mcp-card.svelte"
  import { fetchMCPCards } from "../../lib/data/mcp-api"

  // Type definition
  import type { MCPCard as MCPCardType } from "../../lib/data/mcp-api"

  // MCP card data
  let mcpCards: MCPCardType[] = []
  let filteredCards: MCPCardType[] = []

  // Search term
  let searchQuery = ""

  // Category filter
  let categoryFilter = "all" // 'all', 'project', 'design', 'code', 'test'

  // Data loading state
  let loading = true

  // Fetch data when component mounts
  onMount(async () => {
    try {
      // Initial data load (without search term)
      await searchFromServer("")
    } catch (error) {
      loading = false
    }
  })

  // Server search request function
  async function searchFromServer(query: string) {
    try {
      loading = true

      // Request search from server (pass empty string as is)
      mcpCards = (await fetchMCPCards(query)).cards

      // Apply additional filtering (category, etc.)
      applyFilters()
      loading = false
    } catch (error) {
      loading = false
    }
  }

  // Apply filters function
  function applyFilters() {
    // Local filtering (category, etc.)
    let results = [...mcpCards]

    filteredCards = results
  }

  // Search event handler
  async function handleSearchEvent(event: CustomEvent<{ value: string }>) {
    searchQuery = event.detail.value
    await searchFromServer(searchQuery)
  }
</script>

<div class="p-8">
  <div class="flex flex-col gap-6">
    <!-- Search area -->

    <h1 class="text-2xl font-bold mb-4">MCP Search</h1>

    <div class="w-full max-w-xl rounded-[10px]">
      <Search on:search={handleSearchEvent} />
    </div>

    <!-- Card list area -->

    <p class="text-xl font-semibold mb-2">Search Results ({filteredCards.length} items)</p>

    {#if loading}
      <div class="flex justify-center items-center h-64">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>
    {:else if filteredCards.length === 0}
      <div class="flex justify-center items-center h-64">
        <p class="text-gray-500">No search results found.</p>
      </div>
    {:else}
      <div class="grid grid-cols-1 lg:grid-cols-1 gap-2">
        {#each filteredCards as card (card.id)}
          <MCPCard id={card.id} title={card.title} description={card.description} />
        {/each}
      </div>
    {/if}
  </div>
</div>
