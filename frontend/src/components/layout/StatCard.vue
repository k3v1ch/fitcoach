<template>
  <div class="stat-card">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">{{ value }}</div>
    <div v-if="subtext" class="stat-subtext">
      <span v-if="trend" :class="trendClass">{{ trend }}</span>
      {{ subtext }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  value: String,
  subtext: String,
  trend: String,
  trendUp: {
    type: Boolean,
    default: true
  }
})

const trendClass = computed(() => props.trendUp ? 'trend-up' : 'trend-down')
</script>

<style scoped>
.stat-card {
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.stat-label {
  font-size: 12px;
  font-weight: 700;
  color: #6D7D79;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #152421;
}

.stat-subtext {
  font-size: 12px;
  color: #6D7D79;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-up {
  color: #2E8B57;
  font-weight: 600;
}

.trend-down {
  color: #D64545;
  font-weight: 600;
}
</style>