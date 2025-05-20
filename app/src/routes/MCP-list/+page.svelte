<script lang="ts">
  import { onMount, onDestroy, getContext } from "svelte"
  import Search from "../../lib/components/search.svelte"
  import MCPCard from "../../lib/components/mcp-card.svelte"
  import { fetchMCPCards } from "../../lib/data/mcp-api"
  import { page } from "$app/stores"
  import { browser } from "$app/environment"
  import { listen } from "@tauri-apps/api/event"
  import { WebviewWindow } from "@tauri-apps/api/webviewWindow"
  import NotificationHandler from "./NotificationHandler.svelte"
  import { scrollableContainerKey } from "$lib/contexts"

  // define data type to receive from backend
  import type { MCPCard as MCPCardType, PageInfo } from "../../lib/data/mcp-api"

  // MCP card data
  let mcpCards: MCPCardType[] = []
  let pageInfo: PageInfo = { has_next_page: false, end_cursor: null, total_items: 0 }
  let scrollContainerElement: HTMLElement | null = null // 스크롤 컨테이너 DOM 요소
  let scrollHandlerRef: (() => void) | null = null // 스크롤 핸들러 참조 저장

  // data loading state
  let loading = true
  let loadingMore = false
  let allLoaded = false // Flag to check if all data has been loaded
  let justLoadedNewData = false

  // START: Added variables
  let searchTermFromQuery = "" // Variable to store the search term from the URL
  let isRecommendedSearch = false // State variable for recommended search
  let debounceTimer: ReturnType<typeof setTimeout> | null = null // Debounce timer
  // END: Added variables

  // --- Context API related variable ---
  let scrollContainerElementFromContext: HTMLElement | undefined

  // Scroll event handler
  function handleScroll() {
    if (!scrollContainerElement) {
      console.log("[MCP-list Debug] handleScroll: scrollContainerElement is null, aborting.")
      return
    }
    console.log("[MCP-list Debug] handleScroll called. Conditions:", { loadingMore, hasNextPage: pageInfo.has_next_page, allLoaded, justLoadedNewData })
    if (loadingMore || !pageInfo.has_next_page || allLoaded) {
      console.log("[MCP-list Debug] handleScroll: Aborting due to loadingMore, !has_next_page, or allLoaded.")
      return
    }

    if (justLoadedNewData) {
      justLoadedNewData = false // 플래그 리셋
      console.log("[MCP-list Debug] handleScroll: justLoadedNewData was true, reset. No further action in this call.")
      return // 현재 호출은 여기서 종료, 다음 실제 스크롤 이벤트를 기다림
    }

    // Check scroll position of the scroll container element
    // scrollContainerElement는 <main> 태그가 됩니다.
    const scrollPosition = scrollContainerElement.scrollTop + scrollContainerElement.clientHeight
    const scrollHeight = scrollContainerElement.scrollHeight
    console.log("[MCP-list Debug] Scroll positions (Container):", {
      scrollY: scrollContainerElement.scrollTop,
      clientHeight: scrollContainerElement.clientHeight,
      scrollHeight,
      diff: scrollHeight - scrollPosition,
    })

    // Load next page if scroll is 300px from the bottom (detect faster)
    if (scrollHeight - scrollPosition < 300) {
      console.log("[MCP-list Debug] Threshold reached, calling loadNextPage.")
      loadNextPage()
    }
  }

  // Function to load the next page
  async function loadNextPage() {
    console.log("[MCP-list Debug] loadNextPage called. Conditions:", { hasNextPage: pageInfo.has_next_page, endCursor: pageInfo.end_cursor, loadingMore, allLoaded })
    if (!pageInfo.has_next_page || !pageInfo.end_cursor || loadingMore || allLoaded) {
      if (allLoaded) console.log("[MCP-list Debug] loadNextPage: Aborted because allLoaded is true.")
      else if (loadingMore) console.log("[MCP-list Debug] loadNextPage: Aborted because loadingMore is true.")
      else if (!pageInfo.has_next_page) console.log("[MCP-list Debug] loadNextPage: Aborted because no next page.")
      else if (!pageInfo.end_cursor) console.log("[MCP-list Debug] loadNextPage: Aborted because no end cursor.")
      return
    }

    loadingMore = true
    console.log("[MCP-list Debug] loadNextPage: loadingMore set to true. Fetching next page with cursor:", pageInfo.end_cursor, "and term:", searchTermFromQuery)
    try {
      console.log("Loading next page with cursor:", pageInfo.end_cursor);
      
      // Add a small delay to prevent rapid API calls
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const response = await fetchMCPCards(searchTermFromQuery || undefined, pageInfo.end_cursor)
      console.log("[MCP-list Debug] loadNextPage: API response received:", response)

      if (response.cards && response.cards.length > 0) {
        mcpCards = [...mcpCards, ...response.cards]
        pageInfo = response.page_info
        console.log("[MCP-list Debug] loadNextPage: mcpCards updated. Length:", mcpCards.length, "New pageInfo:", pageInfo)
        justLoadedNewData = true
      } else {
        // 카드가 없으면 더 이상 로드할 데이터가 없다고 간주
        pageInfo.has_next_page = false
        console.log("[MCP-list Debug] loadNextPage: No new cards received, assuming no more data.")
      }

      // Check if all data has been loaded
      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        allLoaded = true
        console.log("[MCP-list Debug] loadNextPage: All data loaded (allLoaded = true). Reason:", { hasNextPage: pageInfo.has_next_page, length: mcpCards.length, total: pageInfo.total_items })
      } else {
        allLoaded = false // 명시적으로 false로 설정
        console.log("[MCP-list Debug] loadNextPage: More data might exist (allLoaded = false).")
      }
    } catch (error) {
      console.error("[MCP-list Debug] Error loading next page:", error)
    } finally {
      loadingMore = false
      console.log("[MCP-list Debug] loadNextPage: loadingMore set to false.")
      // 데이터 로드 후 handleScroll을 즉시 호출하여, 만약 스크롤바가 없는 짧은 컨텐츠라도 다음 페이지를 로드해야 하는 경우를 처리
      // 단, justLoadedNewData 플래그 때문에 바로 다음 페이지를 로드하지는 않음.
      // 사용자가 스크롤을 해야 다음 로드가 진행됨.
      // setTimeout(() => handleScroll(), 150) // 이 부분은 사용자의 스크롤을 기다리는 것으로 변경.
    }
  }

  // Page initialization and data loading function
  function initPageAndLoadData(containerElement: HTMLElement | null, scrollToTop = false) {
    if (!containerElement) {
      console.error("[MCP-list Debug] initPageAndLoadData: containerElement is null. Aborting listener setup.")
      return null
    }
    scrollContainerElement = containerElement // 여기서 scrollContainerElement 설정
    console.log("[MCP-list Debug] initPageAndLoadData: scrollContainerElement SET:", scrollContainerElement)

    // 기존 리스너가 있다면 제거
    if (scrollHandlerRef && scrollContainerElement) {
      console.log("[MCP-list Debug] initPageAndLoadData: Removing existing scroll listener.")
      scrollContainerElement.removeEventListener("scroll", scrollHandlerRef)
      scrollHandlerRef = null
    }

    // Add scroll event listener (with debounce)
    let scrollTimer: ReturnType<typeof setTimeout> | null = null
    const newScrollHandler = () => {
      if (!scrollContainerElement) return
      console.log("[MCP-list Debug] Raw scroll event FIRED on container:", scrollContainerElement)
      if (scrollTimer) clearTimeout(scrollTimer)
      scrollTimer = setTimeout(handleScroll, 100) // Debounced handleScroll
    }

    scrollContainerElement.addEventListener("scroll", newScrollHandler)
    scrollHandlerRef = newScrollHandler // 핸들러 참조 저장
    console.log("[MCP-list Debug] initPageAndLoadData: scroll event listener ADDED to CONTAINER:", scrollContainerElement)

    // 초기 데이터 로드
    console.log("[MCP-list Debug] initPageAndLoadData: Calling searchAndDisplay with searchTerm:", searchTermFromQuery, "scrollToTop:", scrollToTop)
    searchAndDisplay(searchTermFromQuery, scrollToTop)

    // 초기 로드 후 스크롤 가능한지 확인 (콘텐츠가 적을 경우 추가 로드)
    // setTimeout(handleScroll, 500) // 이 부분은 searchAndDisplay 후의 로직으로 이동하거나, 첫 데이터 로드 후 상태에 따라 호출
    return newScrollHandler
  }

  // get data when component is mounted
  onMount(() => {
    // 세션 스토리지에서 알림 키워드 확인 (일회성 처리)
    const pendingKeyword = sessionStorage.getItem("pendingSearchKeyword")
    if (pendingKeyword) {
      console.log("Found pending keyword in session storage:", pendingKeyword)
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
        console.log("Found keyword in URL parameters:", urlKeyword)
        // 마지막 알림 키워드가 URL에 있는 키워드와 같으면 제거
        if (localStorage.getItem("lastNotificationKeyword") === urlKeyword) {
          localStorage.removeItem("lastNotificationKeyword")
        }
        return
      }

      // 로컬 스토리지에서 마지막 알림 키워드 확인 (백업 처리)
      const lastKeyword = localStorage.getItem("lastNotificationKeyword")
      if (lastKeyword && !pendingKeyword && !searchTermFromQuery) {
        console.log("Using last notification keyword as fallback:", lastKeyword)
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
    try {
      // @ts-ignore
      window.__TAURI__.event.listen("search-keyword", (event) => {
        try {
          // 키워드 추출 (문자열이나 객체 형태에 대응)
          const keyword =
            typeof event.payload === "string"
              ? event.payload // 이미 문자열이면 그대로 사용
              : typeof event.payload === "object" && event.payload && event.payload.keyword
                ? event.payload.keyword // 객체에서 keyword 속성 추출
                : null

          if (keyword) {
            console.log("수신된 검색 키워드:", keyword)

            // 1. 검색창에 키워드 설정 (set-search-term 이벤트 발생)
            try {
              // 검색창에 키워드 설정
              const searchEvent = new CustomEvent("set-search-term", { detail: keyword })
              document.dispatchEvent(searchEvent)
              console.log("검색창에 키워드 설정됨:", keyword)

              // 2. 검색 상태 업데이트 (URL은 업데이트하지 않고 시각적으로만 검색 상태 표시)
              searchTermFromQuery = keyword
              isRecommendedSearch = true

              // 3. 검색 실행 - 검색창에 키워드가 표시된 후 약간 지연시켜 검색 실행
              setTimeout(() => {
                searchAndDisplay(keyword)
              }, 200)
            } catch (e) {
              console.error("검색창 키워드 설정 오류:", e)
            }
          } else {
            console.warn("search-keyword 이벤트에서 키워드를 찾을 수 없음")
          }
        } catch (e) {
          console.error("search-keyword 이벤트 처리 오류:", e)
        }
      })
    } catch (e) {
      console.error("Error setting up search-keyword listener:", e)
    }

    // 2. 추가: activation-complete 이벤트 리스닝
    try {
      // @ts-ignore
      window.__TAURI__.event.listen("activation-complete", () => {
        // 앱이 활성화되었을 때 추가 조치
        // URL 파라미터로 키워드가 있는지 확인
        const urlParams = new URLSearchParams(window.location.search)
        const urlKeyword = urlParams.get("keyword")

        // 이미 URL에 키워드가 있다면, 기존 키워드 처리는 무시
        if (urlKeyword) {
          console.log("이미 URL에 키워드가 있어 알림 키워드 처리 무시:", urlKeyword)

          // 마지막 알림 키워드가 현재 URL과 같으면 제거
          const lastKeyword = localStorage.getItem("lastNotificationKeyword")
          if (lastKeyword === urlKeyword) {
            localStorage.removeItem("lastNotificationKeyword")
          }
          return
        }

        // 세션 스토리지에서 먼저 확인
        const pendingKeyword = sessionStorage.getItem("pendingSearchKeyword")
        if (pendingKeyword) {
          console.log("앱 활성화: 세션 스토리지에서 키워드 발견:", pendingKeyword)
          searchTermFromQuery = pendingKeyword
          isRecommendedSearch = true
          searchAndDisplay(pendingKeyword)

          // 사용 후 제거
          sessionStorage.removeItem("pendingSearchKeyword")
          return
        }

        // 로컬 스토리지에서 확인
        const lastKeyword = localStorage.getItem("lastNotificationKeyword")
        if (lastKeyword && (!searchTermFromQuery || searchTermFromQuery !== lastKeyword)) {
          console.log("앱 활성화: 로컬 스토리지에서 키워드 발견:", lastKeyword)
          searchTermFromQuery = lastKeyword
          isRecommendedSearch = true
          searchAndDisplay(lastKeyword)

          // 사용 후 제거
          localStorage.removeItem("lastNotificationKeyword")
        }
      })
    } catch (e) {
      console.error("Error setting up activation-complete listener:", e)
    }
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

    // 세션 스토리지에서 알림 키워드 확인 (일회성 처리)
    const pendingKeyword = sessionStorage.getItem("pendingSearchKeyword")
    if (pendingKeyword) {
      console.log("[MCP-list Debug] Found pending keyword in session storage:", pendingKeyword)
      searchTermFromQuery = pendingKeyword
      isRecommendedSearch = true
      sessionStorage.removeItem("pendingSearchKeyword")
      localStorage.removeItem("lastNotificationKeyword")
    } else {
      const lastKeyword = localStorage.getItem("lastNotificationKeyword")
      if (lastKeyword && !searchTermFromQuery) {
        console.log("[MCP-list Debug] Using last notification keyword as fallback:", lastKeyword)
        searchTermFromQuery = lastKeyword
        isRecommendedSearch = true
        localStorage.removeItem("lastNotificationKeyword")
      }
    }

    // $page 스토어 구독으로 URL 키워드 처리
    const unsubscribePage = page.subscribe((currentPage) => {
      const urlKeyword = currentPage.url.searchParams.get("keyword")
      if (urlKeyword && urlKeyword !== searchTermFromQuery) {
        console.log("[MCP-list Debug] Keyword from URL params in $page subscription:", urlKeyword)
        searchTermFromQuery = urlKeyword
        isRecommendedSearch = !!currentPage.url.searchParams.get("recommended") // recommended 파라미터로 상태 결정
        if (scrollContainerElement) {
          console.log("[MCP-list Debug] $page.subscribe: Calling searchAndDisplay for URL keyword:", urlKeyword)
          searchAndDisplay(urlKeyword, true)
        } else {
          console.log("[MCP-list Debug] $page.subscribe: scrollContainerElement not ready, search for URL keyword will be handled by onMount logic.")
        }

        if (localStorage.getItem("lastNotificationKeyword") === urlKeyword) {
          localStorage.removeItem("lastNotificationKeyword")
        }
      } else if (!urlKeyword && searchTermFromQuery && !sessionStorage.getItem("pendingSearchKeyword")) {
        // URL에 키워드가 없어졌지만, searchTermFromQuery에 값이 남아있는 경우 (예: 사용자가 검색창에서 직접 지움)
        // 이 경우는 search 컴포넌트의 clear 이벤트 등에서 처리하는 것이 더 적절할 수 있음.
        // 여기서는 일단 아무것도 안함.
      }
    })

    // 1. 추가: search-keyword 이벤트 리스닝
    const unlistenSearchKeyword = listen("search-keyword", (event: any) => {
      // any 타입 사용은 지양해야 하지만, Tauri 이벤트 페이로드 타입 추론이 어려울 때 임시 사용
      try {
        const keyword = typeof event.payload === "string" ? event.payload : typeof event.payload === "object" && event.payload && event.payload.keyword ? event.payload.keyword : null

        if (keyword) {
          console.log("[MCP-list Debug] Received search-keyword event with keyword:", keyword)

          const searchEvent = new CustomEvent("set-search-term", { detail: keyword })
          document.dispatchEvent(searchEvent)

          searchTermFromQuery = keyword
          isRecommendedSearch = true

          if (scrollContainerElement) {
            console.log("[MCP-list Debug] search-keyword event: Calling searchAndDisplay:", keyword)
            searchAndDisplay(keyword, true) // 새 키워드로 검색, 스크롤 상단 이동
          } else {
            console.log("[MCP-list Debug] search-keyword event: scrollContainerElement not ready, search will be handled by onMount logic or $page sub.")
          }
        } else {
          console.warn("[MCP-list Debug] search-keyword event: Keyword not found in payload.", event.payload)
        }
      } catch (e) {
        console.error("[MCP-list Debug] Error processing search-keyword event:", e)
      }
    })

    // 2. 추가: activation-complete 이벤트 리스닝
    // NotificationHandler.svelte에서 처리하므로 여기서는 제거해도 될 수 있음.
    // 또는 중복 로직 검토 필요. 여기서는 일단 유지.
    const unlistenActivationComplete = listen("activation-complete", () => {
      console.log("[MCP-list Debug] activation-complete event received.")
      const urlParams = new URLSearchParams(window.location.search)
      const urlKeyword = urlParams.get("keyword")

      if (urlKeyword) {
        console.log("[MCP-list Debug] activation-complete: Keyword already in URL, no special handling:", urlKeyword)
        if (localStorage.getItem("lastNotificationKeyword") === urlKeyword) {
          localStorage.removeItem("lastNotificationKeyword")
        }
        // searchAndDisplay(urlKeyword, true) // $page 구독에서 처리되므로 중복 호출 방지
        return
      }
      // 알림으로 앱이 활성화된 경우의 키워드 처리는 NotificationHandler.svelte 또는 초기 onMount 로직에서 수행됨.
      // 여기서는 URL 키워드가 없는 경우 특별한 동작을 하지 않음.
    })

    return () => {
      console.log("[MCP-list Debug] onDestroy: Cleaning up listeners.")
      if (scrollContainerElement && scrollHandlerRef) {
        scrollContainerElement.removeEventListener("scroll", scrollHandlerRef)
        console.log("[MCP-list Debug] onDestroy: Scroll listener REMOVED from:", scrollContainerElement)
      }
      if (unsubscribePage) unsubscribePage()

      // Tauri 이벤트 리스너 해제
      unlistenSearchKeyword.then((fn) => fn())
      unlistenActivationComplete.then((fn) => fn())

      if (debounceTimer) clearTimeout(debounceTimer)
    }
  })

  // scrollContainerElementFromContext가 설정되면 스크롤 컨테이너 설정 및 데이터 로드
  $: if (browser && scrollContainerElementFromContext && !scrollContainerElement) {
    console.log("[MCP-list Debug] Reactive: scrollContainerElementFromContext is now available:", scrollContainerElementFromContext)
    scrollContainerElement = scrollContainerElementFromContext
    initPageAndLoadData(scrollContainerElement, true) // 초기 데이터 로드 및 스크롤 리스너 설정
  } else if (browser && !scrollContainerElementFromContext && !scrollContainerElement) {
    // onMount에서 즉시 컨텍스트를 가져오지 못한 경우에 대한 방어 로직 (이론상 발생 안해야 함)
    // 또는, +layout.svelte보다 먼저 이 페이지가 어떤 이유로든 초기화되려 할 때
    console.warn("[MCP-list Debug] Reactive: scrollContainerElementFromContext is NOT YET available. This might indicate a timing issue or an error in context provision from layout.")
    // 필요하다면 여기에 짧은 지연 후 다시 컨텍스트를 시도하는 로직을 넣을 수 있으나,
    // Svelte의 기본 흐름상 +layout.svelte의 컨텍스트 설정이 먼저 이루어져야 함.
  }

  // START: Search execution and result display function
  async function searchAndDisplay(term: string) {
    if (!term) {
      isRecommendedSearch = false // Clear recommendation state if search term is empty
      return fetchAllMCPs() // Load all if term is empty
    }

    // 키워드를 검색창에도 설정 (사용자 피드백용)
    try {
      // 이벤트 발생
      const searchEvent = new CustomEvent("set-search-term", { detail: term })
      document.dispatchEvent(searchEvent)
    } catch (e) {
      console.error("Failed to dispatch search event:", e)
    }

    // isRecommendedSearch can only be true if term exists, so don't change it here
    loading = true
    allLoaded = false // 새로운 검색 시작 시 초기화
    justLoadedNewData = false // 새로운 검색 시작 시 초기화
    mcpCards = [] // 기존 카드 목록 초기화
    pageInfo = { has_next_page: false, end_cursor: null, total_items: 0 } // 페이지 정보 초기화
    console.log("[MCP-list Debug] searchAndDisplay called. Term:", term, "ScrollToTop:", scrollToTop)

    if (scrollToTop && scrollContainerElement) {
      console.log("[MCP-list Debug] searchAndDisplay: Scrolling to top of container:", scrollContainerElement)
      scrollContainerElement.scrollTo(0, 0)
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
                duration: 3000,
              },
            })
            document.dispatchEvent(toastEvent)
          } catch (e) {
            console.error("Toast event error:", e)
          }
        }
      }, 100)

      const response = await fetchMCPCards(term)
      mcpCards = response.cards
      pageInfo = response.page_info
      console.log("[MCP-list Debug] searchAndDisplay: mcpCards populated. Length:", mcpCards.length, "PageInfo:", pageInfo)
      justLoadedNewData = true // 새 데이터 로드됨

      if (!pageInfo.has_next_page || mcpCards.length >= pageInfo.total_items) {
        console.log("All search results loaded in initial fetch");
        allLoaded = true
        console.log("[MCP-list Debug] searchAndDisplay: All data loaded on initial search.")
      } else {
        allLoaded = false
        // 초기 로드 후 컨텐츠가 충분하지 않아 스크롤바가 생기지 않는 경우, 추가 데이터 로드 시도
        // 단, scrollContainerElement가 확실히 있고, DOM 업데이트가 반영된 후 호출해야 함.
        setTimeout(() => {
          if (scrollContainerElement && scrollContainerElement.scrollHeight <= scrollContainerElement.clientHeight && pageInfo.has_next_page && !loadingMore && !allLoaded) {
            console.log("[MCP-list Debug] searchAndDisplay: Content too short, attempting to load next page automatically.")
            loadNextPage()
          }
        }, 300) // DOM 업데이트를 기다리기 위한 짧은 지연
      }

      // 검색 결과 표시
      if (isRecommendedSearch) {
        try {
          // 결과 토스트 메시지
          const resultToast = new CustomEvent("show-toast", {
            detail: {
              message: `'${term}' 키워드 검색 결과: ${mcpCards.length}개 발견`,
              type: mcpCards.length > 0 ? "success" : "warning",
              duration: 3000,
            },
          })
          setTimeout(() => document.dispatchEvent(resultToast), 1000)
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
      console.log("[MCP-list Debug] searchAndDisplay: loading set to false.")
    }
  }

  // Search event handler
  const handleSearch = (event: CustomEvent<{ value: string }>) => {
    const term = event.detail.value
    console.log("[MCP-list Debug] handleSearch event. Term:", term)
    searchTermFromQuery = term
    isRecommendedSearch = false // 사용자가 직접 검색하면 추천 검색 상태 해제

    // URL 업데이트 (optional, 검색 결과 페이지를 공유하고 싶을 때)
    // const currentUrl = new URL(window.location.href)
    // if (term) {
    //   currentUrl.searchParams.set('keyword', term)
    // } else {
    //   currentUrl.searchParams.delete('keyword')
    // }
    // window.history.pushState({}, '', currentUrl.toString())
    // goto(currentUrl.toString(), { replaceState: true, noScroll: true }) // SvelteKit 방식으로 URL 변경

    // 기존 디바운스 타이머가 있다면 취소
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    // 새로운 디바운스 타이머 설정
    debounceTimer = setTimeout(() => {
      console.log("[MCP-list Debug] handleSearch (debounced): Calling searchAndDisplay with term:", term)
      searchAndDisplay(term, true) // 검색 시 스크롤 상단으로
    }, 300) // 300ms 디바운스
  }

  const handleClearSearch = () => {
    console.log("[MCP-list Debug] handleClearSearch event.")
    searchTermFromQuery = ""
    isRecommendedSearch = false
    // URL에서 keyword 파라미터 제거 (optional)
    // const currentUrl = new URL(window.location.href)
    // currentUrl.searchParams.delete('keyword')
    // window.history.pushState({}, '', currentUrl.toString())
    // goto(currentUrl.pathname, { replaceState: true, noScroll: true }) // SvelteKit 방식으로 URL 변경 (쿼리 파라미터 없이)

    // 디바운스 타이머가 있다면 취소
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }
    console.log("[MCP-list Debug] handleClearSearch: Calling searchAndDisplay with empty term.")
    searchAndDisplay("", true) // 전체 목록 다시 로드, 스크롤 상단으로
  }

  // NotificationHandler.svelte에서 사용할 props
  const notificationHandlerProps = {
    setSearchTerm: (keyword: string) => {
      console.log("[MCP-list Debug] setSearchTerm from NotificationHandler:", keyword)
      // 검색창에 키워드 설정 (Search 컴포넌트가 이벤트를 수신하도록)
      const event = new CustomEvent("set-search-term", { detail: keyword })
      document.dispatchEvent(event) // Search 컴포넌트가 이 이벤트를 수신해야 함

      // 내부 상태 업데이트 및 검색 실행
      searchTermFromQuery = keyword
      isRecommendedSearch = true // 알림을 통한 검색은 추천 검색으로 간주
      console.log("[MCP-list Debug] setSearchTerm: Calling searchAndDisplay with keyword:", keyword)
      searchAndDisplay(keyword, true) // 검색 실행 및 스크롤 상단으로
    },
  }
