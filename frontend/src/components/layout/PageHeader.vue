<template>
  <header class="page-header">
    <div class="left">
      <button v-if="showBack" class="back-btn" @click="$emit('back')">
        <BaseIcon name="chevron-left" :size="16" color="#152421" />
      </button>
      <div class="title-block">
        <h1>{{ title }}</h1>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div class="actions">
      <div v-if="showSearch === true" class="search">
        <BaseIcon name="search" :size="16" />
        <input
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
          type="text"
          :placeholder="searchPlaceholder"
        />
      </div>
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
import BaseIcon from '../ui/BaseIcon.vue'

defineProps({
  title: String,
  subtitle: String,
  showBack: Boolean,
  showSearch: { type: Boolean, default: true },
  showBell: { type: Boolean, default: true },
  modelValue: { type: String, default: '' },
  searchPlaceholder: { type: String, default: 'Поиск...' }
})
defineEmits(['back', 'update:modelValue'])
</script>

<style scoped>
.page-header {
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

.left { display: flex; align-items: center; gap: 16px; }
.title-block h1 { font-size: 28px; font-weight: 700; color: #152421; }
.title-block p { font-size: 14px; color: #6D7D79; margin-top: 4px; }

.back-btn {
  width: 40px; height: 40px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 10px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}

.actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.search {
  display: flex; align-items: center; gap: 8px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  padding: 0 16px;
  height: 44px;
  width: 280px;
}
.search input {
  border: none; outline: none;
  font-size: 13px; width: 100%;
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
  position: absolute; top: -4px; right: -4px;
  background: #D64545; color: white;
  font-size: 10px; font-weight: 700;
  padding: 2px 6px; border-radius: 99px;
}

@media (max-width: 768px) {
  .search { width: 100%; }
  .actions { width: 100%; }
}
</style>