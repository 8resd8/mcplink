<script lang="ts">
  import { onMount } from "svelte"
  import Search from "../../lib/components/search.svelte"
  import MCPCard from "../../lib/components/mcp-card.svelte"
  import { fetchMCPCards } from "../../lib/data/mcp-api"
  import { page } from "$app/stores"
  import { listen } from "@tauri-apps/api/event"
  import { WebviewWindow } from "@tauri-apps/api/webviewWindow"

  // define data type to receive from backend
  import type { MCPCard as MCPCardType, PageInfo } from "../../lib/data/mcp-api"

  // MCP card data
  let mcpCards: MCPCardType[] = []
  let pageInfo: PageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }

  // data loading state
  let loading = true
  let loadingMore = false
  let allLoaded = false // Flag to check if all data has been loaded

  // START: Added variables
  let searchTermFromQuery = "" // Variable to store the search term from the URL
  let isRecommendedSearch = false // State variable for recommended search
  // END: Added variables

  // Scroll event handler
  function handleScroll() {
    if (loadingMore || !pageInfo.has_next_page || allLoaded) return

    // Check scroll position of the entire document
    const scrollPosition = window.innerHeight + window.scrollY
    const scrollHeight = document.body.offsetHeight

    // Load next page if scroll is 300px from the bottom (detect faster)
    if (scrollHeight - scrollPosition < 300) {
      loadNextPage()
    }
  }

  // Function to load the next page
  async function loadNextPage() {
    if (!pageInfo.has_next_page || !pageInfo.end_cursor || loadingMore || allLoaded) return

    loadingMore = true
    try {
      const response = await fetchMCPCards(searchTermFromQuery || undefined, pageInfo.end_cursor)

      // Add new cards to existing cards
      mcpCards = [...mcpCards, ...response.cards]
      pageInfo = response.page_info

      // Check if all data has been loaded
      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        allLoaded = true
      }
    } catch (error) {
      console.error("Error loading next page:", error)
    } finally {
      loadingMore = false
    }
  }

  // Page initialization and data loading function
  function initPage() {
    // Add global scroll event listener (with debounce)
    let scrollTimer: ReturnType<typeof setTimeout> | null = null
    const scrollHandler = () => {
      if (scrollTimer) clearTimeout(scrollTimer)
      scrollTimer = setTimeout(handleScroll, 100)
    }

    window.addEventListener("scroll", scrollHandler)

    // Check page bottom after initial load
    setTimeout(() => {
      handleScroll()
    }, 500)

    return scrollHandler
  }

  // get data when component is mounted
  onMount(() => {
    // START: Add main window event listener (navigation and centering)
    let unlistenNavigate: (() => void) | undefined
    listen("navigate-to-mcp-list-with-keyword", async (event) => {
      const newUrl = event.payload as string
      if (newUrl && typeof newUrl === "string") {
        const url = new URL(newUrl, window.location.origin) // Create full URL
        const keyword = url.searchParams.get("keyword")
        if (keyword) {
          searchTermFromQuery = keyword
          isRecommendedSearch = true
          await searchAndDisplay(keyword)
        } else {
          searchTermFromQuery = "" // Initialize if keyword is missing
          isRecommendedSearch = false
        }
      }
    }).then((fn) => (unlistenNavigate = fn)) // Save unlisten function

    // START: Read keyword from URL query parameter
    const unsubscribePage = page.subscribe((p) => {
      const keyword = p.url.searchParams.get("keyword")
      if (keyword) {
        if (searchTermFromQuery !== keyword) {
          // Execute later than event or for direct URL access
          searchTermFromQuery = keyword
          isRecommendedSearch = true
          searchAndDisplay(searchTermFromQuery)
        }
      } else {
        if (searchTermFromQuery !== "") {
          // Initialize if there was a previous search term
          searchTermFromQuery = ""
        }
        isRecommendedSearch = false
        fetchAllMCPs()
      }
    })
    // END: Read keyword from URL query parameter

    // Initialize scroll event
    const scrollHandler = initPage()

    // Unregister listener and unsubscribe on component destroy
    return () => {
      if (unlistenNavigate) unlistenNavigate() // Call saved unlisten function
      unsubscribePage()
      window.removeEventListener("scroll", scrollHandler)
    }
  })

  // Detect page change and reinitialize
  $: {
    if ($page) {
      if ($page.url.pathname === "/MCP-list") {
        setTimeout(() => {
          handleScroll()
        }, 500)
      }
    }
  }

  // START: Search execution and result display function
  async function searchAndDisplay(term: string) {
    if (!term) {
      isRecommendedSearch = false // Clear recommendation state if search term is empty
      return fetchAllMCPs() // Load all if term is empty
    }
    // isRecommendedSearch can only be true if term exists, so don't change it here
    loading = true
    allLoaded = false // Reset allLoaded when loading new data
    try {
      const response = await fetchMCPCards(term)
      mcpCards = response.cards
      pageInfo = response.page_info

      // Check if all data has been loaded
      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        allLoaded = true
      } else {
        // Check scroll after initial load
        setTimeout(handleScroll, 500)
      }
    } catch (error) {
      console.error("Error during search:", error)
      mcpCards = []
      pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }
      allLoaded = true
    } finally {
      loading = false
      // Scroll to the top of the page
      window.scrollTo(0, 0)
    }
  }
  // END: Search execution and result display function

  // START: Function to fetch all MCP cards
  async function fetchAllMCPs() {
    isRecommendedSearch = false // Clear recommendation state when fetching all list
    allLoaded = false // Reset allLoaded when loading new data
    try {
      const response = await fetchMCPCards()
      mcpCards = response.cards
      pageInfo = response.page_info

      // Check if all data has been loaded
      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        allLoaded = true
      } else {
        // Check scroll after initial load
        setTimeout(handleScroll, 500)
      }
    } catch (error) {
      console.error("Error fetching MCP data:", error)
      mcpCards = []
      pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }
      allLoaded = true
    } finally {
      loading = false
      // Scroll to the top of the page
      window.scrollTo(0, 0)
    }
  }
  // END: Function to fetch all MCP cards

  // Search event handler
  async function handleSearchEvent(event: CustomEvent<{ value: string }>) {
    const searchTerm = event.detail.value
    // User directly entered/changed search term, so clear recommendation state
    if (isRecommendedSearch && searchTerm !== searchTermFromQuery) {
      isRecommendedSearch = false
    }
    isRecommendedSearch = false

    // Do not update URL, so searchTermFromQuery remains unchanged.
    // searchAndDisplay searches with the current value in the search bar (searchTerm).
    await searchAndDisplay(searchTerm)
  }
