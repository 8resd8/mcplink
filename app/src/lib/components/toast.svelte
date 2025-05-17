<script lang="ts">
  import { onMount } from "svelte";
  import { fly } from "svelte/transition";
  import { CheckCircle, AlertCircle, XCircle, InfoIcon, X } from "lucide-svelte";
  
  // Props
  export let message: string = "";
  export let type: "success" | "error" | "warning" | "info" = "success";
  export let duration: number = 3000; // Duration in ms
  export let position: "top-right" | "top-left" | "bottom-right" | "bottom-left" | "top-center" | "bottom-center" = "top-right";
  export let show: boolean = false;
  
  // Auto-close functionality
  let timeoutId: ReturnType<typeof setTimeout>;
  
  // Auto-hide toast after duration
  $: if (show) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => {
      show = false;
    }, duration);
  }
  
  // For manual close
  function handleClose() {
    show = false;
    clearTimeout(timeoutId);
  }
  
  // Clean up on unmount
  onMount(() => {
    return () => {
      clearTimeout(timeoutId);
    };
  });
  
  // Type-specific properties
  $: typeProps = {
    success: {
      bgColor: "bg-green-50 dark:bg-green-900/20",
      borderColor: "border-l-4 border-green-500",
      textColor: "text-green-800 dark:text-green-200",
      icon: CheckCircle
    },
    error: {
      bgColor: "bg-red-50 dark:bg-red-900/20",
      borderColor: "border-l-4 border-red-500",
      textColor: "text-red-800 dark:text-red-200",
      icon: XCircle
    },
    warning: {
      bgColor: "bg-yellow-50 dark:bg-yellow-900/20",
      borderColor: "border-l-4 border-yellow-500",
      textColor: "text-yellow-800 dark:text-yellow-200",
      icon: AlertCircle
    },
    info: {
      bgColor: "bg-blue-50 dark:bg-blue-900/20", 
      borderColor: "border-l-4 border-blue-500",
      textColor: "text-blue-800 dark:text-blue-200",
      icon: InfoIcon
    }
  }[type];
  
  // Position class
  $: positionClass = {
    "top-right": "top-4 right-4",
    "top-left": "top-4 left-4",
    "bottom-right": "bottom-4 right-4",
    "bottom-left": "bottom-4 left-4",
    "top-center": "top-4 left-1/2 -translate-x-1/2",
    "bottom-center": "bottom-4 left-1/2 -translate-x-1/2"
  }[position];
</script>

{#if show}
  <div 
    class="fixed {positionClass} z-50 max-w-sm shadow-lg"
    transition:fly={{ y: position.startsWith("top") ? -20 : 20, duration: 200 }}
  >
    <div class={`rounded-md shadow-md ${typeProps.bgColor} ${typeProps.borderColor} p-4 flex items-start`}>
      <div class="flex-shrink-0 mr-3">
        <svelte:component this={typeProps.icon} class={`w-5 h-5 ${typeProps.textColor}`} />
      </div>
      <div class="flex-1">
        <p class={`text-sm ${typeProps.textColor}`}>{message}</p>
      </div>
      <button 
        class={`ml-4 flex-shrink-0 ${typeProps.textColor} focus:outline-none focus:ring-2 focus:ring-offset-2`}
        on:click={handleClose}
      >
        <X class="w-4 h-4" />
      </button>
    </div>
  </div>
{/if}