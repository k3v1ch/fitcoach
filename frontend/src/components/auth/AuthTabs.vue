<template>
  <div class="auth-tabs">
    <button 
      v-for="tab in tabs" 
      :key="tab.id"
      type="button"
      class="tab-btn"
      :class="{ active: activeTab === tab.id }"
      @click="$emit('update:activeTab', tab.id)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  activeTab: String,
  tabs: {
    type: Array,
    default: () => [
      { id: 'login', label: 'Вход' },
      { id: 'register', label: 'Регистрация' },
      { id: 'recovery', label: 'Восстановление' }
    ]
  }
})
defineEmits(['update:activeTab'])
</script>

<style scoped>
.auth-tabs {
  display: flex;
  background: var(--color-gray-light);
  padding: 4px;
  border-radius: 10px;
  gap: 4px;
}
.tab-btn {
  flex: 1;
  padding: 10px;
  background: transparent;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 500;
  color: #888;
  transition: all 0.2s;
  cursor: pointer;
  border: none;
  font-family: inherit;
}
.tab-btn.active {
  background: var(--color-white);
  color: var(--color-dark);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
</style>