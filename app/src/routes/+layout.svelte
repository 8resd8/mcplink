<script lang="ts">
  import "../app.css";
  import Select from "../lib/components/select.svelte";
  import { onMount } from 'svelte';
  import type { Window } from '@tauri-apps/api/window';
  import { Presentation, SearchCode, Cog, Minus, X, Square, BookDashed } from 'lucide-svelte';
  
  // Tauri 창 객체를 저장할 변수 초기화
  let tauriWindow: Window | null = null;
  
  onMount(async () => {
    
    // Tauri API가 있는지 확인
    if (typeof window !== 'undefined' && '__TAURI__' in window) {
      try {
        // 현재 창 가져오기 - Tauri v2용 API
        const { getCurrentWindow } = await import('@tauri-apps/api/window');
        tauriWindow = getCurrentWindow();
      } catch (error) {
        console.error('error: get current window', error);
      }
    } else {
    }
  });
  
  // 최소화 버튼 핸들러
  async function minimizeWindow() {
    if (tauriWindow) {
      try {
        await tauriWindow.minimize();
      } catch (error) {
        console.error('error: minimize window', error);
      }
    }
  }
  
  // 최대화 버튼 핸들러
  async function maximizeWindow() {
    if (tauriWindow) {
      try {
        const isMaximized = await tauriWindow.isMaximized();
        if (isMaximized) {
          await tauriWindow.unmaximize();
        } else {
          await tauriWindow.maximize();
        }
      } catch (error) {
        console.error('error: maximize window', error);
      }
    }
  }
  
  // 트레이로 숨기기 버튼 핸들러
  async function hideToTray() {
    if (tauriWindow) {
      try {
        await tauriWindow.hide();
      } catch (error) {
        console.error('error: hide to tray', error);
      }
    }
  }
</script>

<div class="flex h-screen overflow-hidden bg-base-200">
  <div class="w-64 bg-base-100 shadow-lg">
    <div class="flex flex-col h-full">
      <!-- <Select /> -->
      <nav class="flex-1 mt-15">
        <ul>
          <li class="mb-3">
            <a href="/" class="flex items-center p-3 rounded hover:bg-base-200">
              <Presentation />
                <span class="ml-3">Dashboard</span>
            </a>
          </li>
          <li class="mb-2">
            <a href="/search" class="flex items-center p-3 rounded hover:bg-base-200">
              <SearchCode />
                <span class="ml-3">Search MCP</span>
            </a>
          </li>
          <li class="mb-2">
            <a href="#settings" class="flex items-center p-3 rounded hover:bg-base-200">
              <Cog />
                <span class="ml-3">Settings</span>
            </a>
          </li>
          <li class="mb-2">
            <a href="/detail" class="flex items-center p-3 rounded hover:bg-base-200">
              <BookDashed />
                <span class="ml-3">임시(디테일)</span>
            </a>
          </li>
        </ul>
      </nav>
    </div>
  </div>

  <div class="flex-1 flex flex-col overflow-hidden">
    <!-- 상단바 - 드래그 가능하고 투명 배경으로 변경 -->
    <div class="bg-transparent p-2 flex justify-between border-b border-transparent titlebar">
      <div class="flex-1 drag-region"></div>
      <div class="flex">
        <button
          on:click|preventDefault|stopPropagation={minimizeWindow}
          title="Minimize"
          aria-label="window minimize"
          class="mr-2 text-base-content opacity-70 hover:opacity-100"
        >
        <Minus class="w-5 h-5" />
        </button>
        
        <button
          on:click|preventDefault|stopPropagation={maximizeWindow}
          title="Maximize"
          aria-label="window maximize"
          class="mr-2 text-base-content opacity-70 hover:opacity-100"
        >
        <Square class="w-4 h-4" />
        </button>
        
        <button
          on:click|preventDefault|stopPropagation={hideToTray}
          title="To Tray"
          aria-label="hide to tray"
          class="text-base-content opacity-70 hover:opacity-100"
        >
        <X class="w-5 h-5" />
        </button>
      </div>
    </div>
    
    <main class="flex-1 overflow-auto p-4 bg-base-200">
      <slot />
    </main>
  </div>
</div>

<style>
  /* 창 드래그 가능 영역 설정 */
  .titlebar {
    height: 40px;
  }
  
  .drag-region {
    -webkit-app-region: drag;
    app-region: drag;
    height: 100%;
  }
  
  /* Apply no-drag to clickable elements within the drag region */
  button, a {
    -webkit-app-region: no-drag;
    app-region: no-drag;
  }
</style>