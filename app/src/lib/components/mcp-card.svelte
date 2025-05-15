<script lang="ts">
  import { Star, Github } from "lucide-svelte"
  import { invoke } from "@tauri-apps/api/core"
  import { goto } from "$app/navigation"
  import { createEventDispatcher } from "svelte"

  // Create event dispatcher
  const dispatch = createEventDispatcher()

  // Define props needed for the card component
  export let id: number
  export let title: string
  export let description: string
  export let url: string = "" // GitHub URL
  export let stars: number = 0 // GitHub Stars count
  export let installed = false // Prop to indicate if the MCP server is installed
  export let onClick: (() => void) | undefined = undefined // Click handler (optional)
  export let className: string = "" // Additional CSS class (optional)
  export let variant: "default" | "primary" | "accent" = "default" // Card style variant
  export let maxDescLength: number = 150 // Maximum description length
  export let mode = "" // 'installed' or other. Indicates current page context.

  // Format star count (display K if 1000 or more)
  function formatStars(count: number): string {
    if (count >= 1000) {
      return `${Math.round(count / 100) / 10}k`
    }
    return count.toString()
  }

  // Truncate description text
  function truncateDescription(text: string, maxLength: number): string {
    if (text.length <= maxLength) return text
    return text.substring(0, maxLength) + "..."
  }

  // Final description to display
  const displayDescription = truncateDescription(description, maxDescLength)

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

  // Function to navigate to detail page
  function goToDetail() {
    const params = new URLSearchParams({
      id: id.toString(),
      title,
      description,
      url: url || "",
      stars: stars.toString(),
      mode: mode === "installed" ? "edit" : "install",
      referrer: window.location.pathname, // Save current path as referrer
    })


    window.location.href = `/detail?${params.toString()}`
  }

  interface MCPServerConfigTypeScript {
    command: string
    args?: string[] | null
    env?: Record<string, any> | null // Corresponds to serde_json::Map<String, Value>
  }

  async function handleComplete() {
    let errorMessage = ""
    try {


      // 1. Remove config from backend
      await invoke("remove_mcp_server_config", {
        serverName: title,
      })

      // 2. Dispatch 'deleted' event (request GUI update)

      dispatch("deleted", { id: id })

      // 3. Restart Claude Desktop (after event dispatch)
      await invoke("restart_claude_desktop")
    } catch (err: any) {
      // Specify err type as any or Error
      errorMessage = `error occurred: ${err.message || err}`
      console.error(`[mcp-card] Error during removal or restart: ${errorMessage}`)
      alert(`An error occurred: ${errorMessage}`)
    }
  }
</script>

<!-- Reusable MCP card component -->
{#if onClick}
  <button
    class="card card-border w-full shadow-sm {variant === 'default'
      ? 'bg-base-100'
      : variant === 'primary'
        ? 'bg-primary text-primary-content'
        : 'bg-accent text-accent-content'} {className} text-left"
    on:click={onClick}
  >
    <div class="card-body h-[160px] flex flex-col">
      <div class="flex justify-between items-start">
        <h2 class="card-title text-lg">{title}</h2>
        <div class="flex items-center gap-1 flex-shrink-0">
          <!-- Star icon and count -->
          {#if stars > 0}
            <Star class="text-yellow-400" size={18} />
            <span>{formatStars(stars)}</span>
          {/if}

          <!-- GitHub link -->
          {#if url}
            <!-- If URL exists: clickable link -->
            <span on:click|stopPropagation={() => openGitHub(url)} class="ml-2 text-gray-600 hover:text-black focus:outline-none cursor-pointer">
              <Github size={18} />
            </span>
          {:else}
            <!-- If URL does not exist: display as disabled -->
            <span class="ml-2 text-gray-400 opacity-40">
              <Github size={18} />
            </span>
          {/if}
        </div>
      </div>
      <div class="w-full">
        <p class="text-sm overflow-hidden line-clamp-2 max-w-[85%]">{displayDescription}</p>
      </div>
      <div class="card-actions justify-end mt-1">
        <span on:click|stopPropagation={goToDetail} class="btn btn-sm btn-primary cursor-pointer">Details</span>
      </div>
    </div>
  </button>
{:else}
  <div class="card card-border w-full shadow-sm {variant === 'default' ? 'bg-base-100' : variant === 'primary' ? 'bg-primary text-primary-content' : 'bg-accent text-accent-content'} {className}">
    <div class="card-body h-[160px] flex flex-col">
      <div class="flex justify-between items-start">
        <h2 class="card-title text-lg">{title}</h2>
        <div class="flex items-center gap-1 flex-shrink-0">
          <!-- Star icon and count -->
          {#if stars > 0}
            <Star class="text-yellow-400" size={18} />
            <span>{formatStars(stars)}</span>
          {/if}

          <!-- GitHub link -->
          {#if url}
            <!-- If URL exists: clickable link -->
            <button on:click={() => openGitHub(url)} class="ml-2 text-gray-600 hover:text-black focus:outline-none">
              <Github size={18} />
            </button>
          {:else}
            <!-- If URL does not exist: display as disabled -->
            <span class="ml-2 text-gray-400 opacity-40">
              <Github size={18} />
            </span>
          {/if}
        </div>
      </div>
      <div class="w-full">
        <p class="text-sm overflow-hidden line-clamp-2 max-w-[85%]">{displayDescription}</p>
      </div>
      <div class="card-actions justify-end mt-1">
        {#if mode === "installed"}
          <button on:click={goToDetail} class="btn btn-sm btn-primary">Edit</button>
          <button on:click={handleComplete} class="btn btn-sm btn-primary">Delete</button>
        {:else if installed}
          <button on:click={goToDetail} class="btn btn-sm btn-primary">Installed</button>
        {:else}
          <button on:click={goToDetail} class="btn btn-sm btn-primary">Install</button>
        {/if}
      </div>
    </div>
  </div>
{/if}
