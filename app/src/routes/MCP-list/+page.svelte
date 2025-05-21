<script lang="ts">
  import { onMount, onDestroy } from "svelte"
  import Search from "../../lib/components/search.svelte"
  import MCPCard from "../../lib/components/mcp-card.svelte"
  import { fetchMCPCards } from "../../lib/data/mcp-api"
  import { page } from "$app/stores"
  import { browser } from "$app/environment"
  import { listen } from "@tauri-apps/api/event"
  import { WebviewWindow } from "@tauri-apps/api/webviewWindow"
  import NotificationHandler from "./NotificationHandler.svelte"
  import { sharedDataStore, updateCount } from "$lib/stores/data-store"

  // define data type to receive from backend
  import type { MCPCard as MCPCardType, PageInfo } from "../../lib/data/mcp-api"

  // MCP card data
  let mcpCards: MCPCardType[] = []
  let pageInfo: PageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }
  let mainElement: HTMLElement | null = null // 실제 스크롤링 발생하는 메인 요소

  // data loading state
  let loading = true
  let loadingMore = false
  let allLoaded = false // Flag to check if all data has been loaded
  let justLoadedNewData = false

  // 중복 호출 방지를 위한 상태
  let isSearching = false // 현재 검색 중인지 상태
  let lastSearchTerm = "" // 마지막 검색어 기록

  // START: Added variables
  let searchTermFromQuery = "" // Variable to store the search term from the URL
  let isRecommendedSearch = false // State variable for recommended search
  let debounceTimer: ReturnType<typeof setTimeout> | null = null // Debounce timer
  // END: Added variables

  // Scroll event handler
  function handleScroll() {
    if (!mainElement) {
      mainElement = document.querySelector("main.flex-1.overflow-y-auto")
      if (!mainElement) return
    }

    if (loadingMore || !pageInfo.has_next_page || allLoaded) {
      return
    }

    if (justLoadedNewData) {
      justLoadedNewData = false // 플래그 리셋
      return // 현재 호출은 여기서 종료, 다음 실제 스크롤 이벤트를 기다림
    }

    const scrollPosition = mainElement.scrollTop + mainElement.clientHeight
    const scrollHeight = mainElement.scrollHeight

    if (scrollHeight - scrollPosition < 300) {
      loadNextPage()
    }
  }

  // Function to load the next page
  async function loadNextPage() {
    if (!pageInfo.has_next_page || !pageInfo.end_cursor || loadingMore || allLoaded) {
      return
    }

    loadingMore = true
    try {
      await new Promise((resolve) => setTimeout(resolve, 100))

      const response = await fetchMCPCards(searchTermFromQuery || undefined, pageInfo.end_cursor)

      if (response.cards && response.cards.length > 0) {
        mcpCards = [...mcpCards, ...response.cards]
        pageInfo = response.page_info
        justLoadedNewData = true
      } else {
        pageInfo.has_next_page = false
      }

      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        allLoaded = true
      } else {
        allLoaded = false
      }
    } catch (error) {
      console.error("[MCP-list] Error loading next page:", error)
    } finally {
      loadingMore = false
    }
  }

  // get data when component is mounted
  onMount(() => {
    // 실제 스크롤이 발생하는 메인 요소 찾기
    mainElement = document.querySelector("main.flex-1.overflow-y-auto")

    if (mainElement) {
      // 메인 요소에 스크롤 이벤트 핸들러 추가
      mainElement.addEventListener("scroll", handleScroll)
    } else {
      console.warn("[MCP-list] Main scrollable element not found")
    }

    // 초기 데이터 로드
    searchAndDisplay("", true)

    // 세션 스토리지에서 알림 키워드 확인 (일회성 처리)
    const pendingKeyword = sessionStorage.getItem("pendingSearchKeyword")
    if (pendingKeyword) {
      searchTermFromQuery = pendingKeyword
      isRecommendedSearch = true

      // 페이지 로드 시 자동 검색 실행
      setTimeout(() => {
        searchAndDisplay(pendingKeyword)
      }, 300)

      // 사용한 키워드는 세션 스토리지에서 제거
      sessionStorage.removeItem("pendingSearchKeyword")

      // 마지막 알림 키워드도 제거 (페이지 새로고침에서 재사용 방지)
      localStorage.removeItem("lastNotificationKeyword")
    } else {
      // URL 파라미터에서 키워드를 확인
      const urlParams = new URLSearchParams(window.location.search)
      const urlKeyword = urlParams.get("keyword")

      // URL에 키워드가 있으면 마지막 키워드 검사는 건너뜀
      if (urlKeyword) {
        // 마지막 알림 키워드가 URL에 있는 키워드와 같으면 제거
        if (localStorage.getItem("lastNotificationKeyword") === urlKeyword) {
          localStorage.removeItem("lastNotificationKeyword")
        }
        return // URL에 키워드가 있으면 나머지 로직은 실행 안 함
      }

      // 로컬 스토리지에서 마지막 알림 키워드 확인 (백업 처리)
      const lastKeyword = localStorage.getItem("lastNotificationKeyword")
      if (lastKeyword && !pendingKeyword && !searchTermFromQuery) {
        searchTermFromQuery = lastKeyword
        isRecommendedSearch = true

        // 페이지 로드 시 자동 검색 실행
        setTimeout(() => {
          searchAndDisplay(lastKeyword)
          // 사용 후 키워드 제거 (재사용 방지)
          localStorage.removeItem("lastNotificationKeyword")
        }, 300)
      }
    }

    // 1. 추가: search-keyword 이벤트 리스닝
    const unlistenSearchKeywordTauri = listen("search-keyword", (event: any) => {
      try {
        const keyword = typeof event.payload === "string" ? event.payload : typeof event.payload === "object" && event.payload && event.payload.keyword ? event.payload.keyword : null

        if (keyword) {
          const searchEvent = new CustomEvent("set-search-term", { detail: keyword })
          document.dispatchEvent(searchEvent)

          searchTermFromQuery = keyword
          isRecommendedSearch = true
          searchAndDisplay(keyword, true)
        } else {
          console.warn("[MCP-list] search-keyword event: Keyword not found in payload.", event.payload)
        }
      } catch (e) {
        console.error("[MCP-list] Error processing search-keyword event:", e)
      }
    })

    // 2. 추가: activation-complete 이벤트 리스닝
    const unlistenActivationCompleteTauri = listen("activation-complete", () => {
      const urlParams = new URLSearchParams(window.location.search)
      const urlKeyword = urlParams.get("keyword")

      if (urlKeyword) {
        if (localStorage.getItem("lastNotificationKeyword") === urlKeyword) {
          localStorage.removeItem("lastNotificationKeyword")
        }
        return
      }
      // 알림으로 앱이 활성화된 경우의 키워드 처리는 다른 곳에서 이미 처리됨 (예: NotificationHandler 또는 초기 onMount 로직의 pendingKeyword)
    })

    // $page 스토어 구독으로 URL 키워드 처리
    const unsubscribePage = page.subscribe((currentPage) => {
      const urlKeyword = currentPage.url.searchParams.get("keyword")
      if (urlKeyword && urlKeyword !== searchTermFromQuery) {
        searchTermFromQuery = urlKeyword
        isRecommendedSearch = !!currentPage.url.searchParams.get("recommended")
        searchAndDisplay(urlKeyword, true)

        if (localStorage.getItem("lastNotificationKeyword") === urlKeyword) {
          localStorage.removeItem("lastNotificationKeyword")
        }
      }
    })

    return () => {
      if (mainElement) {
        mainElement.removeEventListener("scroll", handleScroll)
      }
      if (unsubscribePage) unsubscribePage()

      // Tauri 이벤트 리스너 해제
      unlistenSearchKeywordTauri.then((fn) => fn())
      unlistenActivationCompleteTauri.then((fn) => fn())

      if (debounceTimer) clearTimeout(debounceTimer)
    }
  })

  // START: Search execution and result display function
  async function searchAndDisplay(term: string, scrollToTop: boolean = false) {
    // 동일한 검색어로 연속 호출 방지 (검색어가 비어있는 경우는 제외 - 초기 로드 시 필요)
    if (isSearching && term && term === lastSearchTerm) {
      return // 이미 동일 검색어로 검색 중이면 중복 실행 방지
    }

    // 검색 상태 설정 및 마지막 검색어 기록
    isSearching = true
    lastSearchTerm = term

    if (!term) {
      isRecommendedSearch = false // Clear recommendation state if search term is empty
      // 빈 검색어로 fetchMCPCards 호출 - 전체 목록 가져오기
      loading = true
      allLoaded = false
      justLoadedNewData = false
      mcpCards = []
      pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }

      if (scrollToTop && mainElement) {
        mainElement.scrollTo(0, 0)
      }

      try {
        const response = await fetchMCPCards()
        mcpCards = response.cards
        pageInfo = response.page_info
        justLoadedNewData = true

        // 글로벌 데이터 스토어에 MCP 리스트 개수 업데이트
        updateCount("listCount", pageInfo.total_items)

        if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
          allLoaded = true
        } else {
          allLoaded = false
        }
      } catch (error) {
        console.error("Error loading all MCPs:", error)
        mcpCards = []
        pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }
        allLoaded = true
      } finally {
        loading = false
        isSearching = false // 검색 상태 종료
      }
      return
    }

    // isRecommendedSearch can only be true if term exists, so don't change it here
    loading = true
    allLoaded = false // 새로운 검색 시작 시 초기화
    justLoadedNewData = false // 새로운 검색 시작 시 초기화
    mcpCards = [] // 기존 카드 목록 초기화
    pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 } // 페이지 정보 초기화

    if (scrollToTop && mainElement) {
      mainElement.scrollTo(0, 0)
    }

    try {
      // 알림이 처리되었음을 사용자에게 표시 (타이밍 잠깐 지연)
      setTimeout(() => {
        if (isRecommendedSearch) {
          try {
            // 토스트 메시지 표시
            const toastEvent = new CustomEvent("show-toast", {
              detail: {
                message: `'${term}' 키워드로 검색중..`,
                type: "info",
                duration: 2000,
              },
            })
            document.dispatchEvent(toastEvent)
          } catch (e) {
            console.error("Toast event error:", e)
          }
        }
      }, 50)

      const response = await fetchMCPCards(term)
      mcpCards = response.cards
      pageInfo = response.page_info
      justLoadedNewData = true // 새 데이터 로드됨

      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        allLoaded = true
      } else {
        allLoaded = false
      }

      // 검색 결과 표시
      if (isRecommendedSearch) {
        try {
          // 결과 토스트 메시지
          const resultToast = new CustomEvent("show-toast", {
            detail: {
              message: `'${term}' 키워드 검색 결과: ${mcpCards.length}개 발견`,
              type: mcpCards.length > 0 ? "success" : "warning",
              duration: 2000,
            },
          })
          setTimeout(() => document.dispatchEvent(resultToast), 500)
        } catch (e) {
          console.error("Result toast error:", e)
        }
      }
    } catch (error) {
      console.error("Error during search:", error)
      mcpCards = []
      pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }
      allLoaded = true

      // 검색 오류 표시
      if (isRecommendedSearch) {
        try {
          const errorToast = new CustomEvent("show-toast", {
            detail: {
              message: `'${term}' 키워드 검색 오류 발생`,
              type: "error",
              duration: 3000,
            },
          })
          document.dispatchEvent(errorToast)
        } catch (e) {
          console.error("Error toast error:", e)
        }
      }
    } finally {
      loading = false
      isSearching = false // 검색 상태 종료
    }
  }

  // Search event handler
  const handleSearch = (event: CustomEvent<{ value: string }>) => {
    const term = event.detail.value
    searchTermFromQuery = term
    isRecommendedSearch = false // 사용자가 직접 검색하면 추천 검색 상태 해제

    // 이미 동일한 검색어로 검색 중이면 중복 실행 방지
    if (isSearching && term === lastSearchTerm) {
      return
    }

    // 기존 디바운스 타이머가 있다면 취소
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    // 새로운 디바운스 타이머 설정
    debounceTimer = setTimeout(() => {
      // 디바운스 시간이 지난 후에도 중복 검색이 아닌지 확인
      if (!isSearching || term !== lastSearchTerm) {
        searchAndDisplay(term, true) // 검색 시 스크롤 상단으로
      }
    }, 150) // 150ms 디바운스
  }

  const handleClearSearch = () => {
    searchTermFromQuery = ""
    isRecommendedSearch = false

    // 검색 중이면 중복 실행 방지
    if (isSearching && lastSearchTerm === "") {
      return
    }

    // 디바운스 타이머가 있다면 취소
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    // 중복 검색 방지
    if (!isSearching || lastSearchTerm !== "") {
      searchAndDisplay("", true) // 전체 목록 다시 로드, 스크롤 상단으로
    }
  }

  // NotificationHandler.svelte에서 사용할 props
  const notificationHandlerProps = {
    setSearchTerm: (keyword: string) => {
      // 검색창에 키워드 설정 (Search 컴포넌트가 이벤트를 수신하도록)
      const event = new CustomEvent("set-search-term", { detail: keyword })
      document.dispatchEvent(event) // Search 컴포넌트가 이 이벤트를 수신해야 함

      // 내부 상태 업데이트
      searchTermFromQuery = keyword
      isRecommendedSearch = true // 알림을 통한 검색은 추천 검색으로 간주

      // 이미 동일한 검색어로 검색 중이면 중복 실행 방지
      if (isSearching && keyword === lastSearchTerm) {
        return
      }

      // 중복 검색이 아닌 경우에만 실행
      if (!isSearching || keyword !== lastSearchTerm) {
        searchAndDisplay(keyword, true) // 검색 실행 및 스크롤 상단으로
      }
    },
  }
