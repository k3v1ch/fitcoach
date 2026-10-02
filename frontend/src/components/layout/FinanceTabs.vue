<template>
  <div class="finance-tabs">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      class="tab-item"
      active-class="active"
    >
      {{ tab.label }}
    </router-link>
  </div>
</template>

<script setup>
const tabs = [
  { label: 'Dashboard', path: '/trainer/finance' },
  { label: 'Начисления', path: '/trainer/finance/charges' },
  { label: 'Оплаты', path: '/trainer/finance/payments' },
  { label: 'Целевые сборы', path: '/trainer/finance/targets' },
  { label: 'Расходы', path: '/trainer/finance/expenses' },
  { label: 'Реестры', path: '/trainer/finance/registries' }
]
</script>

<style scoped>
.finance-tabs {
  display: flex; gap: 8px;
  border-bottom: 1px solid #E3EAE8;
  padding-bottom: 8px;
  flex-wrap: wrap;
}
.tab-item {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #6D7D79;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}
.tab-item:hover { color: #152421; }
.tab-item.active {
  color: #152421;
  font-weight: 700;
  border-bottom-color: #102522;
}
</style>