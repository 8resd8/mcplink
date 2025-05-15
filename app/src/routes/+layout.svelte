<script lang="ts">
  import "../app.css"
  import { onMount, onDestroy } from "svelte"
  import { browser } from "$app/environment"
  import type { Window as TauriWindowType } from "@tauri-apps/api/window"
  import { Presentation, Cog, Minus, X, Square, Settings } from "lucide-svelte"
  import { page } from "$app/stores"
  import { goto } from "$app/navigation"
  import { listen, type UnlistenFn } from "@tauri-apps/api/event"
  import { getCurrentWindow, UserAttentionType } from "@tauri-apps/api/window"
  import { platform as getOsPlatform } from "@tauri-apps/plugin-os"
  import { invoke } from "@tauri-apps/api/core"

  // --- Configuration for app appearance ---
  // Background class for the title bar and the tab bar area.
  const topAreaBackgroundClass = "bg-accent"
  // Text color for the top area (title bar and tabs).
  const topAreaContentColorClass = "text-accent-content"

  // --- Tab Definitions ---
  // mainThemeColorVar: CSS variable for the background of the main content area when this tab is active.
  // mainContentColorVar: CSS variable for the text/icon color within the main content area.
  const tabs = [
    { path: "/Installed-MCP", name: "Installed MCP", icon: Presentation, mainThemeColorVar: "--color-primary", mainContentColorVar: "--color-primary-content" },
    { path: "/MCP-list", name: "MCP List", icon: Cog, mainThemeColorVar: "--color-secondary", mainContentColorVar: "--color-secondary-content" },
    { path: "/test", name: "Test", icon: Cog, mainThemeColorVar: "--color-neutral", mainContentColorVar: "--color-neutral-content" },
  ]
  const settingsTab = { path: "/settings", name: "Settings", icon: Settings, mainThemeColorVar: "--color-base-100", mainContentColorVar: "--color-base-content" }

  // --- Tauri specific variables ---
  let tauriWindow: TauriWindowType | null = null
  let currentPlatform: string = "unknown"
  let unlistenMoveToCenter: UnlistenFn | undefined
  let unlistenNavigateTo: UnlistenFn | undefined

  // --- Svelte reactive state ---
  let activeTabPath = "/"
  $: isFirstInstallPage = $page?.url?.pathname === "/first-install"
  $: isPopupPage = $page?.url?.pathname === "/popup"

  // --- Lifecycle and Subscriptions ---
  onMount(async () => {
    activeTabPath = $page.url.pathname

    if (browser) {
      try {
        const osType: string = await getOsPlatform()
        currentPlatform = osType
        if (osType === "windows") {
          // ... (Your existing config file checking logic)
        }
      } catch (e) {
        console.error("[Layout] OS detection error:", e)
        currentPlatform = "unknown"
      }
    }

    if (typeof window !== "undefined" && "__TAURI__" in window) {
      try {
        tauriWindow = await getCurrentWindow()
        unlistenMoveToCenter = await listen("move-main-to-center", async () => {
          /* ... */
        })
        unlistenNavigateTo = await listen("navigate-to", async (event) => {
          if (event.payload && typeof event.payload === "string") goto(event.payload as string)
        })
      } catch (error) {
        console.error("[Layout] Error during Tauri initialization:", error)
      }
    }
  })

  page.subscribe((value) => {
    if (browser) activeTabPath = value.url.pathname
  })

  onDestroy(() => {
    unlistenMoveToCenter?.()
    unlistenNavigateTo?.()
  })

  // --- Window control functions ---
  async function minimizeWindow() {
    if (tauriWindow) await tauriWindow.minimize()
  }
  async function maximizeWindow() {
    if (tauriWindow) {
      ;(await tauriWindow.isMaximized()) ? tauriWindow.unmaximize() : tauriWindow.maximize()
    }
  }
  async function hideToTray() {
    if (tauriWindow) await tauriWindow.hide()
  }

  // --- Reactive computations for styling ---
  $: currentActivePageConfig = (() => {
    if (activeTabPath === settingsTab.path) return settingsTab
    const foundTab = tabs.find((t) => activeTabPath.startsWith(t.path))
    return foundTab || tabs.find((t) => t.path === "/Installed-MCP") || tabs[0] // Default
  })()

  // Main content area's background and text color, determined by the active tab.
  $: activeMainAreaBackgroundColor = `var(${currentActivePageConfig.mainThemeColorVar})`
  $: activeMainAreaContentColor = `var(${currentActivePageConfig.mainContentColorVar})`
