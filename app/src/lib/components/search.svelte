<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  
  // 이벤트 디스패처 생성
  const dispatch = createEventDispatcher<{
    search: { value: string }
  }>();
  
  // 검색어
  let searchValue = '';
  
  // 검색 이벤트 핸들러
  function handleSearch() {
    dispatch('search', { value: searchValue });
  }
  
  // 키보드 이벤트 핸들러 (Enter 키 누르면 검색)
  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      handleSearch();
    }
  }
</script>

<label class="input input-bordered rounded-[10px]">

  <input 
    type="search" 
    class="grow" 
    placeholder="MCP name or keyword search..." 
    bind:value={searchValue} 
    on:input={handleSearch}
    on:keydown={handleKeyDown}
  />
</label>

  