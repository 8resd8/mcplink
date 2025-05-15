<script lang="ts">
  // import { WebviewWindow } from '@tauri-apps/api/webviewWindow'; // No longer used, so commented out or remove
  import { invoke } from "@tauri-apps/api/core"
  import { onMount, onDestroy } from "svelte"
  import { isPermissionGranted, requestPermission, sendNotification } from "@tauri-apps/plugin-notification"
  import { getCurrentWindow } from "@tauri-apps/api/window"
  import { emit, listen } from "@tauri-apps/api/event"

  // Clickable tag list
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

  // --- CLAUDE CODE MODIFICATION START ---
  // Trigger system notification on button click - Create notification directly from frontend
  async function handleButtonClick(tag: string) {
    // Check and request notification permission
    let permissionGranted = await isPermissionGranted()
    if (!permissionGranted) {
      const permission = await requestPermission()
      permissionGranted = permission === "granted"
    }

    if (permissionGranted) {
      // Create notification directly from frontend
      await sendNotification({
        title: "Check Recommendation", // English title
        body: `Selected keyword: ${tag}. Click to check.`, // English body
        icon: "icons/icon.png",
      })

      // Add simulation button for notification click (for development)
      const simulateClick = confirm(`[Development Mode] Simulate notification click for "${tag}"? (For notification click test)`) // English confirm message
      if (simulateClick) {
        // Activate main window
        await activateMainWindow()

        // Navigate to MCP-list page and search with the tag
        const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`

        // Emit navigate-to event
        emit("navigate-to", targetUrl)

        // Emit MCP-list search event
        emit("navigate-to-mcp-list-with-keyword", targetUrl)
      }

      // Automatic simulation after notification click - Activate automatically if actual notification doesn't work
      // Automatically simulate notification click 3 seconds after sending notification
      setTimeout(async () => {
        await activateMainWindow()
        const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`
        emit("navigate-to", targetUrl)
        emit("navigate-to-mcp-list-with-keyword", targetUrl)
      }, 3000)
    } else {
      alert("Notification permission is required. Please check your system settings.") // English alert
    }
  }
  // --- CLAUDE CODE MODIFICATION END ---

  // --- CLAUDE CODE MODIFICATION START ---
  // Create notification using notification button format (utilizing backend)
  async function handleBackendNotification(tag: string) {
    // Call backend function to display notification
    await invoke("show_popup", { tag })

    // Simulate notification click after backend notification display
    const simulateClick = confirm(`[Development Mode] Simulate notification click for "${tag}" for backend notification? (For notification click test)`) // English confirm message
    if (simulateClick) {
      // Activate main window
      await activateMainWindow()

      // Navigate to MCP-list page and search with the tag
      const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`

      // Emit navigate-to event
      emit("navigate-to", targetUrl)

      // Emit MCP-list search event
      emit("navigate-to-mcp-list-with-keyword", targetUrl)
    }

    // Automatic simulation for backend notification - Activate automatically if actual notification doesn't work
    setTimeout(async () => {
      await activateMainWindow()
      const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`
      emit("navigate-to", targetUrl)
      emit("navigate-to-mcp-list-with-keyword", targetUrl)
    }, 3000)
  }
  // --- CLAUDE CODE MODIFICATION END ---

  // Listener for notification click event detection
  let unlistenNotificationAction: (() => void) | undefined = undefined

  onMount(async () => {
    // Check and request notification permission
    let permissionGranted = await isPermissionGranted()
    if (!permissionGranted) {
      const permission = await requestPermission()
      permissionGranted = permission === "granted"
    }

    if (permissionGranted) {
      // Register notification click event listener - Apply Tauri v2 method
      try {
        // Try various events to detect notification click
        const events = ["tauri://notification-clicked", "tauri://notification-action", "notification://action"]

        // Attempt to register listener for all possible events
        for (const eventName of events) {
          try {
            const unlisten = await listen(eventName, async (event) => {
              // 1. Activate main window
              await activateMainWindow()

              // 2. Extract tag information from event payload
              let tag = null
              const payload = event.payload

              if (payload) {
                try {
                  // Handle various payload structures
                  if (typeof payload === "object") {
                    // 1. Attempt to extract from body property
                    if ("body" in payload) {
                      const text = payload.body as string
                      const match = text.match(/Selected keyword: (.*?)\. Click to check./) // Match English text
                      if (match && match[1]) tag = match[1]
                    }

                    // 2. Attempt to extract from notification property
                    if (!tag && "notification" in payload && typeof payload.notification === "object" && payload.notification !== null) {
                      const notification = payload.notification
                      if ("body" in notification) {
                        const text = notification.body as string
                        const match = text.match(/Selected keyword: (.*?)\. Click to check./) // Match English text
                        if (match && match[1]) tag = match[1]
                      }
                    }

                    // 3. Find direct tag property
                    if (!tag && "tag" in payload) {
                      tag = payload.tag as string
                    }
                  }

                  // Attempt direct extraction if string
                  if (!tag && typeof payload === "string") {
                    const match = payload.match(/Selected keyword: (.*?)\. Click to check./) // Match English text
                    if (match && match[1]) tag = match[1]
                  }
                } catch (extractError) {}
              }

              // If extracted tag exists, navigate to MCP-list page
              if (tag) {
                // Navigate to MCP-list page and pass tag
                const targetUrl = `/MCP-list?keyword=${encodeURIComponent(tag)}`

                // Emit page navigation and search events
                await emit("navigate-to", targetUrl)
                await emit("navigate-to-mcp-list-with-keyword", targetUrl)
              } else {
              }
            })

            // Store only the first listener (for memory management)
            if (!unlistenNotificationAction) {
              unlistenNotificationAction = unlisten
            }
          } catch (listenerError) {}
        }
      } catch (error) {}
    } else {
    }
  })

  // Window activation function
  async function activateMainWindow() {
    try {
      const mainWindow = getCurrentWindow()
      await mainWindow.show()
      await mainWindow.unminimize()
      await mainWindow.setFocus()
    } catch (error) {}
  }

  onDestroy(() => {
    // Unregister listener on component destruction
    if (unlistenNotificationAction) {
      unlistenNotificationAction()
    }
  })
</script>

<div class="container mx-auto p-4 text-center">
  <h1 class="text-2xl font-bold mb-6">System Notification Test (Frontend)</h1>
  <!-- English heading -->
  <p class="mb-4">Clicking the keywords below will generate a system notification from the frontend:</p>
  <!-- English paragraph -->
  <div class="tags-container">
    {#each tags as tag}
      <button class="tag-button" on:click={() => handleButtonClick(tag)}>
        {tag}
      </button>
    {/each}
  </div>
</div>

<div class="container p-4">
  <h1 class="text-2xl font-bold mb-4">System Notification Test (Backend)</h1>
  <!-- English heading -->
  <p class="mb-4">Clicking the buttons below will display a system notification from the backend:</p>
  <!-- English paragraph -->
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
    background-color: #4a5568; /* Tailwind gray-700 */
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
    background-color: #2d3748; /* Tailwind gray-800 */
    transform: translateY(-1px);
  }
  .tag-button:active {
    background-color: #1a202c; /* Tailwind gray-900 */
    transform: translateY(0px);
  }
</style>
