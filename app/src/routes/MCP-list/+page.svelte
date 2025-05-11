<script lang="ts">
  import { onMount } from 'svelte';
  import Search from '../../lib/components/search.svelte';
  import MCPCard from '../../lib/components/mcp-card.svelte';
  import { fetchMCPCards } from '../../lib/data/mcp-api';
  import { page } from '$app/stores';
  import { listen } from '@tauri-apps/api/event';
  import { WebviewWindow } from '@tauri-apps/api/webviewWindow';
  
  // define data type to receive from backend
  import type { MCPCard as MCPCardType } from '../../lib/data/mcp-api';
  
  // MCP card data
  let mcpCards: MCPCardType[] = [];
  
  // data loading state
  let loading = true;
  
  // START: 추가된 변수
  let searchTermFromQuery = ''; // URL에서 가져온 검색어를 저장할 변수
  let isRecommendedSearch = false; // 추천 검색 여부 상태 변수
  // END: 추가된 변수
  
  // get data when component is mounted
  onMount(async () => {
    // START: 메인 윈도우 이벤트 리스너 추가 (네비게이션 및 중앙 이동)
    const unlistenNavigate = await listen('navigate-to-mcp-list-with-keyword', async (event) => {
      const newUrl = event.payload as string;
      if (newUrl && typeof newUrl === 'string') {
        const url = new URL(newUrl, window.location.origin); // Full URL로 만듬
        const keyword = url.searchParams.get('keyword');
        if (keyword) {
          searchTermFromQuery = keyword;
          isRecommendedSearch = true;
          await searchAndDisplay(keyword); 
        } else {
          searchTermFromQuery = ''; // 키워드가 없으면 초기화
          isRecommendedSearch = false;
        }
        // SvelteKit의 goto 대신 window.location 할당 또는 메인 윈도우의 navigate 명령을 통해 이동
        // window.location.href = newUrl; // 이 방식은 페이지를 새로고침하므로 SPA에 부적합
        // goto(newUrl)를 사용하거나, 메인 윈도우가 직접 navigate 하도록 해야 함.
        // 여기서는 이미 MCP-List 페이지로 왔다고 가정하고 키워드 처리만.
      }
    });

    // (선택적) 메인 윈도우 중앙 이동 이벤트 리스너 (Positioner 사용)
    // const unlistenMoveToCenter = await listen('move-main-to-center', async () => {
    //   try {
    //     const { move_window, Position } = await import('tauri-plugin-positioner-api');
    //     const mainWindow = WebviewWindow.getByLabel('main');
    //     if (await mainWindow) { // mainWindow가 Promise이므로 await 처리
    //       // Positioner는 현재 컨텍스트의 창에 작용하므로, 메인 윈도우에서 직접 호출 필요
    //       // 이 코드는 MCP-list 페이지(메인 윈도우의 일부)에서 실행되므로 잘 동작할 것
    //       await move_window(Position.Center);
    //     }
    //   } catch (error) {
    //     console.error("Failed to move main window to center:", error);
    //   }
    // });
    // END: 메인 윈도우 이벤트 리스너 추가

    // START: URL query parameter에서 keyword 읽어오기
    const unsubscribePage = page.subscribe(p => {
      console.log('[MCP-list] $page store 구독 콜백 실행, URL:', p.url.toString());
      const keyword = p.url.searchParams.get('keyword');
      console.log('[MCP-list] URL에서 추출한 keyword:', keyword);
      if (keyword) {
        if (searchTermFromQuery !== keyword) { // 이벤트보다 늦게 실행되거나, 직접 URL 접근 시
          searchTermFromQuery = keyword;
          isRecommendedSearch = true;
          console.log('[MCP-list] searchTermFromQuery 할당됨:', searchTermFromQuery);
          searchAndDisplay(searchTermFromQuery);
        }
      } else {
        console.log('[MCP-list] URL에 keyword 없음, 전체 목록 로드 시도');
        if (searchTermFromQuery !== '') { // 이전 검색어가 있었다면 초기화
          searchTermFromQuery = '';
        }
        isRecommendedSearch = false;
        fetchAllMCPs();
      }
    });
    // END: URL query parameter에서 keyword 읽어오기

    // 컴포넌트 파괴 시 리스너 및 구독 해제
    return () => {
      unlistenNavigate();
      // unlistenMoveToCenter?.(); // 선택적 리스너 해제
      unsubscribePage();
    };
  });
  
  // START: 검색 실행 및 결과 표시 함수
  async function searchAndDisplay(term: string) {
    if (!term) {
      isRecommendedSearch = false; // 검색어가 없으면 추천 상태도 해제
      return fetchAllMCPs(); // 용어가 없으면 전체 로드
    }
    // isRecommendedSearch는 term이 있을 때만 true일 수 있으므로, 여기서는 변경하지 않음
    loading = true;
    try {
      mcpCards = await fetchMCPCards(term);
    } catch (error) {
      console.error('검색 중 오류 발생:', error);
      mcpCards = []; // 오류 시 빈 배열로 초기화
    } finally {
      loading = false;
    }
  }
  // END: 검색 실행 및 결과 표시 함수

  // START: 모든 MCP 카드 가져오는 함수
  async function fetchAllMCPs() {
    isRecommendedSearch = false; // 전체 목록 조회 시 추천 상태 해제
    loading = true;
    try {
      mcpCards = await fetchMCPCards();
    } catch (error) {
      console.error('MCP 데이터를 가져오는 중 오류 발생:', error);
      mcpCards = []; // 오류 시 빈 배열로 초기화
    } finally {
      loading = false;
    }
  }
  // END: 모든 MCP 카드 가져오는 함수

  // Search event handler
  async function handleSearchEvent(event: CustomEvent<{ value: string }>) {
    const searchTerm = event.detail.value;
    // 사용자가 직접 검색어를 입력/변경하여 검색했으므로 추천 상태 해제
    if (isRecommendedSearch && searchTerm !== searchTermFromQuery) {
        isRecommendedSearch = false;
    }
    // 또는, searchTermFromQuery와 searchTerm이 다르면 무조건 isRecommendedSearch = false;
    // 하지만, 사용자가 추천 검색어를 지웠다가 다시 다같이 입력하는 경우도 고려해야 함.
    // 가장 간단한 방법은 Search 컴포넌트에서 on:input 발생 시 isRecommendedSearch = false 하는 것.
    // 여기서는 일단, 검색 이벤트가 발생하면 추천 상태를 해제하는 것으로 MOCKUP
    isRecommendedSearch = false; 

    // URL을 업데이트하지 않으므로, searchTermFromQuery는 변경하지 않음.
    // searchAndDisplay는 현재 검색창의 값(searchTerm)으로 검색.
    await searchAndDisplay(searchTerm);
  }
  
</script>

<div class="p-8">
  <div class="flex flex-col gap-6">
    <!-- card list area -->

      <div class="flex justify-between items-center mb-2">
        <p class="text-xl font-semibold">Search results ({mcpCards.length} MCPs)</p>
        <div class="search-area-wrapper flex items-center ml-auto">
          {#if isRecommendedSearch}
            <span class="mr-2 text-yellow-500" title="추천 검색">✨</span>
          {/if}
          <div class="search-component-wrapper w-72 rounded-[10px]">
            {#key searchTermFromQuery}
              <Search on:search={handleSearchEvent} initialValue={searchTermFromQuery} />
            {/key}
          </div>
        </div>
      </div>
      
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
              url={card.url}
              stars={card.stars}
            />
          {/each}
        </div>
      {/if}
  </div>
</div> 