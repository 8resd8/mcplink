<script lang="ts">
  import { onMount } from "svelte"
  import { getCurrentWebviewWindow, WebviewWindow } from "@tauri-apps/api/webviewWindow"
  import type { Window as TauriWindow } from "@tauri-apps/api/window"
  import { emitTo } from "@tauri-apps/api/event"

  let tag = ""
  let currentPopupWebviewWindow: WebviewWindow | null = null

  onMount(async () => {
    const urlParams = new URLSearchParams(window.location.search)
    tag = urlParams.get("tag") || "N/A"
    currentPopupWebviewWindow = getCurrentWebviewWindow()

    if (typeof window !== "undefined" && ("__TAURI_INTERNALS__" in window || "__TAURI__" in window)) {
      try {
        await currentPopupWebviewWindow?.setTitle(`Notification: ${tag}`)
      } catch (e) {
        console.error("[Popup] Failed to set window title:", e)
      }
    }
  })

  async function handleConfirm() {
    if (!tag || tag === "N/A") {
      return
    }

    try {
      await emitTo("main", "move-main-to-center", null)
      await emitTo("main", "navigate-to", `/MCP-list?keyword=${encodeURIComponent(tag)}`)
      await currentPopupWebviewWindow?.close()
    } catch (error) {
      console.error("[Popup] Error during confirm:", error)
    }
  }
</script>

<div class="popup-container">
  <div class="keyword-display">Selected Keyword: {tag}</div>
  <button class="confirm-button" on:click={handleConfirm}>Confirm Recommendation</button>
</div>

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
