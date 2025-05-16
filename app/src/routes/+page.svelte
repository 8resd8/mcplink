<script lang="ts">
  import { onMount } from "svelte"
  import { goto } from "$app/navigation"
  
  // Redirect to the Installed-MCP page as soon as the page loads
  onMount(() => {
    goto("/Installed-MCP", { replaceState: true })
  })
</script>

<div class="flex justify-center items-center h-64">
  <span class="loading loading-spinner loading-lg text-primary"></span>
  <p class="ml-4">Loading...</p>
</div>
