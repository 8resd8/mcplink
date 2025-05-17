import { writable } from 'svelte/store';

type ToastType = 'success' | 'error' | 'warning' | 'info';
type ToastPosition = 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' | 'top-center' | 'bottom-center';

interface ToastState {
  show: boolean;
  message: string;
  type: ToastType;
  duration: number;
  position: ToastPosition;
}

// Initialize default state
const defaultState: ToastState = {
  show: false,
  message: '',
  type: 'success',
  duration: 3000,
  position: 'top-right'
};

// Create writable store
const toastStore = writable<ToastState>(defaultState);

// Helper functions to show different types of toasts
export function showToast(
  message: string,
  type: ToastType = 'success',
  duration: number = 3000,
  position: ToastPosition = 'top-right'
) {
  toastStore.update(state => ({
    ...state,
    show: true,
    message,
    type,
    duration,
    position
  }));
}

export function hideToast() {
  toastStore.update(state => ({
    ...state,
    show: false
  }));
}

// Convenience functions
export function showSuccess(message: string, duration?: number, position?: ToastPosition) {
  showToast(message, 'success', duration, position);
}

export function showError(message: string, duration?: number, position?: ToastPosition) {
  showToast(message, 'error', duration, position);
}

export function showWarning(message: string, duration?: number, position?: ToastPosition) {
  showToast(message, 'warning', duration, position);
}

export function showInfo(message: string, duration?: number, position?: ToastPosition) {
  showToast(message, 'info', duration, position);
}

export default toastStore;