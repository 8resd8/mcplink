<script lang="ts">
  import { onMount } from "svelte"
  import { Star, Github, ArrowLeft } from "lucide-svelte"
  import { fetchMCPCardDetail } from "$lib/data/mcp-api"
  import { invoke } from "@tauri-apps/api/core"
  import { goto } from "$app/navigation"
  import { page } from "$app/stores"

  // Get MCP information from URL
  let id: number = 0
  let title: string = ""
  let description: string = ""
  let url: string = ""
  let stars: number = 0
  // Detailed settings binding
  let args: string[] = []
  let env: Record<string, any> = {}
  let command: string = ""
  let loading = true // Initial loading state true
  let error = ""
  let mode: "install" | "edit" = "install" // Add mode variable and set default value
  let referrer: string = "/" // Default referrer value is home

  // Format star count (display K if 1000 or more)
  function formatStars(count: number): string {
    if (count >= 1000) {
      return `${Math.round(count / 100) / 10}k`
    }
    return count.toString()
  }

  // Function to open GitHub link
  async function openGitHub(urlToOpen: string) {
    if (!urlToOpen) return

    // Add https:// if protocol is missing
    const finalUrl = urlToOpen.startsWith("http") ? urlToOpen : `https://${urlToOpen}`

    try {
      // Use browser's default window.open method
      window.open(finalUrl, "_blank")
    } catch (error) {
      console.error("Failed to open link:", error)
      // Notify user on failure
      alert(`Could not open the link. Please navigate manually: ${finalUrl}`)
    }
  }

  // Change goBack function to use goto
  function goBack() {
    console.log(`[Detail] goBack: Navigating to previous page. referrer: ${referrer}`)
    goto(referrer)
  }

  // Create star rating array
  let starsArray: number[] = []

  // Convert array or object to string (for placeholder)
  function formatValueForPlaceholder(value: any): string {
    if (Array.isArray(value)) {
      return value.join(", ")
    }
    if (typeof value === "object" && value !== null) {
      return Object.entries(value)
        .map(([k, v]) => `${k}: ${v}`)
        .join(", ")
    }
    return String(value || "")
  }

  // Load MCP detail data
  async function loadMCPDetail(mcpId: number) {
    if (!mcpId) {
      error = "ID is missing."
      loading = false
      return
    }

    loading = true
    error = ""

    try {
      const detail = await fetchMCPCardDetail(mcpId)
      console.log("Successfully loaded detail data:", detail)

      // Update basic information (optional, as already fetched from URL)
      title = detail.title || title
      description = detail.description || description
      url = detail.url || url
      stars = detail.stars || stars

      // Update detailed settings
      args = detail.args || []
      env = detail.env || {}
      command = detail.command || ""

      // Update star rating array
      const starCount = Math.min(Math.round(stars / 1000), 5)
      starsArray = Array(5)
        .fill(0)
        .map((_, i) => (i < starCount ? 1 : 0))
    } catch (err: any) {
      console.error("Failed to load detail data", err)
      error = `Failed to load detail data: ${err.message || err.toString()}`
      // Use example data (optional - for providing alternative UI on error)
      if (mcpId === 16) {
        // Sentry server example
        args = ["mcp-server-sentry", "--auth-token", "YOUR_SENTRY_TOKEN"]
        env = { SENTRY_DSN: "https://example.sentry.io/123456" }
        command = "uvx"
      }
    } finally {
      loading = false
    }
  }

  onMount(() => {
    // Get parameters from URL
    const params = new URLSearchParams(window.location.search)
    id = parseInt(params.get("id") || "0")

    // Read mode parameter
    const modeParam = params.get("mode")
    if (modeParam === "edit" || modeParam === "install") {
      mode = modeParam
    }

    // Read referrer parameter - save previous page information
    const refParam = params.get("referrer")
    if (refParam) {
      referrer = refParam
    } else {
      // Use document.referrer if available, otherwise set default based on mode
      referrer = mode === "edit" ? "/Installed-MCP" : "/MCP-list"
    }

    // Get basic information from URL parameters
    title = params.get("title") || "Untitled"
    description = params.get("description") || "No description"
    url = params.get("url") || ""
    stars = parseInt(params.get("stars") || "0")

    // Create star rating array (max 5)
    const starCount = Math.min(Math.round(stars / 1000), 5)
    starsArray = Array(5)
      .fill(0)
      .map((_, i) => (i < starCount ? 1 : 0))

    // If ID exists, load detail data
    if (id) {
      loadMCPDetail(id)
    } else {
      error = "Invalid access: ID is missing."
      loading = false
    }
  })
