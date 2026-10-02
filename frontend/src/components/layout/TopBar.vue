<template>
  <header class="top-bar">
    <div class="greeting">
      <h1>{{ title }}</h1>
      <p>{{ subtitle }}</p>
    </div>
    <div class="actions">
      <div class="search">
        <BaseIcon name="search" :size="16" />
        <input type="text" placeholder="Поиск по разделу... Ctrl+K" />
      </div>
      <BaseButton class="action-btn" @click="$emit('action')">
        <BaseIcon name="plus" :size="16" color="#102522" />
        {{ actionLabel }}
      </BaseButton>
    </div>
  </header>
</template>

<script setup>
import BaseButton from '../ui/BaseButton.vue'
import BaseIcon from '../ui/BaseIcon.vue'

defineProps({
  title: String,
  subtitle: String,
  actionLabel: String
})
defineEmits(['action'])
</script>

<style scoped>
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  position: sticky;
  top: 0;
  z-index: 10;
  background: #F4F7F8;
  padding-bottom: 16px;
}

.greeting h1 { font-size: 28px; font-weight: 700; color: #152421; }
.greeting p { font-size: 14px; color: #6D7D79; margin-top: 4px; }

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  padding: 0 16px;
  height: 44px;
  width: 280px;
}
.search input {
  border: none;
  outline: none;
  font-size: 13px;
  width: 100%;
  background: transparent;
}

.icon-btn {
  position: relative;
  width: 44px; height: 44px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.badge {
  position: absolute;
  top: -4px; right: -4px;
  background: #D64545;
  color: white;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 99px;
}

.action-btn {
  width: auto;
  padding: 0 16px;
  height: 44px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .search { width: 100%; }
  .actions { width: 100%; }
}
</style>