<script lang="ts">
  import "../app.css"
  import { onMount, onDestroy, afterUpdate, tick } from "svelte"
  import { browser } from "$app/environment"
  import type { Window } from "@tauri-apps/api/window"
  import { Presentation, Cog, Minus, X, Square, Settings } from "lucide-svelte"
  import { page } from "$app/stores"
  import { listen } from "@tauri-apps/api/event"
  import type { UnlistenFn } from "@tauri-apps/api/event"
  import { goto } from "$app/navigation"
  import { getCurrentWindow, UserAttentionType } from "@tauri-apps/api/window"

  // Tauri FS, Path, OS API 임포트
  import { exists } from "@tauri-apps/plugin-fs"
  import { appDataDir, join } from "@tauri-apps/api/path"
  import { platform as getOsPlatform } from "@tauri-apps/plugin-os" // platform 변수명 충돌 방지
  import { invoke } from "@tauri-apps/api/core"

  // Tauri window object to save
  let tauriWindow: Window | null = null
  // OS platform detection (UI용)
  let platform: string = "unknown"

  // START: 추가된 변수 (이벤트 리스너 해제용)
  let unlistenMoveToCenter: UnlistenFn | undefined
  let unlistenNavigateTo: UnlistenFn | undefined
  // END: 추가된 변수

  onMount(async () => {
    // 설정 파일 확인 및 리다이렉션 로직
    console.log("[Layout] onMount 시작 - 설정 확인 및 Tauri 초기화 진행")

    // 브라우저 환경에서만 실행
    if (browser) {
      try {
        // OS 타입 확인
        const osType: string = await getOsPlatform()
        platform = osType // UI용 platform 변수 할당

        console.log(`[Layout] 감지된 OS: ${platform}`)

        // 설정 파일 확인 로직
        const CLAUDE_CONFIG_FILE = "claude_desktop_config.json"
        const MCPLINK_CONFIG_FILE = "mcplink_desktop_config.json"
        const FIRST_INSTALL_PATH = "/first-install"
        const DEFAULT_PATH_AFTER_INSTALL = "/Installed-MCP"
        const currentPath = $page?.url?.pathname

        // Windows 환경에서만 설정 파일 확인
        if (osType === "windows") {
          let configFilesFound = false

          try {
            // 백엔드에서 directly 설정 파일 확인
            const claudeConfigExists = await invoke<boolean>("check_claude_config_exists")
            console.log(`[Layout] 설정 파일 확인: claude_desktop_config.json 존재: ${claudeConfigExists}`)

            const mcplinkConfigExists = await invoke<boolean>("check_mcplink_config_exists")
            console.log(`[Layout] 설정 파일 확인: mcplink_desktop_config.json 존재: ${mcplinkConfigExists}`)

            if (claudeConfigExists && mcplinkConfigExists) {
              configFilesFound = true
            }

            // 리다이렉션 로직
            if (!configFilesFound && currentPath !== FIRST_INSTALL_PATH) {
              console.log(`[Layout] 설정 파일이 없습니다. ${FIRST_INSTALL_PATH}로 리다이렉션합니다.`)
              await goto(FIRST_INSTALL_PATH, { replaceState: true })
              return
            } else if (configFilesFound && currentPath === FIRST_INSTALL_PATH) {
              console.log(`[Layout] 설정 파일이 존재합니다. ${DEFAULT_PATH_AFTER_INSTALL}로 리다이렉션합니다.`)
              await goto(DEFAULT_PATH_AFTER_INSTALL, { replaceState: true })
              return
            }
          } catch (e) {
            console.error("[Layout] 설정 파일 확인 중 오류 발생:", e)
            // 오류 발생 시 안전하게 first-install로 이동
            if (currentPath !== FIRST_INSTALL_PATH) {
              await goto(FIRST_INSTALL_PATH, { replaceState: true })
              return
            }
          }
        }
      } catch (e) {
        console.error("[Layout] OS 감지 오류:", e)
        platform = "unknown"
      }
    }

    // --- START: 기존 Tauri 초기화 및 이벤트 리스너 로직 ---
    if (typeof window !== "undefined" && "__TAURI__" in window) {
      try {
        // 플랫폼 정보 가져오기 및 현재 창 참조 설정
        tauriWindow = await getCurrentWindow()

        // OS 정보 가져오기 (@tauri-apps/api/os)
        try {
          const os = await import("@tauri-apps/plugin-os")
          platform = await os.platform()
        } catch (error) {
          console.error("[Layout] Failed to get platform info:", error)
        }

        console.log("[Layout] Tauri 환경 감지, 플랫폼:", platform)

        unlistenMoveToCenter = await listen("move-main-to-center", async () => {
          try {
            // move_window 대신 직접 창 활성화 메서드 사용
            if (tauriWindow) {
              console.log("[Layout] 창 활성화 시도: show()")
              await tauriWindow.show()
              console.log("[Layout] 창 활성화 시도: unminimize()")
              await tauriWindow.unminimize()
              console.log("[Layout] 창 활성화 시도: setFocus()")
              await tauriWindow.setFocus()
              console.log("[Layout] 창 활성화 시도: requestUserAttention()")

              // Windows에서는 requestUserAttention을 사용하여 작업 표시줄 깜빡임
              if (platform === "win32") {
                try {
                  await tauriWindow.requestUserAttention(UserAttentionType.Critical)
                  console.log("[Layout] requestUserAttention 성공")
                } catch (attentionError) {
                  console.error("[Layout] requestUserAttention 실패:", attentionError)
                }
              }

              console.log("[Layout] Window activated")

              // 창 상태 확인
              const isVisible = await tauriWindow.isVisible()
              console.log("[Layout] Window is visible:", isVisible)

              // 창 위치 및 크기 로깅
              try {
                const position = await tauriWindow.outerPosition()
                const size = await tauriWindow.outerSize()
                console.log("[Layout] Window position:", position, "size:", size)
              } catch (posError) {
                console.error("[Layout] Failed to get window position/size:", posError)
              }
            }
          } catch (error) {
            console.error("[Layout] Failed to activate main window:", error)
          }
        })

        unlistenNavigateTo = await listen("navigate-to", async (event) => {
          if (event.payload && typeof event.payload === "string") {
            goto(event.payload as string)
          }
        })
      } catch (error) {
        console.error("[Layout] error: get current window or platform", error)
      }
    } else {
      console.log("[Layout] 브라우저 환경 또는 Tauri API 없음")
      // 브라우저 환경에서의 OS 감지는 UI 표시용으로만 사용되므로 유지
      if (typeof navigator !== "undefined") {
        if (navigator.userAgent.indexOf("Win") !== -1) platform = "win32"
        else if (navigator.userAgent.indexOf("Mac") !== -1) platform = "darwin"
        else if (navigator.userAgent.indexOf("Linux") !== -1) platform = "linux"
      }
    }
    console.log("[Layout] onMount 종료")
    // --- END: 기존 Tauri 초기화 및 이벤트 리스너 로직 ---
  })

  onDestroy(() => {
    console.log("[Layout] onDestroy 호출됨, 리스너 해제 시도")
    unlistenMoveToCenter?.()
    unlistenNavigateTo?.()
    console.log("[Layout] 리스너 해제 완료")
  })

  async function minimizeWindow() {
    if (tauriWindow) {
      try {
        await tauriWindow.minimize()
      } catch (error) {
        console.error("error: minimize window", error)
      }
    }
  }

  async function maximizeWindow() {
    if (tauriWindow) {
      try {
        const isMaximized = await tauriWindow.isMaximized()
        if (isMaximized) {
          await tauriWindow.unmaximize()
        } else {
          await tauriWindow.maximize()
        }
      } catch (error) {
        console.error("error: maximize window", error)
      }
    }
  }

  async function hideToTray() {
    if (tauriWindow) {
      try {
        await tauriWindow.hide()
      } catch (error) {
        console.error("error: hide to tray", error)
      }
    }
  }

  let activeTab = "/"
  const tabs = [
    { path: "/Installed-MCP", name: "Installed MCP", icon: Presentation, color: "#eef2ff", hoverColor: "#eef2ff", bgColor: "#eef2ff" },
    { path: "/MCP-list", name: "MCP List", icon: Cog, color: "#ecfdf5", hoverColor: "#ecfdf5", bgColor: "#ecfdf5" },
    { path: "/test", name: "Test", icon: Cog, color: "#f0f9ff", hoverColor: "#f0f9ff", bgColor: "#f0f9ff" },
  ]
  const settingsTab = { path: "/settings", name: "Settings", icon: Settings, color: "#f3f4f6", hoverColor: "#f3f4f6", bgColor: "#f3f4f6" }
  $: activeTabBgColor = settingsTab.path === activeTab ? settingsTab.bgColor : tabs.find((tab) => tab.path === activeTab)?.bgColor || "#f8fafc"
  $: isFirstInstallPage = $page?.url?.pathname === "/first-install"
  $: isPopupPage = $page?.url?.pathname === "/popup"
