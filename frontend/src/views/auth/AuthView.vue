<template>
  <div class="auth-layout">
    <aside class="auth-cover">
      <div class="cover-bg"></div>
      <div class="cover-content">
        <BaseLogo class="brand-logo" dark />
        <div class="cover-text">
          <h1>Секции под контролем.<br>Прогресс — на виду.</h1>
          <p>Расписание, посещаемость, финансы и общение с родителями в одной системе.</p>
        </div>
        <div class="cover-footer">© 2026 ФитКоуч · Защита данных</div>
      </div>
    </aside>

    <main class="auth-form-container">
      <div class="mobile-logo">
        <BaseLogo />
      </div>

      <div class="auth-card">
        <AuthTabs v-model:activeTab="currentTab" />

        <div class="card-header">
          <h2>{{ currentTitle }}</h2>
          <p>{{ currentDescription }}</p>
        </div>

        <!-- ВХОД -->
        <form v-if="currentTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
          <BaseInput id="email" label="Электронная почта" type="email" v-model="form.email" placeholder="alexey@fitcoach.ru" />
          <BaseInput id="password" label="Пароль" type="password" v-model="form.password" placeholder="••••••••••" />

          <div class="form-options">
            <span></span>
            <button type="button" class="link-btn" @click="goToRecovery">Забыли пароль?</button>
          </div>

          <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>
          <div v-if="infoMessage" class="success-box">{{ infoMessage }}</div>

          <BaseButton type="submit" :loading="isLoading">Войти</BaseButton>
          <p class="help-text">Нужна помощь? support@fitcoach.ru</p>
        </form>

        <!-- РЕГИСТРАЦИЯ -->
        <form v-if="currentTab === 'register'" @submit.prevent="handleRegisterRequest" class="auth-form">
          <BaseInput id="reg-email" label="Электронная почта" type="email" v-model="form.email" placeholder="alexey@fitcoach.ru" />

          <div class="field">
            <label class="field-label">Кто вы?</label>
            <select v-model="form.accountType" class="role-select">
              <option value="ATHLETE">Спортсмен</option>
              <option value="PARENT">Родитель</option>
              <option value="TRAINER">Тренер</option>
            </select>
          </div>

          <div v-if="successMessage" class="success-box">{{ successMessage }}</div>
          <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>

          <BaseButton type="submit" :loading="isLoading">Получить ссылку на регистрацию</BaseButton>
          <p class="help-text">Создавая аккаунт, вы принимаете условия использования.</p>
        </form>

        <!-- ВОССТАНОВЛЕНИЕ -->
        <form v-if="currentTab === 'recovery'" @submit.prevent="handleRecovery" class="auth-form">
          <BaseInput id="rec-email" label="Электронная почта" type="email" v-model="form.email" placeholder="coach@fitclub.ru" />

          <div v-if="successMessage" class="success-box">{{ successMessage }}</div>
          <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>

          <BaseButton type="submit" :loading="isLoading">Отправить ссылку</BaseButton>
          <button type="button" class="back-link" @click="currentTab = 'login'">← Вернуться ко входу</button>
        </form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BaseLogo from '../../components/ui/BaseLogo.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import AuthTabs from '../../components/auth/AuthTabs.vue'
import { authApi } from '../../api/auth'
import { loadSession, homePath } from '../../utils/session'

const router = useRouter()

const currentTab = ref('login')
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const infoMessage = ref('')

const form = reactive({
  email: '',
  password: '',
  fullName: '',
  accountType: 'ATHLETE'
})

const currentTitle = computed(() => ({
  login: 'С возвращением',
  register: 'Запрос на регистрацию',
  recovery: 'Восстановление доступа'
}[currentTab.value] || ''))

const currentDescription = computed(() => ({
  login: 'Войдите, чтобы продолжить работу с секциями.',
  register: 'Укажите email — отправим ссылку на завершение регистрации.',
  recovery: 'Укажите рабочую почту — отправим ссылку для создания нового пароля.'
}[currentTab.value] || ''))

function resetMessages() {
  errorMessage.value = ''
  successMessage.value = ''
  infoMessage.value = ''
}

function goToRecovery() {
  resetMessages()
  currentTab.value = 'recovery'
}

