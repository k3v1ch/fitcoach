<template>
  <div v-if="organizations.length > 1" class="org-switch">
    <label class="org-label" :for="id">ОРГАНИЗАЦИЯ</label>
    <select :id="id" class="org-select" :value="currentOrganization?.organizationId" @change="switchOrganization($event.target.value)">
      <option v-for="o in organizations" :key="o.organizationId" :value="o.organizationId">{{ o.organizationName }}</option>
    </select>
  </div>
</template>

<script setup>
// Показывается, только если пользователь состоит в нескольких организациях
import { currentOrganization, organizations, switchOrganization } from '../../utils/session'

const id = `org-select-${Math.random().toString(36).slice(2, 8)}`
</script>

<style scoped>
.org-switch { margin: 0 0 16px; display: flex; flex-direction: column; gap: 6px; }
.org-label { font-size: 10px; font-weight: 700; letter-spacing: 0.08em; color: #98A6A2; }
.org-select {
  width: 100%; padding: 8px 10px; border-radius: 10px; border: 1px solid rgba(255, 255, 255, 0.12);
  background: #19332F; color: white; font-size: 13px; cursor: pointer;
}
.org-select:focus { outline: 2px solid #C4F000; outline-offset: 1px; }
</style>
