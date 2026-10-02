<template>
  <div class="input-group">
    <label v-if="label" :for="id" class="input-label">{{ label }}</label>
    <input 
      :id="id"
      :type="type" 
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      :placeholder="placeholder"
      class="input-field"
    />
  </div>
</template>

<script setup>
defineProps({
  label: String,
  id: String,
  type: {
    type: String,
    default: 'text'
  },
  placeholder: String,
  modelValue: String
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.input-label {
  font-size: 12px;
  color: var(--color-gray-text);
  font-weight: 500;
}

.input-field {
  width: 100%;
  min-height: 40px;
  padding: 8px 12px;
  border: 1px solid var(--color-gray-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--color-dark);
  outline: none;
  transition: border-color 0.2s;
  background: var(--color-white);
}

.input-field:focus {
  border-color: var(--color-primary);
}

.input-field::placeholder {
  color: #aaa;
}
</style>