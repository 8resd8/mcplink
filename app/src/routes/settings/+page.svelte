<script lang="ts">
  import { onMount } from 'svelte';
  
  // OS 감지 변수
  let detectedOS = 'unknown';
  
  // 컴포넌트 마운트 시 실제 OS 감지
  onMount(() => {
    // 브라우저 환경에서 기본 감지
    if (typeof navigator !== 'undefined') {
      if (navigator.userAgent.indexOf('Win') !== -1) detectedOS = 'win32';
      else if (navigator.userAgent.indexOf('Mac') !== -1) detectedOS = 'darwin';
      else if (navigator.userAgent.indexOf('Linux') !== -1) detectedOS = 'linux';
    }
    
    // Tauri에서 실제 OS 감지 (가능한 경우)
    if (typeof window !== 'undefined' && ('__TAURI_INTERNALS__' in window || '__TAURI__' in window)) {
      import('@tauri-apps/plugin-os').then(({ platform }) => {
        (async () => {
          try {
            const os = await platform();
            detectedOS = os;
          } catch (error) {
            console.error(error);
          }
        })();
      }).catch(console.error);
    }
  });
</script>

<div class="p-6">
  <h2 class="text-2xl font-bold mb-6">설정</h2>
  
  <div class="mb-6">
    <button class="btn btn-neutral">Search Workspace</button>
    <button class="btn btn-error ml-2">제발 누르지마</button>
  </div>
  
  <div class="mb-6">
    <h3 class="text-xl font-semibold mb-3">시스템 정보</h3>
    <p class="mb-3">실제 OS: <span class="font-medium">{detectedOS === 'darwin' ? 'macOS' : detectedOS === 'win32' ? 'Windows' : detectedOS === 'linux' ? 'Linux' : detectedOS}</span></p>
  </div>
  
  <div class="mt-8">
    <h3 class="text-xl font-semibold mb-3">창 컨트롤 미리보기</h3>
    
    <div class="border rounded-lg p-4 mb-6">
      <div class="flex items-center">
        <h4 class="font-bold">macOS 스타일</h4>
        <div class="ml-3 flex items-center">
          <div class="w-3 h-3 rounded-full bg-red-500 mr-2"></div>
          <div class="w-3 h-3 rounded-full bg-yellow-500 mr-2"></div>
          <div class="w-3 h-3 rounded-full bg-green-500"></div>
        </div>
      </div>
    </div>
    
    <div class="border rounded-lg p-4">
      <div class="flex items-center justify-between">
        <h4 class="font-bold">Windows/Linux 스타일</h4>
        <div class="flex">
          <div class="mr-2 opacity-70 hover:opacity-100">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
          <div class="mr-2 opacity-70 hover:opacity-100">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect></svg>
          </div>
          <div class="opacity-70 hover:opacity-100 hover:text-red-500">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>