</script>

<NotificationHandler />

<div class="container mx-auto pb-4">
  <!-- Top header area (not fixed) - background color same as page background -->
  <div class="py-2 px-4 sticky top-0 z-10 bg-[var(--color-secondary)]">
    <div class="flex flex-col sm:flex-row justify-between items-center w-full px-4">
      <h1 class="text-2xl font-bold text-center sm:text-left sm:mr-auto">MCP List ({$sharedDataStore.loaded ? (searchTermFromQuery ? mcpCards.length : $sharedDataStore.counts.listCount || pageInfo.total_items) : pageInfo.total_items})</h1>

      <!-- Search UI -->
      <div class="relative w-full max-w-xs mx-auto sm:mx-0 sm:w-64 mt-2 sm:mt-0 sm:ml-auto">
        {#if isRecommendedSearch}
          <span class="absolute left-[-20px] top-3 text-yellow-500" title="Recommended Search">✨</span>
        {/if}

        <Search initialValue={searchTermFromQuery} placeholder="Search servers..." customClass="input input-bordered w-full" on:search={handleSearch} />

        {#if loading && !mcpCards.length}
          <span class="loading loading-spinner loading-xs absolute right-3 top-3"></span>
        {/if}
      </div>
    </div>
  </div>

  <!-- Add padding to content area -->
  <div class="mt-3 px-4">
    <!-- Loading indicator -->
    {#if loading && mcpCards.length === 0 && !searchTermFromQuery}
      <div class="text-center py-10">
        <div class="flex flex-col items-center gap-4">
          <span class="loading loading-spinner loading-lg text-secondary"></span>
          <p>Loading MCP servers...</p>
        </div>
      </div>
      <!-- MCP cards grid -->
    {:else if mcpCards.length > 0}
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        {#each mcpCards as card (card.id)}
          <MCPCard {...card} mode="" />
        {/each}
      </div>

      <!-- Loading more indicator -->
      {#if loadingMore}
        <div class="flex justify-center items-center py-4">
          <span class="loading loading-sm loading-spinner text-secondary"></span>
        </div>
      {/if}

      <!-- All loaded message -->
      {#if allLoaded && !loadingMore}
        <div class="text-center py-4 text-gray-500">All MCPs loaded.</div>
      {/if}
      <!-- No results message -->
    {:else if searchTermFromQuery}
      <div class="text-center py-10 text-gray-500">
        <p>No search results for "{searchTermFromQuery}"</p>
        <button class="btn btn-sm btn-outline mt-3" on:click={handleClearSearch}>Delete search</button>
      </div>
      <!-- Initially no cards and no search term (empty state) -->
    {:else}
      <div class="text-center py-10 text-gray-500">
        <p>No MCPs found.</p>
        <p class="mt-2">Search or filter to find MCPs.</p>
      </div>
    {/if}
  </div>
</div>

<style>
  /* Add any specific styles for this page if needed */
  .container {
    max-width: 1200px; /* Optional: Set a max width */
  }

  /* Scrollbar styles */
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
