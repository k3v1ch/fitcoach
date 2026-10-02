<template>
  <Teleport to="body">
    <div v-if="modelValue" class="modal-backdrop" @click.self="close">
      <div class="modal-window" :style="{ width: width + 'px' }">
        <header class="modal-header">
          <h2>{{ title }}</h2>
          <button class="close-btn" @click="close">
            <BaseIcon name="close" :size="16" color="#98A6A2" />
          </button>
        </header>

        <div class="modal-content">
          <slot />
        </div>

        <footer class="modal-footer">
          <button class="cancel-btn" @click="close">Отмена</button>
          <button class="submit-btn" @click="$emit('submit')">{{ submitLabel }}</button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import BaseIcon from './BaseIcon.vue'

const props = defineProps({
  modelValue: Boolean,
  title: String,
  submitLabel: { type: String, default: 'Сохранить' },
  width: { type: Number, default: 500 }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const close = () => emit('update:modelValue', false)
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  padding: 20px;
}

.modal-window {
  background: white;
  border-radius: 20px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #152421;
}

.close-btn {
  width: 32px; height: 32px;
  background: #F4F7F8;
  border: none; border-radius: 16px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}

.modal-content {
  display: flex; flex-direction: column; gap: 16px;
}

.modal-footer {
  display: flex; justify-content: flex-end; gap: 12px;
}
.cancel-btn {
  padding: 12px 20px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 14px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.submit-btn {
  padding: 12px 20px; background: #B7F34B;
  border: none; border-radius: 12px;
  font-size: 14px; font-weight: 700; color: #102522;
  cursor: pointer;
}
</style>