async function handleLogin() {
  resetMessages()
  isLoading.value = true
  try {
    await authApi.login(form.email, form.password)
    await loadSession()
    // next — страница, с которой отправили на вход; только локальные пути
    const next = router.currentRoute.value.query.next
    const safeNext = typeof next === 'string' && next.startsWith('/') && !next.startsWith('//')
    router.push(safeNext ? next : homePath())
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    isLoading.value = false
  }
}

async function handleRegisterRequest() {
  resetMessages()
  isLoading.value = true
  try {
    await authApi.register(form.email, form.accountType)
    // Подтверждение по ссылке из письма снова спрашивает тип аккаунта — подставим выбранный здесь
    try { localStorage.setItem('fitcoach.registerAccountType', form.accountType) } catch (_) { /* приватный режим */ }
    successMessage.value = 'Если регистрация доступна — на email придёт письмо со ссылкой.'
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    isLoading.value = false
  }
}

async function handleRecovery() {
  resetMessages()
  isLoading.value = true
  try {
    await authApi.requestPasswordReset(form.email)
    successMessage.value = 'Если такая почта есть в системе, мы отправили ссылку.'
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  const query = router.currentRoute.value.query
  if (query.registered === '1') {
    currentTab.value = 'login'
    infoMessage.value = 'Регистрация завершена. Войдите с новым паролем.'
  } else if (query.reset === '1') {
    currentTab.value = 'login'
    infoMessage.value = 'Пароль изменён. Войдите с новым паролем.'
  } else if (query.expired === '1') {
    currentTab.value = 'login'
    infoMessage.value = 'Сеанс завершён. Войдите снова — вернём на ту же страницу.'
  }
})
</script>

<style scoped>
.auth-layout { display: flex; min-height: 100vh; width: 100%; }
.auth-cover {
  display: none; width: 45%; max-width: 600px;
  background: var(--color-dark); position: relative;
  overflow: hidden; padding: 40px;
  flex-direction: column; justify-content: space-between;
}
@media (min-width: 1024px) { .auth-cover { display: flex; } }
.cover-bg {
  position: absolute; inset: 0;
  background-image: url('https://placehold.co/600x800/1E1E1E/333333?text=Gym');
  background-size: cover; background-position: center;
  opacity: 0.3; z-index: 1;
}
.cover-content {
  position: relative; z-index: 2;
  display: flex; flex-direction: column; justify-content: space-between; height: 100%;
}
.cover-text { margin: auto 0; }
.cover-text h1 { color: white; font-size: 36px; line-height: 1.2; font-weight: 400; margin-bottom: 16px; }
.cover-text p { color: #D0D0D0; font-size: 14px; line-height: 1.5; max-width: 360px; }
.cover-footer { color: #B5B5B5; font-size: 10px; }

.auth-form-container {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 20px; background: var(--color-white);
}
.mobile-logo { margin-bottom: 30px; }
@media (min-width: 1024px) { .mobile-logo { display: none; } }

.auth-card {
  width: 100%; max-width: 400px; background: var(--color-white);
  border-radius: 16px; padding: 32px;
}
@media (max-width: 1023px) {
  .auth-form-container { background: var(--color-gray-bg); }
  .auth-card { box-shadow: 0 8px 24px rgba(30,30,30,0.06); border: 1px solid var(--color-gray-border); }
}
.card-header { margin: 24px 0; }
.card-header h2 { font-size: 24px; font-weight: 400; color: var(--color-dark); margin-bottom: 8px; }
.card-header p { font-size: 13px; color: var(--color-gray-text); line-height: 1.4; }

.auth-form { display: flex; flex-direction: column; gap: 16px; }
.form-options { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
.link-btn { background: none; border: none; padding: 0; color: var(--color-dark); font-weight: 500; font-size: inherit; cursor: pointer; font-family: inherit; }
.link-btn:hover { text-decoration: underline; }

.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: var(--color-gray-text); font-weight: 500; }
.role-select {
  padding: 10px 12px; border: 1px solid var(--color-gray-border);
  border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none;
}

.error-box { padding: 10px 12px; background: #FCE2E5; color: #D64545; border-radius: 8px; font-size: 12px; }
.success-box { padding: 10px 12px; background: #E9F7D5; color: #2E8B57; border-radius: 8px; font-size: 12px; }
.back-link { background: none; border: none; color: var(--color-gray-text); font-size: 12px; cursor: pointer; text-align: left; font-family: inherit; }
.back-link:hover { color: var(--color-dark); }
.help-text { text-align: center; font-size: 10px; color: #888; margin-top: 8px; }
</style>