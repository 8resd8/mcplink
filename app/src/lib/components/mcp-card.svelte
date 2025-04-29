<script lang="ts">
  // 카드 컴포넌트에 필요한 props 정의
  export let id: number;
  export let title: string;
  export let description: string;
</script>

<!-- card border를 사용하는 카드 형태 -->

<div class="card card-border bg-base-100 w-full h-full shadow-sm">
    <div class="card-body">
      <h2 class="card-title">{title}</h2>
      <p>{description}</p>
    </div>
  </div>