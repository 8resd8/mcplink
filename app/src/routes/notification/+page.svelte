<script lang="ts">
  import { onMount } from "svelte"
  import { isPermissionGranted, requestPermission, sendNotification } from "@tauri-apps/plugin-notification"

  let permissionGrantedGlobally = false

  onMount(async () => {
    // Check and request notification permission on page load
    permissionGrantedGlobally = await isPermissionGranted()
    if (!permissionGrantedGlobally) {
      const permission = await requestPermission()
      permissionGrantedGlobally = permission === "granted"
    }

    if (!permissionGrantedGlobally) {
      alert("Notification permission denied. Please allow notifications in app settings to receive them.")
    }
  })

  async function showNotification(buttonText: string) {
    if (!permissionGrantedGlobally) {
      alert("No notification permission. Please refresh the page or check settings.")
      return
    }

    try {
      await sendNotification({
        title: buttonText,
        body: `${buttonText} is awesome!`,
      })
    } catch (error) {}
  }

  const buttonLabels = ["GOOGLE", "FACEBOOK", "APPLE", "TAURI"]
</script>

<div class="container mx-auto p-4">
  <h1 class="text-2xl font-bold mb-4">Notification Test Page</h1>
  <p class="mb-4">Click a button to send a notification. Make sure notification permissions are granted.</p>

  {#if !permissionGrantedGlobally}
    <div class="p-4 mb-4 text-sm text-yellow-700 bg-yellow-100 rounded-lg" role="alert">
      <span class="font-medium">Warning!</span>
      Notification permission is not granted. Please allow notifications for this app.
    </div>
  {/if}

  <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
    {#each buttonLabels as label}
      <button class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded" on:click={() => showNotification(label)}>
        Send "{label}" Notification
      </button>
    {/each}
  </div>

  <div class="mt-8 p-4 border rounded-md bg-gray-50">
    <h2 class="text-lg font-semibold mb-2">How it works:</h2>
    <ul class="list-disc list-inside space-y-1">
      <li>
        When the page loads (
        <code class="bg-gray-200 px-1 rounded">onMount</code>
        ), it checks for notification permissions.
      </li>
      <li>If not granted, it requests permission from the user.</li>
      <li>
        Clicking a button calls the <code class="bg-gray-200 px-1 rounded">showNotification</code>
        function with the button's text.
      </li>
      <li>
        The function then uses Tauri's <code class="bg-gray-200 px-1 rounded">sendNotification</code>
        API.
      </li>
    </ul>
  </div>
</div>

<style>
</style>