</script>

<div class="p-8">
  <!-- Fixed header area - Set background color same as tab color -->
  <div class="sticky top-0 z-10 pb-4">
    <div class="flex justify-between items-center">
      <p class="text-xl font-semibold">Search results ({pageInfo.total_items} MCPs)</p>
      <div class="search-area-wrapper flex items-center ml-auto">
        {#if isRecommendedSearch}
          <span class="mr-2 text-yellow-500" title="Recommended Search">✨</span>
        {/if}
        <div class="search-component-wrapper w-72 rounded-[10px]">
          {#key searchTermFromQuery}
            <Search on:search={handleSearchEvent} initialValue={searchTermFromQuery} />
          {/key}
        </div>
      </div>
    </div>
  </div>

  <!-- Content area -->
  <div class="mt-2">
    {#if loading}
      <div class="flex justify-center items-center h-64">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>
    {:else if mcpCards.length === 0}
      <div class="flex justify-center items-center h-64">
        <p class="text-gray-500">No MCPs found.</p>
      </div>
    {:else}
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {#each mcpCards as card (card.id)}
          <MCPCard {...card} />
        {/each}
      </div>

      <!-- Loading spinner (loading more data) -->
      {#if loadingMore}
        <div class="flex justify-center items-center py-4">
          <span class="loading loading-spinner loading-md text-primary"></span>
        </div>
      {/if}

      <!-- When no more data -->
      {#if allLoaded && mcpCards.length > 0}
        <div class="flex justify-center items-center py-4 text-gray-500">All results loaded.</div>
      {/if}
    {/if}
  </div>
</div>

<style>
  /* Match page background color with tab color (#ecfdf5) */
  :global(body) {
    background-color: #ecfdf5;
  }

  /* Remove blue line */
  :global(.sticky) {
    border-bottom: none !important;
  }

  /* Customize scrollbar style */
  :global(::-webkit-scrollbar) {
    width: 8px;
  }

  :global(::-webkit-scrollbar-track) {
    background: transparent;
  }

  :global(::-webkit-scrollbar-thumb) {
    background: #888;
    border-radius: 4px;
  }

  :global(::-webkit-scrollbar-thumb:hover) {
    background: #555;
  }
</style>
