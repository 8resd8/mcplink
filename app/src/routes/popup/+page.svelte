<script lang="ts">
  import { onMount } from 'svelte';
  // import { goto } from '$app/navigation'; // 직접 사용하지 않으므로 주석 처리 또는 삭제 가능
  import { getCurrentWebviewWindow, WebviewWindow } from '@tauri-apps/api/webviewWindow';
  // import { TauriEvent } from '@tauri-apps/api/event'; // 직접 사용하지 않으므로 주석 처리 또는 삭제 가능
  import type { Window as TauriWindow } from '@tauri-apps/api/window';
  import { emitTo } from '@tauri-apps/api/event';
  import { move_window, Position } from 'tauri-plugin-positioner-api'; // 상단에서 import

  let tag = '';
  let currentPopupWebviewWindow: WebviewWindow | null = null;

  onMount(async () => {
    console.log("[Popup] onMount 시작");
    const urlParams = new URLSearchParams(window.location.search);
    tag = urlParams.get('tag') || 'N/A';
    console.log("[Popup] URL에서 읽은 태그:", tag);
    currentPopupWebviewWindow = getCurrentWebviewWindow();

    // setTimeout 제거하고 바로 실행
    console.log("[Popup] Tauri API 직접 접근 시도");
    if ((window as any).__TAURI_INVOKE__) { // 또는 직접 currentPopupWebviewWindow?.setTitle 등으로 API 사용 가능 여부 확인
      console.log("[Popup] Tauri 환경 감지 (onMount 내부)");
      try {
        await currentPopupWebviewWindow?.setTitle(`알림: ${tag}`);
        console.log("[Popup] 제목 설정 시도 완료");
        if (currentPopupWebviewWindow) {
          console.log("[Popup] move_window(Position.BottomRight) 호출 전");
          await move_window(Position.BottomRight); // API 직접 호출
          console.log("[Popup] move_window(Position.BottomRight) 호출 후 - 창 우측 하단 이동 완료");
        } else {
          console.warn("[Popup] currentPopupWebviewWindow가 null이라 위치 이동 안 함");
        }
      } catch (e) {
        console.error("[Popup] Failed to move popup window using positioner or set title:", e);
      }
    } else {
      console.log("[Popup] Tauri 환경 아님 (onMount 내부) - 위치 이동 로직 실행 불가");
    }
    console.log("[Popup] onMount 종료");
  });

  async function handleConfirm() {
    console.log("[Popup] handleConfirm 시작, 현재 태그:", tag);
    if (!tag || tag === 'N/A') {
      console.error("[Popup] 태그 없음 또는 N/A, 이벤트 발생 안 함");
      return;
    }

    try {
      console.log("[Popup] 'move-main-to-center' 이벤트 발생 시도, target: 'main'");
      await emitTo('main', 'move-main-to-center', null); 
      console.log("[Popup] 'move-main-to-center' 이벤트 발생 완료");

      const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`;
      console.log(`[Popup] 'navigate-to' 이벤트 발생 시도, target: 'main', payload: '${targetUrl}'`);
      await emitTo('main', 'navigate-to', targetUrl);
      console.log("[Popup] 'navigate-to' 이벤트 발생 완료");
      
      console.log("[Popup] 팝업 창 닫기 시도");
      await currentPopupWebviewWindow?.close();
      console.log("[Popup] 팝업 창 닫기 완료");
    } catch (error) {
      console.error('[Popup] Error during confirm:', error);
    }
  }

</script>

<style>
  .popup-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100vh;
    padding: 15px;
    background-color: #f9fafb;
    border-radius: 8px;
    text-align: center;
    box-sizing: border-box;
  }
  .keyword-display {
    font-size: 1.1em;
    font-weight: bold;
    color: #1f2937;
    margin-bottom: 15px;
  }
  .confirm-button {
    padding: 10px 20px;
    background-color: #3b82f6;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 1em;
    font-weight: 500;
    transition: background-color 0.2s ease-in-out;
  }
  .confirm-button:hover {
    background-color: #2563eb;
  }
</style>

<div class="popup-container">
  <div class="keyword-display">선택된 키워드: {tag}</div>
  <button class="confirm-button" on:click={handleConfirm}>
    추천 확인
  </button>
</div>