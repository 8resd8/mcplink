// Tauri doesn't have a Node.js server to do proper SSR
// so we will use adapter-static to prerender the app (SSG)
// See: https://v2.tauri.app/start/frontend/sveltekit/ for more info
import { browser } from "$app/environment" // Check if in browser environment
import { redirect } from "@sveltejs/kit" // Import redirect function
import type { LayoutLoad } from "./$types" // Import LayoutLoad type

// Keep existing settings
export const prerender = true
export const ssr = false
export const csr = true

const CONFIG_FILE_NAME = "claude_desktop_config.json" // Config file name to check
const FIRST_INSTALL_PATH = "/first-install"
const DEFAULT_PATH_AFTER_INSTALL = "/Installed-MCP" // Default path after setup

/** @type {import('./$types').LayoutLoad} */
export const load: LayoutLoad = async ({ url }) => {

  return {}
}