</script>

<div class="p-8 max-w-5xl mx-auto">
  <div class="bg-white rounded-lg shadow-sm p-6 relative">
    <!-- Back button -->
    <button class="absolute top-4 right-4 btn btn-sm btn-ghost gap-1" on:click={goBack}>
      <ArrowLeft size={18} />
      <span>Back</span>
    </button>

    <!-- Title and version -->
    <div class="mb-8 border-b pb-4">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-bold">{title}</h1>
        <div class="flex items-center gap-1">
          <Star class="text-yellow-400 fill-yellow-400" size={22} />
          <span class="text-gray-600 font-medium">{stars > 0 ? formatStars(stars) : "0"}</span>
        </div>
        {#if url && url.includes("github.com")}
          <button on:click={() => openGitHub(url)} class="text-gray-500 hover:text-gray-700 transition-colors" title="Open GitHub repository">
            <Github size={20} />
          </button>
        {/if}
      </div>
    </div>

    {#if loading}
      <div class="flex justify-center p-8">
        <div class="loading loading-spinner loading-lg"></div>
      </div>
    {:else if error}
      <div class="alert alert-error">
        <span>{error}</span>
        {#if id}
          <div class="mt-2">
            <button class="btn btn-sm btn-outline" on:click={() => loadMCPDetail(id)}>Retry</button>
          </div>
        {/if}
      </div>
    {:else}
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <!-- Left column: MCP basic information -->
        <div>
          <div class="mb-4"></div>

          <div>
            <h2 class="text-xl font-semibold mb-4">Description</h2>
            <div class="prose max-w-none">
              <p>{description}</p>
            </div>
          </div>
        </div>

        <!-- Right column: Required settings -->
        <div>
          <h2 class="text-xl font-semibold mb-4">Required Settings</h2>
          <div class="mb-6">
            {#if command}
              <div class="form-control mb-4">
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Command</span>
                  </label>
                  <input type="text" class="input input-bordered" value={command} />
                </div>
              </div>
            {/if}

            {#if args && args.length > 0}
              <div class="form-control mb-4">
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Arguments</span>
                  </label>
                  <!-- Use readonly textarea instead of password type -->
                  <textarea class="textarea textarea-bordered" rows="3">{formatValueForPlaceholder(args)}</textarea>
                </div>
              </div>
            {/if}

            {#if env && Object.keys(env).length > 0}
              <div class="form-control mb-4">
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Environment Variables</span>
                  </label>
                  <!-- Use readonly textarea instead of password type -->
                  <textarea class="textarea textarea-bordered" rows="3">{formatValueForPlaceholder(env)}</textarea>
                </div>
              </div>
            {/if}

            {#if !command && (!args || args.length === 0) && (!env || Object.keys(env).length === 0)}
              <p class="text-gray-500">No required settings.</p>
            {/if}
          </div>
        </div>
      </div>
    {/if}

    <!-- Bottom button area -->
    <div class="mt-8 flex justify-end gap-2 border-t pt-4">
      <button class="btn btn-sm btn-outline" on:click={goBack}>Cancel</button>
      <button
        class="btn btn-sm btn-primary"
        on:click={() => {
          if (mode === "edit") {
            alert("MCP has been modified!")
            // Add actual modification logic later
            goBack() // Automatically go back to previous page
          } else {
            // Configure MCP server settings
            const serverConfig = {
              command: command || "",
              args: args || [],
              env: env || {},
              cwd: null,
            }

            // Add server and restart
            invoke("add_mcp_server_config", {
              serverId: id,
              serverName: title,
              serverConfig: serverConfig,
            })
              .then(() => {
                // Restart Claude Desktop
                return invoke("restart_claude_desktop")
              })
              .then(() => {
                alert("MCP has been installed!")
                goBack() // Automatically go back to previous page
              })
              .catch((err) => {
                alert(`Error during MCP installation: ${err}`)
                console.error("MCP installation error:", err)
              })
          }
        }}
        disabled={loading || !!error}
      >
        {mode === "edit" ? "Save Changes" : "Install"}
      </button>
    </div>
  </div>
</div>
