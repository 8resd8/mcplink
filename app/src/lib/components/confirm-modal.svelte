<script lang="ts">
  import { createEventDispatcher, onMount } from "svelte"

  // Props
  export let isOpen: boolean = false
  export let title: string = "Confirm Action"
  export let message: string = "Are you sure you want to proceed?"
  export let okLabel: string = "Yes"
  export let cancelLabel: string = "No"
  export let type: "warning" | "info" | "error" | "success" = "warning"

  // Event dispatcher
  const dispatch = createEventDispatcher<{
    confirm: boolean
    cancel: void
  }>()

  // Confirm handler
  function handleConfirm() {
    dispatch("confirm", true)
    isOpen = false
  }

  // Cancel handler
  function handleCancel() {
    dispatch("cancel")
    isOpen = false
  }

  // Add/remove body class when modal opens/closes
  $: if (isOpen) {
    if (typeof document !== "undefined") {
      document.body.classList.add("modal-open")
    }
  } else {
    if (typeof document !== "undefined") {
      document.body.classList.remove("modal-open")
    }
  }

  // Clean up on component unmount
  onMount(() => {
    return () => {
      if (typeof document !== "undefined") {
        document.body.classList.remove("modal-open")
      }
    }
  })
</script>

{#if isOpen}
  <!-- Modal backdrop -->
  <div class="fixed inset-0 bg-black/50 z-[9999] flex items-center justify-center p-4 text-center">
    <!-- Modal container -->
    <div class="bg-white dark:bg-gray-800 max-w-md w-full rounded-lg shadow-lg p-6">
      <!-- Modal header -->
      <h3 class="font-bold text-lg text-center">{title}</h3>

      <!-- Modal content -->
      <div class="py-4">
        <div class="alert alert-{type} flex flex-col justify-center items-center text-center px-4 py-5">
          <p style="white-space: pre-line; text-align: center; width: 100%; font-size: 1rem;">{message}</p>
        </div>
      </div>

      <!-- Modal actions -->
      <div class="flex justify-center space-x-4 mt-4">
        <button class="btn btn-sm min-w-[80px] border-[1px] border-neutral-200" on:click={handleCancel}>
          {cancelLabel}
        </button>
        <button
          class="btn btn-sm {type === 'warning' && okLabel === 'Delete' ? 'btn-warning' : okLabel === 'Install' || okLabel === 'Update' ? 'btn-success' : 'btn-secondary'} min-w-[80px]"
          on:click={handleConfirm}
        >
          {okLabel}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  /* Prevent scrolling when modal is open */
  :global(body.modal-open) {
    overflow: hidden;
  }
</style>
