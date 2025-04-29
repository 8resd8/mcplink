<script lang="ts">
  // 카드 컴포넌트에 필요한 props 정의
  export let id: number;
  export let title: string;
  export let content: string;
</script>

<!-- card border를 사용하는 카드 형태 -->

<div class="card card-border bg-base-100 w-full h-full shadow-sm">
    <div class="card-body">
      <h2 class="card-title">{title}</h2>
      <p>{content}</p>
    </div>
  </div>

  <!-- 일반 카드 -->
  <!-- <div class="card w-96 bg-base-100 card-xs shadow-sm">
  <div class="card-body">
    <h2 class="card-title">Xsmall Card</h2>
    <p>A card component has a figure, a body part, and inside body there are title and actions parts</p>
    <div class="justify-end card-actions">
      <button class="btn btn-primary">Buy Now</button>
    </div>
  </div>
</div> -->