</script>

<NotificationHandler />

<div class="container mx-auto pb-8">
  <!-- Top header area (not fixed) - background color same as page background -->
  <div class="pt-1 pb-2 border-b border-primary-content/10">
    <div class="flex flex-col sm:flex-row justify-between items-center w-full px-4">
      <h1 class="text-2xl font-bold text-center sm:text-left sm:mr-auto">MCP List ({pageInfo.total_items})</h1>

      <!-- Search UI -->
      <div class="relative w-full max-w-xs mx-auto sm:mx-0 sm:w-64 mt-2 sm:mt-0 sm:ml-auto">
        {#if isRecommendedSearch}
          <span class="absolute left-[-20px] top-3 text-yellow-500" title="Recommended Search">✨</span>
        {/if}

        <Search initialValue={searchTermFromQuery} placeholder="Search MCPs..." customClass="input input-bordered w-full pr-10" on:search={(event) => handleSearchEvent(event)} />

        {#if loading && !mcpCards.length}
          <span class="loading loading-spinner loading-xs absolute right-3 top-3"></span>
        {:else}
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="w-5 h-5 absolute right-3 top-3"
          >
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
        {/if}
      </div>
    </div>
  </div>
</div>

<!-- Loading indicator -->
{#if loading && mcpCards.length === 0 && !searchTermFromQuery}
  <div class="flex justify-center items-center h-64">
    <span class="loading loading-lg loading-spinner text-primary"></span>
  </div>
{/if}

<!-- MCP cards grid -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-4">
  {#each mcpCards as card (card.id)}
    <MCPCard {...card} />
  {/each}
</div>

<!-- Loading more indicator -->
{#if loadingMore}
  <div class="flex justify-center items-center py-4">
    <span class="loading loading-sm loading-spinner text-secondary"></span>
  </div>
{/if}

<!-- All loaded message -->
{#if allLoaded && mcpCards.length > 0 && !loadingMore}
  <div class="text-center py-4 text-neutral-500">모든 MCP를 불러왔습니다.</div>
{/if}

<!-- No results message -->
{#if !loading && mcpCards.length === 0 && searchTermFromQuery && !loadingMore}
  <div class="text-center py-10">
    <p class="text-xl font-semibold">"{searchTermFromQuery}"에 대한 검색 결과가 없습니다.</p>
    <p class="text-neutral-500 mt-2">다른 검색어를 입력해보세요.</p>
  </div>
{/if}

<!-- Initially no cards and no search term (empty state) -->
{#if !loading && mcpCards.length === 0 && !searchTermFromQuery && !loadingMore}
  <div class="text-center py-10">
    <p class="text-xl font-semibold">MCP 목록이 비어있습니다.</p>
    <p class="text-neutral-500 mt-2">검색하거나 필터를 사용하여 MCP를 찾아보세요.</p>
  </div>
{/if}

<style>
  /* 이전에 mcpListScrollContainer에 적용했던 스타일은 +layout.svelte의 main 태그로 옮겨졌으므로 여기선 불필요 */
</style>
