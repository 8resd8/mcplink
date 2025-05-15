// Tauri doesn't have a Node.js server to do proper SSR
// See: https://v2.tauri.app/start/frontend/sveltekit/ for more info
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte"
import adapter from "@sveltejs/adapter-static"
import { dirname, join } from "path"
import { fileURLToPath } from "url"

const __dirname = dirname(fileURLToPath(import.meta.url))

/** @type {import('@sveltejs/kit').Config} */
const config = {
  preprocess: vitePreprocess(),

  kit: {
    adapter: adapter(),
    alias: {
      $app: join(__dirname, ".svelte-kit/runtime/app"),
    },
  },
}

export default config
