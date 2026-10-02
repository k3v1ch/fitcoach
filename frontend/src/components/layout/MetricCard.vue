<template>
  <div class="metric-card">
    <div class="metric-top">
      <div class="metric-info">
        <div class="metric-label">{{ label }}</div>
        <div class="metric-value">{{ value }}</div>
      </div>
      <div class="metric-icon" :style="{ background: color }">
        <BaseIcon :name="icon" :size="19" :color="iconColor" />
      </div>
    </div>
    <div class="metric-note">{{ note }}</div>
  </div>
</template>

<script setup>
import BaseIcon from '../ui/BaseIcon.vue'

defineProps({
  label: String,
  value: String,
  note: String,
  icon: String,
  color: String,
  iconColor: String
})
</script>

<style scoped>
.metric-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 12px;
}
.metric-top { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.metric-info { display: flex; flex-direction: column; gap: 4px; }
.metric-label { font-size: 13px; color: #6D7D79; font-weight: 500; }
.metric-value { font-size: 22px; font-weight: 700; color: #152421; }
.metric-icon {
  width: 40px; height: 40px; border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.metric-note { font-size: 12px; color: #6D7D79; font-weight: 500; }
</style>