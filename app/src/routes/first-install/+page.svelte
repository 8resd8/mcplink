<script lang="ts">
  // import { open } from '@tauri-apps/api/dialog'; // Tauri dialog API import
  // import { homeDir } from '@tauri-apps/api/path'; // Tauri path API for default path

  let selectedDirectory = "" // Keep for potential future use, but dialog logic is disabled
  let errorMessage = "" // For displaying errors

  /* // Temporarily disable directory selection logic
  async function handleSelectDirectory() {
    errorMessage = ""; // Reset error message
    try {
      const selectedPath = await open({
        directory: true,
        multiple: false,
        // defaultPath: await homeDir(), // Optional: set a default path
        title: "Select Configuration Directory",
      });

      if (typeof selectedPath === 'string') {
        selectedDirectory = selectedPath;
      } else if (selectedPath === null) {
        // User cancelled the dialog
        console.log("Directory selection cancelled by user.");
      } else {
        // This case should not happen with multiple: false
        console.warn("Unexpected selection result:", selectedPath);
      }
    } catch (err) {
      console.error("Error selecting directory:", err);
      errorMessage = "폴더를 선택하는 중 오류가 발생했습니다. 다시 시도해주세요.";
    }
  }
  */

  function handleComplete() {
    /* // Temporarily disable directory check for completion
    if (!selectedDirectory) {
      errorMessage = "먼저 설정 파일이 위치한 폴더를 선택해주세요.";
      return;
    }
    */
    errorMessage = ""
    // TODO: Implement completion logic (selectedDirectory might be empty or manually entered if re-enabled elsewhere)
    console.log("Complete clicked, directory (currently disabled for auto-selection):", selectedDirectory)
    // Potentially navigate to another page or save the configuration
  }
</script>

<div class="flex items-center justify-center min-h-screen bg-slate-100">
  <div class="bg-white p-8 rounded-lg shadow-xl w-full max-w-md">
    <h1 class="text-2xl font-bold text-center mb-6 text-gray-700">세팅 안내</h1>
    <p class="text-gray-600 mb-6 text-center">claude_desktop_config.json 파일의 위치를 설정해주세요. 이후 파일 위치 변경은 설정창에서 가능합니다</p>

    <div class="mb-6">
      <label for="directory-input" class="block text-sm font-medium text-gray-700 mb-1">설정 파일 위치 (현재 폴더 찾기 비활성화됨)</label>
      <div class="flex items-center">
        <input
          type="text"
          id="directory-input"
          bind:value={selectedDirectory}
          class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-l-md focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5"
          placeholder="디렉토리 경로를 입력하세요..."
          readonly={false}
        />
        <button class="bg-gray-400 text-white font-semibold p-2.5 rounded-r-md whitespace-nowrap cursor-not-allowed" disabled={true}>폴더 찾기</button>
      </div>
    </div>

    {#if errorMessage && errorMessage.includes("오류")}
      <p class="text-red-500 text-sm text-center mb-4">{errorMessage}</p>
    {/if}

    <button
      on:click={handleComplete}
      class="w-full bg-green-500 hover:bg-green-600 text-white font-bold py-3 px-4 rounded-md focus:outline-none focus:shadow-outline transition duration-150 ease-in-out"
    >
      완료
    </button>
  </div>
</div>

<style>
  /* You can add page-specific styles here if needed, though Tailwind handles most of it */
</style>
