<script lang="ts">
  // import { WebviewWindow } from '@tauri-apps/api/webviewWindow'; // 더 이상 사용되지 않으므로 주석 처리 또는 제거
  import { invoke } from "@tauri-apps/api/core"
  import { onMount, onDestroy } from "svelte"
  import { isPermissionGranted, requestPermission, sendNotification } from "@tauri-apps/plugin-notification"
  import { getCurrentWindow } from "@tauri-apps/api/window"
  import { emit, listen } from "@tauri-apps/api/event"

  // 클릭 가능한 태그 목록
  const tags: string[] = [
    "AWS",
    "aws",
    "BRAVE",
    "brave",
    "SEARCH",
    "search",
    "EVERART",
    "everart",
    "EVERYTHING",
    "everything",
    "FETCH",
    "fetch",
    "FILESYSTEM",
    "filesystem",
    "FILE",
    "file",
    "SYSTEM",
    "system",
    "GIT",
    "git",
    "GITHUB",
    "github",
    "GITLAB",
    "gitlab",
    "DRIVE",
    "drive",
    "GOOGLE",
    "google",
    "MAP",
    "map",
    "MEMORY",
    "memory",
    "POSTGRES",
    "postgres",
    "PUPPETEER",
    "puppeteer",
    "REDIS",
    "redis",
    "SENTRY",
    "sentry",
    "SEQUENTIAL",
    "sequential",
    "THINKING",
    "thinking",
    "THINK",
    "think",
    "SLACK",
    "slack",
    "SQLITE",
    "sqlite",
    "TIME",
    "time",
  ]

  // --- CLAUDE CODE 수정 시작 ---
  // 버튼 클릭 시 시스템 알림 발생 - 프론트엔드에서 직접 알림 생성
  async function handleButtonClick(tag: string) {
    console.log(`[Test Page] 시스템 알림 버튼 클릭됨: ${tag}`)
    try {
      // 알림 권한 확인 및 요청
      let permissionGranted = await isPermissionGranted()
      if (!permissionGranted) {
        const permission = await requestPermission()
        permissionGranted = permission === "granted"
      }

      if (permissionGranted) {
        // 프론트엔드에서 직접 알림 생성
        await sendNotification({
          title: "추천 확인",
          body: `선택된 키워드: ${tag}. 클릭하여 확인하세요.`,
          icon: "icons/icon.png",
        })
        console.log(`[Test Page] 알림 발송 완료 (태그: ${tag})`)
        
        // 로깅용: 알림 상태 확인 이벤트를 콘솔에 출력
        console.log(`[Test Page] 알림 발송 후 이벤트 처리 대기 중... 알림이 표시되면 클릭해보세요.`)
        console.log(`[Test Page] 알림 클릭 시 자동으로 태그(${tag})로 MCP-list 페이지를 열고 검색합니다.`)
        
        // 알림 클릭 시뮬레이션 버튼 추가 (개발용)
        const simulateClick = confirm(`[개발 모드] "${tag}" 알림 클릭을 시뮬레이션하시겠습니까? (알림 클릭 테스트용)`);
        if (simulateClick) {
          console.log(`[Test Page] 알림 클릭 이벤트 시뮬레이션 - 태그: ${tag}`);
          
          // 메인 창 활성화
          await activateMainWindow();
          
          // MCP-list 페이지로 이동하고 태그로 검색 실행
          const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`;
          console.log(`[Test Page] 시뮬레이션 - MCP-list 페이지 이동 URL: ${targetUrl}`);
          
          // navigate-to 이벤트 발생
          emit("navigate-to", targetUrl);
          
          // MCP-list 검색 이벤트 발생
          emit("navigate-to-mcp-list-with-keyword", targetUrl);
        }
        
        // 알림 클릭 후 자동 시뮬레이션 - 실제 알림이 작동하지 않을 경우를 대비해 자동 활성화
        // 알림 전송 후 3초 후에 자동으로 알림 클릭 시뮬레이션 수행
        setTimeout(async () => {
          console.log(`[Test Page] 알림 클릭 자동 시뮬레이션 시작 - 태그: ${tag}`);
          await activateMainWindow();
          const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`;
          emit("navigate-to", targetUrl);
          emit("navigate-to-mcp-list-with-keyword", targetUrl);
        }, 3000);
      } else {
        console.warn("[Test Page] 알림 권한이 없습니다.")
        alert("알림 권한이 필요합니다. 시스템 설정에서 확인해 주세요.")
      }
    } catch (error) {
      console.error("[Test Page] Failed to send notification:", error)
      alert(`알림 발송 실패: ${error}`)
    }
  }
  // --- CLAUDE CODE 수정 끝 ---

  // --- CLAUDE CODE 수정 시작 ---
  // 알림 버튼 형식을 사용하여 알림 생성 (백엔드 활용)
  async function handleBackendNotification(tag: string) {
    console.log(`[Test Page] 백엔드 알림 버튼 클릭됨: ${tag}`)
    try {
      // 백엔드에서 알림 표시 함수 호출
      await invoke("show_popup", { tag })
      console.log(`[Test Page] show_popup 호출 완료 (태그: ${tag})`)
      
      // 백엔드 알림 표시 후 알림 클릭 시뮬레이션
      const simulateClick = confirm(`[개발 모드] 백엔드 알림에 대한 "${tag}" 알림 클릭을 시뮬레이션하시겠습니까? (알림 클릭 테스트용)`);
      if (simulateClick) {
        console.log(`[Test Page] 백엔드 알림 클릭 이벤트 시뮬레이션 - 태그: ${tag}`);
        
        // 메인 창 활성화
        await activateMainWindow();
        
        // MCP-list 페이지로 이동하고 태그로 검색 실행
        const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`;
        console.log(`[Test Page] 시뮬레이션 - 백엔드 알림 MCP-list 페이지 이동 URL: ${targetUrl}`);
        
        // navigate-to 이벤트 발생
        emit("navigate-to", targetUrl);
        
        // MCP-list 검색 이벤트 발생
        emit("navigate-to-mcp-list-with-keyword", targetUrl);
      }
      
      // 백엔드 알림에 대한 자동 시뮬레이션 - 실제 알림이 작동하지 않을 경우를 대비해 자동 활성화
      setTimeout(async () => {
        console.log(`[Test Page] 백엔드 알림 클릭 자동 시뮬레이션 시작 - 태그: ${tag}`);
        await activateMainWindow();
        const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`;
        emit("navigate-to", targetUrl);
        emit("navigate-to-mcp-list-with-keyword", targetUrl);
      }, 3000);
    } catch (error) {
      console.error("[Test Page] Failed to invoke show_popup:", error)
      alert(`알림 발송 실패: ${error}`)
    }
  }
  // --- CLAUDE CODE 수정 끝 ---

  // 알림 클릭 이벤트 감지용 리스너
  let unlistenNotificationAction: (() => void) | undefined = undefined

  onMount(async () => {
    // 알림 권한 확인 및 요청
    let permissionGranted = await isPermissionGranted()
    if (!permissionGranted) {
      const permission = await requestPermission()
      permissionGranted = permission === "granted"
    }

    if (permissionGranted) {
      console.log("[Test Page] 알림 권한 승인됨. 알림 클릭 리스너 등록 중...")

      // --- CLAUDE CODE 수정 시작 ---
      // 알림 클릭 이벤트 리스너 등록 - Tauri v2 방식 적용
      try {
        // 다양한 이벤트를 시도해 알림 클릭을 검출
        const events = ["tauri://notification-clicked", "tauri://notification-action", "notification://action"];
        
        // 모든 가능한 이벤트에 대해 리스너 등록 시도
        for (const eventName of events) {
          console.log(`[Test Page] ${eventName} 이벤트에 대한 리스너 등록 시도`);
          
          try {
            const unlisten = await listen(eventName, async (event) => {
              console.log(`[Test Page] 알림 이벤트 감지됨: ${eventName}`, event);
              
              // 1. 메인 창 활성화
              await activateMainWindow();
              
              // 2. 이벤트 페이로드에서 태그 정보 추출
              let tag = null;
              const payload = event.payload;
              console.log(`[Test Page] 이벤트 페이로드:`, payload);
              
              if (payload) {
                try {
                  // 다양한 페이로드 구조에 대응
                  if (typeof payload === 'object') {
                    // 1. body 속성에서 추출 시도
                    if ('body' in payload) {
                      const text = payload.body as string;
                      const match = text.match(/선택된 키워드: (.*?)\. 클릭하여/);
                      if (match && match[1]) tag = match[1];
                    }
                    
                    // 2. notification 속성에서 추출 시도
                    if (!tag && 'notification' in payload && typeof payload.notification === 'object') {
                      const notification = payload.notification;
                      if ('body' in notification) {
                        const text = notification.body as string;
                        const match = text.match(/선택된 키워드: (.*?)\. 클릭하여/);
                        if (match && match[1]) tag = match[1];
                      }
                    }
                    
                    // 3. 직접 태그 속성 찾기
                    if (!tag && 'tag' in payload) {
                      tag = payload.tag as string;
                    }
                  }
                  
                  // 문자열인 경우 직접 추출 시도
                  if (!tag && typeof payload === 'string') {
                    const match = payload.match(/선택된 키워드: (.*?)\. 클릭하여/);
                    if (match && match[1]) tag = match[1];
                  }
                } catch (extractError) {
                  console.error(`[Test Page] 태그 추출 중 오류:`, extractError);
                }
              }
              
              // 추출된 태그가 있는 경우 MCP-list 페이지로 이동
              if (tag) {
                console.log(`[Test Page] 알림에서 추출한 태그: ${tag}`);
                
                // MCP-list 페이지로 이동 및 태그 전달
                const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`;
                console.log(`[Test Page] 이동할 대상 URL: ${targetUrl}`);
                
                // 페이지 이동 및 검색 이벤트 발생
                await emit("navigate-to", targetUrl);
                await emit("navigate-to-mcp-list-with-keyword", targetUrl);
              } else {
                console.error(`[Test Page] 알림에서 태그를 추출할 수 없습니다. 페이로드:`, payload);
              }
            });
            
            console.log(`[Test Page] ${eventName} 이벤트 리스너 등록 성공`);
            
            // 첫 번째 리스너만 저장 (메모리 관리용)
            if (!unlistenNotificationAction) {
              unlistenNotificationAction = unlisten;
            }
          } catch (listenerError) {
            console.error(`[Test Page] ${eventName} 이벤트 리스너 등록 실패:`, listenerError);
          }
        }
      } catch (error) {
        console.error("[Test Page] 알림 이벤트 리스너 등록 중 오류:", error);
      }
      // --- CLAUDE CODE 수정 끝 ---

      console.log("[Test Page] 알림 클릭 리스너 등록 완료")
    } else {
      console.warn("[Test Page] 알림 권한이 승인되지 않았습니다.")
    }
  })

  // 창 활성화 함수
  async function activateMainWindow() {
    try {
      const mainWindow = getCurrentWindow()
      await mainWindow.show()
      await mainWindow.unminimize()
      await mainWindow.setFocus()
      console.log("[Test Page] 메인 창 활성화됨")
    } catch (error) {
      console.error("[Test Page] 메인 창 활성화 실패:", error)
    }
  }

  onDestroy(() => {
    // 컴포넌트 파괴 시 리스너 해제
    if (unlistenNotificationAction) {
      console.log("[Test Page] 알림 클릭 리스너 해제")
      unlistenNotificationAction()
    }
  })
</script>

<div class="container mx-auto p-4 text-center">
  <h1 class="text-2xl font-bold mb-6">시스템 알림 테스트 (프론트엔드)</h1>
  <p class="mb-4">아래 키워드를 클릭하면 프론트엔드에서 시스템 알림이 생성됩니다:</p>
  <div class="tags-container">
    {#each tags as tag}
      <button class="tag-button" on:click={() => handleButtonClick(tag)}>
        {tag}
      </button>
    {/each}
  </div>
</div>

<div class="container p-4">
  <h1 class="text-2xl font-bold mb-4">시스템 알림 테스트 (백엔드)</h1>
  <p class="mb-4">아래 버튼을 클릭하면 백엔드에서 시스템 알림이 표시됩니다:</p>
  <div class="flex flex-wrap gap-2">
    <button class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded" on:click={() => handleBackendNotification("Keyword 1")}>Keyword 1</button>
    <button class="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded" on:click={() => handleBackendNotification("Another Keyword")}>Another Keyword</button>
    <button class="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded" on:click={() => handleBackendNotification("Test Tag")}>Test Tag</button>
    <button class="bg-purple-500 hover:bg-purple-700 text-white font-bold py-2 px-4 rounded" on:click={() => handleBackendNotification("Special:Keyword!")}>Special:Keyword!</button>
  </div>
</div>

<style>
  .tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding: 20px;
    justify-content: center;
  }
  .tag-button {
    padding: 10px 15px;
    background-color: #4a5568;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition:
      background-color 0.2s ease-in-out,
      transform 0.1s ease;
    font-weight: 500;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
  .tag-button:hover {
    background-color: #2d3748;
    transform: translateY(-1px);
  }
  .tag-button:active {
    background-color: #1a202c;
    transform: translateY(0px);
  }
</style>
