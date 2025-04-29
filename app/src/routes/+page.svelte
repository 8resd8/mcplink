<script lang="ts">
  import { onMount } from 'svelte';
  import Card from '../lib/components/card.svelte';
  import { getCards } from '../lib/data/cards';
  import MCPCard from '../lib/components/mcp-card.svelte';
  import { fetchMCPCards } from '../lib/data/mcp-api';
  
  // 백엔드로부터 받아올 데이터 타입 정의
  import type { Card as CardType } from '../lib/data/cards';
  import type { MCPCard as MCPCardType } from '../lib/data/mcp-api';
  
  // 일반 카드 데이터
  let cards: CardType[] = [];
  
  // MCP 카드 데이터
  let mcpCards: MCPCardType[] = [];
  
  // 데이터 로딩 상태
  let loading = true;
  let mcpLoading = true;
  
  // 선택된 카드 정보
  let selectedCard: CardType | null = null;
  
  // 카드 선택 핸들러
  function handleCardClick(card: CardType) {
    selectedCard = card;
  }
  
  // 컴포넌트 마운트 시 데이터 가져오기
  onMount(async () => {
    try {
      // 일반 카드 데이터 가져오기
      loading = true;
      cards = await getCards();
      loading = false;
      
      // MCP 카드 데이터 가져오기 (Tauri 백엔드를 통해)
      mcpLoading = true;
      mcpCards = await fetchMCPCards();
      mcpLoading = false;
    } catch (error) {
      console.error('error: get data', error);
      loading = false;
      mcpLoading = false;
    }
  });
</script>

<div class="p-8">
  
  <div class="flex flex-col gap-1.5">
    <!-- 상단 정사각형 카드 3개 -->
    <div class="p-2 rounded-lg">
      {#if loading}
        <div class="flex justify-center items-center h-64">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>
      {:else}
        <div class="grid grid-cols-1 sm:grid-cols-3 md:grid-cols-3 gap-4">
          <!-- 처음 3개의 카드만 보여줌 -->
          {#each cards.slice(0, 3) as card (card.id)}
            <div class="aspect-square">
              <Card 
                id={card.id}
                title={card.title}
                content={card.content}
              />
            </div>
          {/each}
        </div>
      {/if}
    </div>
    
    <!-- 하단 MCP 카드 컨테이너 -->
    <div class="p-2 rounded-lg">
      <p class="text-1xl font-bold">추천 MCP</p>
      
      {#if mcpLoading}
        <div class="flex justify-center items-center h-64">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>
      {:else}
        <div class="grid grid-cols-1 sm:grid-cols-1 gap-4">
          {#each mcpCards as card (card.id)}
            <MCPCard 
              id={card.id}
              title={card.title}
              description={card.description}
            />
          {/each}
        </div>
      {/if}
    </div>
  </div>
</div>
