<template>
  <div class="confirm-page">
    <div class="confirm-card">
      <div class="brand">
        <BaseLogo />
      </div>

      <h2>Новый пароль</h2>
      <p>Придумайте новый пароль для входа в ФитКоуч</p>

      <form @submit.prevent="handleReset" class="confirm-form">
        <BaseInput id="rp-password" label="Новый пароль (не менее 15 символов)" type="password" v-model="form.password" placeholder="••••••••••" />
        <BaseInput id="rp-password2" label="Повторите пароль" type="password" v-model="form.passwordRepeat" placeholder="••••••••••" />

        <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>

        <BaseButton type="submit" :loading="isLoading">Сохранить пароль</BaseButton>
        <router-link to="/" class="back-link">← Вернуться ко входу</router-link>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseLogo from '../../components/ui/BaseLogo.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import { authApi } from '../../api/auth'

const route = useRoute()
const router = useRouter()
const token = ref('')
const form = reactive({ password: '', passwordRepeat: '' })
const isLoading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  token.value = route.query.token || ''
  if (!token.value) {
    errorMessage.value = 'Ссылка неполная: нет токена восстановления. Запросите письмо ещё раз.'
  }
})

async function handleReset() {
  errorMessage.value = ''
  if (form.password.length < 15) {
    errorMessage.value = 'Пароль должен быть не короче 15 символов'
    return
  }
  if (form.password !== form.passwordRepeat) {
    errorMessage.value = 'Пароли не совпадают'
    return
  }
  isLoading.value = true
  try {
    await authApi.confirmPasswordReset(token.value, form.password)
    router.push('/?reset=1')
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.confirm-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: #F4F7F8; padding: 20px;
}
.confirm-card {
  width: 100%; max-width: 420px; background: white;
  border-radius: 20px; padding: 32px; box-shadow: 0 8px 24px rgba(23,52,46,0.06);
}
.brand { display: flex; justify-content: center; margin-bottom: 24px; }
.confirm-card h2 { font-size: 22px; font-weight: 700; color: #152421; margin-bottom: 8px; text-align: center; }
.confirm-card p { font-size: 13px; color: #6D7D79; text-align: center; margin-bottom: 24px; }
.confirm-form { display: flex; flex-direction: column; gap: 16px; }
.error-box { padding: 10px 12px; background: #FCE2E5; color: #D64545; border-radius: 8px; font-size: 12px; }
.back-link { font-size: 13px; color: #6D7D79; text-align: center; text-decoration: none; }
.back-link:hover { color: #152421; }
</style>
