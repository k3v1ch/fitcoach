<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Добро пожаловать, Алексей!" 
        subtitle="Сегодня Пятница, 18 сентября 2026"
        :show-search="false"
      >
        <template #actions>
          <BaseButton @click="showTrainingModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать тренировку
          </BaseButton>
        </template>
      </PageHeader>

      <section class="stats-grid">
        <StatCard label="Активных спортсменов" :value="stats.athletes" trend="+4" subtext="зарегистрировано" />
        <StatCard label="Тренировок сегодня" :value="stats.trainings" subtext="в расписании" />
        <StatCard label="Посещаемость" :value="stats.attendance" trend="+2.4%" subtext="средняя за месяц" />
        <StatCard label="Ближайший сбор" :value="stats.camp" subtext="Сочи 2026" />
        <StatCard label="Предстоящих событий" :value="stats.events" subtext="в этом месяце" />
      </section>

      <section class="content-grid">
        <div class="card schedule-card">
          <h2>Расписание на сегодня</h2>
          <div class="schedule-list">
            <div v-for="item in schedule" :key="item.time" class="schedule-item">
              <div class="schedule-time">{{ item.time }}</div>
              <div class="schedule-info">
                <div class="schedule-title">{{ item.title }}</div>
                <div class="schedule-location">{{ item.location }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="right-column">
          <div class="card actions-card">
            <h2>Последние действия</h2>
            <ul class="activity-list">
              <li v-for="(act, i) in activities" :key="i" class="activity-item">
                <span class="dot"></span>
                {{ act }}
              </li>
            </ul>
          </div>

          <div class="card quick-actions">
            <h2>Быстрые действия</h2>
            <button class="quick-btn" @click="showTrainingModal = true">
              ⚡ Создать тренировку
            </button>
            <button class="quick-btn" @click="showAthleteModal = true">
              ⚡ Добавить спортсмена
            </button>
            <button class="quick-btn" @click="showPaymentModal = true">
              ⚡ Начислить оплату
            </button>
            <button class="quick-btn" @click="showEventModal = true">
              ⚡ Создать событие
            </button>
          </div>
        </div>
      </section>

      <BaseModal v-model="showAthleteModal" title="Добавить нового спортсмена" @submit="handleAthleteSubmit">
        <BaseInput v-model="athleteForm.name" label="ФИО спортсмена" placeholder="Иван Иванов" />
        <BaseInput v-model="athleteForm.group" label="Группа" placeholder="Группа А1 (Старшие)" />
        <BaseInput v-model="athleteForm.section" label="Секция" placeholder="Плавание" />
        <BaseInput v-model="athleteForm.age" label="Возраст" placeholder="17" />
      </BaseModal>

      <BaseModal v-model="showTrainingModal" title="Создать тренировку" @submit="handleTrainingSubmit">
        <BaseInput v-model="trainingForm.group" label="Группа" placeholder="Группа А1 (Старшие)" />
        <div class="row-2">
          <BaseInput v-model="trainingForm.date" label="Дата" placeholder="16.10.2024" />
          <BaseInput v-model="trainingForm.time" label="Время" placeholder="10:00" />
        </div>
        <BaseInput v-model="trainingForm.type" label="Тип тренировки" placeholder="Водная подготовка" />
        <BaseInput v-model="trainingForm.place" label="Место" placeholder="Бассейн" />
      </BaseModal>

      <BaseModal v-model="showEventModal" title="Создать событие" @submit="handleEventSubmit">
        <BaseInput v-model="eventForm.title" label="Наименование" placeholder="Летние сборы" />
        <div class="row-2">
          <BaseInput v-model="eventForm.dateFrom" label="Дата С" placeholder="16.10.2024" />
          <BaseInput v-model="eventForm.dateTo" label="До" placeholder="20.10.2024" />
        </div>
        <BaseInput v-model="eventForm.type" label="Тип" placeholder="Сборы" />
        <BaseInput v-model="eventForm.participants" label="Количество участников" placeholder="20" />
      </BaseModal>

      <BaseModal v-model="showPaymentModal" title="Начислить оплату" @submit="handlePaymentSubmit">
        <BaseInput v-model="paymentForm.athlete" label="Спортсмен" placeholder="Иван Иванов" />
        <BaseInput v-model="paymentForm.group" label="Группа" placeholder="Группа А1 (Старшие)" />
        <BaseInput v-model="paymentForm.service" label="Услуга" placeholder="Месячный абонемент" />
        <BaseInput v-model="paymentForm.amount" label="Сумма, ₽" placeholder="3 400" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import StatCard from '../../components/layout/StatCard.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const stats = ref({
  athletes: '0 атлетов',
  trainings: '0 занятий',
  attendance: '0%',
  camp: '—',
  events: '0 событий'
})

