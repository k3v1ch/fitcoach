<template>
  <button class="base-button" :class="{ 'is-loading': loading }" :disabled="disabled || loading">
    <span v-if="!loading" class="button-content">
      <slot />
    </span>
    <span v-else class="loader"></span>
  </button>
</template>

<script setup>
defineProps({
  loading: Boolean,
  disabled: Boolean
})
</script>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 44px;
  padding: 0 16px;
  background: var(--color-primary, #C4F000);
  border-radius: 12px;
  color: var(--color-dark, #102522);
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}

.base-button:hover { opacity: 0.9; }
.base-button:disabled { opacity: 0.5; cursor: not-allowed; }

.button-content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.loader {
  width: 20px; height: 20px;
  border: 2px solid currentColor;
  border-bottom-color: transparent;
  border-radius: 50%;
  animation: rotation 1s linear infinite;
}

@keyframes rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>