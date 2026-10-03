<template>
  <div class="confirm-page">
    <div class="confirm-card">
      <div class="brand">
        <BaseLogo />
      </div>

      <h2>Завершение регистрации</h2>
      <p>Заполните профиль и установите пароль</p>

      <form @submit.prevent="handleConfirm" class="confirm-form">
        <BaseInput id="cf-name" label="Имя и фамилия" v-model="form.fullName" placeholder="Иван Иванов" />
        <BaseInput id="cf-password" label="Пароль (не менее 15 символов)" type="password" v-model="form.password" placeholder="••••••••••" />

        <div class="field">
          <label class="field-label">Кто вы?</label>
          <select v-model="form.accountType" class="role-select">
            <option value="ATHLETE">Спортсмен</option>
            <option value="PARENT">Родитель</option>
            <option value="TRAINER">Тренер</option>
          </select>
        </div>

        <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>

        <BaseButton type="submit" :loading="isLoading">Подтвердить</BaseButton>
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
const form = reactive({ fullName: '', password: '', accountType: 'ATHLETE' })
const isLoading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  try {
    const saved = localStorage.getItem('fitcoach.registerAccountType')
    if (['ATHLETE', 'PARENT', 'TRAINER'].includes(saved)) form.accountType = saved
  } catch (_) { /* приватный режим */ }
  token.value = route.query.token || ''
  if (!token.value) {
    errorMessage.value = 'Нет токена регистрации'
  }
})

async function handleConfirm() {
  errorMessage.value = ''
  isLoading.value = true
  try {
    await authApi.registerConfirm(token.value, form.fullName, form.password, form.accountType)
    try {
      localStorage.removeItem('fitcoach.registerAccountType')
      // подсказка для экрана «Вы ещё не в организации»: тренеру — создать свою, остальным — ждать приглашения
      localStorage.setItem('fitcoach.accountType', form.accountType)
    } catch (_) { /* приватный режим */ }
    router.push('/?registered=1')
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
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.role-select {
  padding: 10px 12px; border: 1px solid #E3EAE8;
  border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none;
}
.error-box { padding: 10px 12px; background: #FCE2E5; color: #D64545; border-radius: 8px; font-size: 12px; }
</style>