</script>

<!-- Outermost container. If an overall app background different from main content is needed, apply it here. -->
<!-- For now, it's just a flex container. -->
<div class="flex flex-col h-screen overflow-hidden">
  <!-- Top Area: Title Bar and Tab Bar container, with 'accent' background -->
  {#if !isPopupPage}
    <div class="{topAreaBackgroundClass} {topAreaContentColorClass} fixed top-0 left-0 right-0 z-50">
      <!-- Title Bar -->
      <div class="h-8 flex items-center text-xs select-none" data-tauri-drag-region>
        <div class="p-2">
          <img src="/favicon.png" alt="App Icon" class="w-4 h-4" />
        </div>
        <div class="flex-1" data-tauri-drag-region>
          <slot name="title">MCPLINK</slot>
        </div>
        <div class="flex items-center">
          <button on:click={minimizeWindow} title="Minimize" class="p-2 hover:bg-black/5 rounded-sm"><Minus class="w-5 h-5" /></button>
          <button on:click={maximizeWindow} title="Maximize" class="p-2 hover:bg-black/5 rounded-sm"><Square class="w-4 h-4" /></button>
          <button on:click={hideToTray} title="Close to Tray" class="p-2 hover:bg-black/5 rounded-sm"><X class="w-5 h-5" /></button>
        </div>
      </div>

      <!-- Tab Bar (only if not the first install page) -->
      {#if !isFirstInstallPage}
        <div class="tab-bar px-2 flex w-full items-end" style="--Info">
          <div class="flex gap-1">
            {#each tabs as tab (tab.path)}
              <a
                href={tab.path}
                class="tab text-sm md:text-base px-3 py-2 md:px-4 rounded-t-md transition-colors duration-150 ease-in-out hover:bg-white/10"
                style="
                  border-bottom: 2px solid {activeTabPath.startsWith(tab.path) ? 'currentColor' : 'transparent'};
                  opacity: {activeTabPath.startsWith(tab.path) ? '1' : '0.7'};
                "
                on:click|preventDefault={() => goto(tab.path)}
              >
                <svelte:component this={tab.icon} class="w-4 h-4 mr-1 md:mr-2" />
                <span>{tab.name}</span>
              </a>
            {/each}
          </div>
          <div class="ml-auto">
            <!-- Settings Tab -->
            <a
              href={settingsTab.path}
              class="tab text-sm md:text-base px-3 py-2 md:px-4 rounded-t-md transition-colors duration-150 ease-in-out hover:bg-white/10"
              style="
                border-bottom: 2px solid {activeTabPath === settingsTab.path ? 'currentColor' : 'transparent'};
                opacity: {activeTabPath === settingsTab.path ? '1' : '0.7'};
              "
              on:click|preventDefault={() => goto(settingsTab.path)}
            >
              <svelte:component this={settingsTab.icon} class="w-4 h-4 mr-1 md:mr-2" />
              <span>{settingsTab.name}</span>
            </a>
          </div>
        </div>
      {/if}
    </div>
  {/if}

  <!-- Main Content Area -->
  <!-- This area's background and text color change based on the active tab. -->
  <!-- It's pushed down by the height of the title bar and tab bar. -->
  <main
    class="flex-1 overflow-auto p-4"
    style="
      background-color: {isFirstInstallPage || isPopupPage ? 'var(--color-base-100)' : activeMainAreaBackgroundColor};
      color: {isFirstInstallPage || isPopupPage ? 'var(--color-base-content)' : activeMainAreaContentColor};
      padding-top: {!isPopupPage && !isFirstInstallPage ? 'calc(2rem + 2.75rem + 1rem)' : !isPopupPage && isFirstInstallPage ? '2rem + 1rem' : '1rem'}; /* Titlebar + Tab bar + desired top padding */
    "
  >
    <slot />
  </main>
</div>

<style>
  .tab {
    font-weight: 500;
    /* Ensure consistent color from parent if not overridden by inline styles */
    color: inherit;
  }
  /* Add any other global styles or adjustments here */
</style>
