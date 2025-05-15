<script lang="ts">
  import { invoke } from "@tauri-apps/api/core"
  
  let configPath = ""
  let isResetting = false

  // Function to reset settings
  async function resetSettings() {
    if (!confirm("Warning: This will reset all MCP server settings except for the fallback server. Continue?")) {
      return
    }

    isResetting = true
    try {
      await invoke("reset_mcp_settings")
      alert("Settings have been reset successfully. The application will now restart.")
      // Restart Claude Desktop
      await invoke("restart_claude_desktop")
    } catch (error) {
      console.error("Failed to reset settings:", error)
      alert(`Failed to reset settings: ${error}`)
    } finally {
      isResetting = false
    }
  }
</script>

<div class="p-8 max-w-2xl mx-auto">
  <h1 class="text-2xl font-bold mb-6">Settings</h1>
  
  <div class="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
    <h2 class="text-lg font-semibold mb-4">Reset Settings</h2>
    <p class="text-gray-600 mb-4">
      This will remove all MCP server configurations except for the fallback server.
      Use this option if you want to start fresh with your MCP configurations.
    </p>
    <button 
      class="btn btn-error" 
      on:click={resetSettings}
      disabled={isResetting}
    >
      {#if isResetting}
        <span class="loading loading-spinner loading-sm"></span> Resetting...
      {:else}
        Reset Settings
      {/if}
    </button>
  </div>
</div>
