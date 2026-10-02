<template>
  <div class="confirm-page">
    <div class="confirm-card">
      <div class="brand">
        <BaseLogo />
      </div>

      <h2>Вы ещё не в организации</h2>
      <p>
        Тренер может создать свою организацию и сразу стать её тренером.
        Родителей и спортсменов добавляет тренер — попросите его пригласить вас по email.
      </p>

      <form @submit.prevent="handleCreate" class="confirm-form">
        <BaseInput id="ob-name" label="Название организации" v-model="form.name" placeholder="СШ «Олимп»" />
        <BaseInput id="ob-address" label="Адрес (необязательно)" v-model="form.address" placeholder="г. Москва, ул. Спортивная, 1" />

        <div class="field">
          <label class="field-label" for="ob-timezone">Часовой пояс</label>
          <select id="ob-timezone" v-model="form.timezone" class="role-select">
            <option v-for="zone in TIMEZONES" :key="zone.id" :value="zone.id">{{ zone.label }}</option>
          </select>
        </div>

        <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>

        <BaseButton type="submit" :loading="isLoading">Создать организацию</BaseButton>
        <button type="button" class="back-link" @click="handleLogout">Выйти из аккаунта</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import BaseLogo from '../../components/ui/BaseLogo.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import { organizationsApi } from '../../api/organizations'
import { loadSession, logout, homePath } from '../../utils/session'

const TIMEZONES = [
  { id: 'Europe/Kaliningrad', label: 'Калининград (UTC+2)' },
  { id: 'Europe/Moscow', label: 'Москва (UTC+3)' },
  { id: 'Europe/Samara', label: 'Самара (UTC+4)' },
  { id: 'Asia/Yekaterinburg', label: 'Екатеринбург (UTC+5)' },
  { id: 'Asia/Omsk', label: 'Омск (UTC+6)' },
  { id: 'Asia/Novosibirsk', label: 'Новосибирск (UTC+7)' },
  { id: 'Asia/Krasnoyarsk', label: 'Красноярск (UTC+7)' },
  { id: 'Asia/Irkutsk', label: 'Иркутск (UTC+8)' },
  { id: 'Asia/Yakutsk', label: 'Якутск (UTC+9)' },
  { id: 'Asia/Vladivostok', label: 'Владивосток (UTC+10)' },
  { id: 'Asia/Magadan', label: 'Магадан (UTC+11)' },
  { id: 'Asia/Kamchatka', label: 'Камчатка (UTC+12)' }
]

// По умолчанию — пояс браузера, если он есть в списке
function defaultTimezone() {
  try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
    if (TIMEZONES.some(z => z.id === zone)) return zone
  } catch (_) { /* старый браузер */ }
  return 'Europe/Moscow'
}

const router = useRouter()
const form = reactive({ name: '', address: '', timezone: defaultTimezone() })
const isLoading = ref(false)
const errorMessage = ref('')

async function handleCreate() {
  errorMessage.value = ''
  const name = form.name.trim()
  if (!name) {
    errorMessage.value = 'Укажите название организации'
    return
  }
  isLoading.value = true
  try {
    await organizationsApi.create({
      name,
      address: form.address.trim() || null,
      timezone: form.timezone
    })
    await loadSession()
    router.push(homePath())
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    isLoading.value = false
  }
}

async function handleLogout() {
  await logout()
  router.push('/')
}
</script>

<style scoped>
.confirm-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: #F4F7F8; padding: 20px;
}
.confirm-card {
  width: 100%; max-width: 460px; background: white;
  border-radius: 20px; padding: 32px; box-shadow: 0 8px 24px rgba(23,52,46,0.06);
}
.brand { display: flex; justify-content: center; margin-bottom: 24px; }
.confirm-card h2 { font-size: 22px; font-weight: 700; color: #152421; margin-bottom: 8px; text-align: center; }
.confirm-card p { font-size: 13px; color: #6D7D79; text-align: center; margin-bottom: 24px; line-height: 1.5; }
.confirm-form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.role-select {
  padding: 10px 12px; border: 1px solid #E3EAE8;
  border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none;
}
.error-box { padding: 10px 12px; background: #FCE2E5; color: #D64545; border-radius: 8px; font-size: 12px; }
.back-link { background: none; border: none; font-size: 13px; color: #6D7D79; cursor: pointer; }
.back-link:hover { color: #152421; }
</style>
