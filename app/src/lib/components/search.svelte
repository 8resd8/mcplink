<script lang="ts">
  import { createEventDispatcher, onMount } from 'svelte';
  
  // Create event dispatcher
  const dispatch = createEventDispatcher<{
    search: { value: string }
  }>();
  
  // START: initialValue prop 추가
  export let initialValue: string = '';
  // END: initialValue prop 추가
  
  // Search value
  let searchValue = '';
  
  // 디바운스 타이머
  let debounceTimer: ReturnType<typeof setTimeout> | null = null;
  
  // 디바운스 지연 시간 (밀리초)
  const DEBOUNCE_DELAY = 300;
  
  // START: onMount에서 initialValue로 searchValue 설정
  onMount(() => {
    if (initialValue) {
      searchValue = initialValue;
    }
  });
  // END: onMount에서 initialValue로 searchValue 설정
  
  // 디바운스된 검색 함수
  function debouncedSearch() {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      dispatch('search', { value: searchValue });
    }, DEBOUNCE_DELAY);
  }
  
  // Search event handler
  function handleSearch() {
    dispatch('search', { value: searchValue });
  }
  
  // Keyboard event handler (Enter key pressed to search)
  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      if (debounceTimer) clearTimeout(debounceTimer);
      handleSearch();
    }
  }
</script>

<label class="input input-bordered rounded-[10px] flex items-center">
  <input 
    type="search" 
    class="grow" 
    placeholder="MCP name or keyword search..." 
    bind:value={searchValue} 
    on:input={debouncedSearch} 
    on:keydown={handleKeyDown}
  />
</label>

<style>
  label {
    width: 100%; /* 필요하다면 부모 요소에서 너비 제어 */
  }
</style>

  