const schedule = ref([])
const activities = ref([])

onMounted(() => {
  stats.value = {
    athletes: '48 атлетов',
    trainings: '5 занятий',
    attendance: '87%',
    camp: 'через 5 дней',
    events: '3 события'
  }
  schedule.value = [
    { time: '09:00', title: 'Группа А1 (Старшие)', location: 'Бассейн (Дорожка 3)' },
    { time: '11:00', title: 'Спец-подготовка', location: 'Бассейн (Дорожка 1)' },
    { time: '14:00', title: 'ОФП Начальная', location: 'Зал сухого плавания' },
    { time: '17:00', title: 'Юниоры Бокс', location: 'Зал единоборств' }
  ]
  activities.value = [
    'Орлова О. добавила справку по болезни',
    'Литвинов М. оплатил абонемент',
    'Добавлена новая секция Плавание',
    'Рогов Д. пропустил занятие',
    'Группа А1: изменен тренер'
  ]
})

const showAthleteModal = ref(false)
const showTrainingModal = ref(false)
const showEventModal = ref(false)
const showPaymentModal = ref(false)

const athleteForm = reactive({ name: '', group: '', section: '', age: '' })
const trainingForm = reactive({ group: '', date: '', time: '', type: '', place: '' })
const eventForm = reactive({ title: '', dateFrom: '', dateTo: '', type: '', participants: '' })
const paymentForm = reactive({ athlete: '', group: '', service: '', amount: '' })

const clearForm = (form) => Object.keys(form).forEach(k => form[k] = '')

const handleAthleteSubmit = () => {
  alert(`Спортсмен ${athleteForm.name} добавлен (демо)`)
  showAthleteModal.value = false
  clearForm(athleteForm)
}
const handleTrainingSubmit = () => {
  alert('Тренировка создана (демо)')
  showTrainingModal.value = false
  clearForm(trainingForm)
}
const handleEventSubmit = () => {
  alert('Событие создано (демо)')
  showEventModal.value = false
  clearForm(eventForm)
}
const handlePaymentSubmit = () => {
  alert(`Начисление ${paymentForm.amount} ₽ для ${paymentForm.athlete} создано (демо)`)
  showPaymentModal.value = false
  clearForm(paymentForm)
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace {
  flex: 1; padding: 28px 32px 32px;
  display: flex; flex-direction: column; gap: 24px;
  overflow-y: auto;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.content-grid {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 24px;
}
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }
.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.card h2 { font-size: 18px; font-weight: 700; color: #152421; }
.schedule-list { display: flex; flex-direction: column; gap: 12px; }
.schedule-item {
  display: flex; align-items: center; gap: 16px;
  padding: 12px; background: #F4F7F8; border-radius: 12px;
}
.schedule-time { width: 60px; font-weight: 700; color: #152421; }
.schedule-title { font-weight: 600; color: #152421; font-size: 14px; }
.schedule-location { font-size: 12px; color: #6D7D79; }
.right-column { display: flex; flex-direction: column; gap: 24px; }
.activity-list { list-style: none; display: flex; flex-direction: column; gap: 12px; }
.activity-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #6D7D79;
}
.dot {
  width: 8px; height: 8px;
  background: #B7F34B; border-radius: 50%;
  flex-shrink: 0;
}
.quick-btn {
  text-align: left;
  background: #F4F7F8;
  border: 1px solid #E3EAE8;
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
  transition: background 0.2s;
}
.quick-btn:hover { background: #E9F0EE; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>