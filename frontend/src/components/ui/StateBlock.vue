<template>
  <div class="state-block" :class="`state-block--${kind}`" :role="kind === 'error' ? 'alert' : 'status'">
    <BaseIcon :name="icon" :size="20" :color="color" />
    <p class="state-text">{{ text }}</p>
    <slot />
  </div>
</template>

<script setup>
// Единое состояние списка или блока: загрузка, пусто, ошибка.
// <StateBlock kind="loading" /> · <StateBlock kind="empty" message="Групп пока нет" /> · <StateBlock kind="error" :message="error" />
import { computed } from 'vue'
import BaseIcon from './BaseIcon.vue'

const props = defineProps({
  kind: { type: String, default: 'empty' }, // loading | empty | error
  message: { type: String, default: '' }
})

const DEFAULTS = {
  loading: { icon: 'clock', color: '#98A6A2', text: 'Загрузка…' },
  empty: { icon: 'file-text', color: '#98A6A2', text: 'Пока ничего нет' },
  error: { icon: 'alert', color: '#D64545', text: 'Не удалось загрузить данные' }
}

const icon = computed(() => (DEFAULTS[props.kind] || DEFAULTS.empty).icon)
const color = computed(() => (DEFAULTS[props.kind] || DEFAULTS.empty).color)
const text = computed(() => props.message || (DEFAULTS[props.kind] || DEFAULTS.empty).text)
</script>

<style scoped>
.state-block {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  padding: 32px 16px; text-align: center; color: #6D7D79; font-size: 13px;
}
.state-block--error { color: #D64545; }
.state-text { margin: 0; max-width: 420px; line-height: 1.5; }
</style>