</script>

<div class="flex flex-col h-screen overflow-hidden">
  <div class="flex flex-col overflow-hidden">
    {#if !isPopupPage}
      <div class="h-8 bg-base-300 flex items-center text-xs select-none z-50 fixed top-0 left-0 right-0">
        <!-- 앱 아이콘 -->
        <div class="p-2">
          <img src="/favicon.png" alt="App Icon" class="w-4 h-4" />
        </div>
        <!-- 창 제목 -->
        <div class="flex-1" data-tauri-drag-region>
          <!-- drag-region 클래스 대신 data-tauri-drag-region 속성 사용 고려 -->
          <slot name="title">My App</slot>
        </div>
        <!-- 창 컨트롤 버튼 (최소화, 최대화, 닫기) -->
        <div class="flex items-center">
          <button on:click={minimizeWindow} class="p-2 hover:bg-base-content hover:bg-opacity-10">
            <Minus class="w-5 h-5" />
          </button>
          <button on:click={maximizeWindow} class="p-2 hover:bg-base-content hover:bg-opacity-10">
            <Square class="w-4 h-4" />
          </button>
          <button on:click={hideToTray} class="p-2 hover:bg-base-content hover:bg-opacity-10">
            <X class="w-5 h-5" />
          </button>
        </div>
      </div>
    {/if}

    {#if !isPopupPage && !isFirstInstallPage}
      <div class="tab-bar px-2 bg-transparent flex w-full">
        <div class="flex gap-2">
          {#each tabs as tab}
            <a href={tab.path} class="tab {activeTab === tab.path ? 'active' : ''}" on:click={() => (activeTab = tab.path)} style="--tab-color: {tab.color}; --tab-hover-color: {tab.hoverColor};">
              <svelte:component this={tab.icon} class="w-4 h-4 mr-2" />
              <span>{tab.name}</span>
            </a>
          {/each}
        </div>
        <div class="ml-auto">
          <a
            href={settingsTab.path}
            class="tab {activeTab === settingsTab.path ? 'active' : ''}"
            on:click={() => (activeTab = settingsTab.path)}
            style="--tab-color: {settingsTab.color}; --tab-hover-color: {settingsTab.hoverColor};"
          >
            <svelte:component this={settingsTab.icon} class="w-4 h-4 mr-2" />
            <span>{settingsTab.name}</span>
          </a>
        </div>
      </div>
    {/if}

    <main class="flex-1 overflow-auto p-4" style={!isPopupPage ? "background-color:" + activeTabBgColor + "; margin-top: -1px;" : ""}>
      <slot />
    </main>
  </div>
</div>

<style>
  .titlebar {
    height: 3rem;
    user-select: none;
    cursor: grab;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 9999;
    background-color: transparent;
  }
  .titlebar .drag-region {
    flex-grow: 1;
  }
  .titlebar:active {
    cursor: grabbing;
  }
  @media (prefers-color-scheme: dark) {
    .titlebar.darwin button {
    }
  }
  .tab-bar {
    margin-top: 3rem;
    padding-top: 0.5rem;
    border-bottom: 1px solid #e5e7eb;
  }
  .tab {
    padding: 0.5rem 1rem;
    border-radius: 0.375rem 0.375rem 0 0;
    display: flex;
    align-items: center;
    cursor: pointer;
    transition:
      background-color 0.2s ease-in-out,
      color 0.2s ease-in-out;
    background-color: var(--tab-color);
    color: #374151;
    font-weight: 500;
  }
  .tab:hover {
    background-color: var(--tab-hover-color);
    color: #1f2937;
  }
  .tab.active {
    background-color: var(--tab-hover-color);
    color: #111827;
    font-weight: 600;
    position: relative;
  }
  .tab.active::after {
    content: "";
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 2px;
  }
  main {
    padding-top: 1rem;
  }
</style>
