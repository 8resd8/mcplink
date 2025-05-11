<script lang="ts">
  import { WebviewWindow } from '@tauri-apps/api/webviewWindow';

  // 클릭 가능한 태그 목록
  const tags: string[] = [
    "AWS", "aws", "BRAVE", "brave", "SEARCH", "search", "EVERART", "everart",
    "EVERYTHING", "everything", "FETCH", "fetch", "FILESYSTEM", "filesystem",
    "FILE", "file", "SYSTEM", "system", "GIT", "git", "GITHUB", "github",
    "GITLAB", "gitlab", "DRIVE", "drive", "GOOGLE", "google", "MAP", "map",
    "MEMORY", "memory", "POSTGRES", "postgres", "PUPPETEER", "puppeteer",
    "REDIS", "redis", "SENTRY", "sentry", "SEQUENTIAL", "sequential",
    "THINKING", "thinking", "THINK", "think", "SLACK", "slack", "SQLITE",
    "sqlite", "TIME", "time"
  ];

  // 태그 클릭 시 새 창 생성
  async function onTagClick(tag: string) {
    const newWindowLabel = `notification-${tag}-${Date.now()}`; // 유니크한 레이블 생성
    const webview = new WebviewWindow(newWindowLabel, {
      url: `/popup?tag=${encodeURIComponent(tag)}`, // tag 정보를 query parameter로 전달
      title: '추천 알림',
      width: 300,
      height: 150,
      resizable: false,
      decorations: false,
      alwaysOnTop: true,
      skipTaskbar: true,
    });

    // webview.once('tauri://created', ...) 등은 popup 페이지 내부에서 처리

    await webview.once('tauri://error', (e) => {
      console.error('Failed to create new window:', e);
    });
  }
</script>

<style>
  .tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding: 20px;
    justify-content: center;
  }
  .tag-button {
    padding: 10px 15px;
    background-color: #4a5568;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s ease-in-out, transform 0.1s ease;
    font-weight: 500;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
  .tag-button:hover {
    background-color: #2d3748;
    transform: translateY(-1px);
  }
  .tag-button:active {
    background-color: #1a202c;
    transform: translateY(0px);
  }
</style>

<div class="container mx-auto p-4 text-center">
  <h1 class="text-2xl font-bold mb-6">키워드를 선택하세요</h1>
  <div class="tags-container">
    {#each tags as tag}
      <button class="tag-button" on:click={() => onTagClick(tag)}>
        {tag}
      </button>
    {/each}
  </div>
</div> 