<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Заполнение отчёта по тренировке" 
        subtitle="Отчёт за 16 сентября · Группа А1"
        :show-back="true"
        :show-search="false"
        @back="$router.push('/trainer/trainings')"
      />

      <div class="report-form">
        <div class="meta-row">
          <div class="field">
            <label>ДАТА ПРОВЕДЕНИЯ</label>
            <input v-model="form.date" class="field-input" type="text" />
          </div>
          <div class="field">
            <label>ВРЕМЯ</label>
            <input v-model="form.time" class="field-input" type="text" />
          </div>
          <div class="field">
            <label>МЕСТО (ЗАЛ)</label>
            <input v-model="form.place" class="field-input" type="text" />
          </div>
        </div>

        <div class="inputs-block">
          <div class="field">
            <label>ТЕМА ТРЕНИРОВКИ</label>
            <input v-model="form.theme" class="field-input" type="text" />
          </div>
          <div class="field">
            <label>ФАКТИЧЕСКОЕ ПРОВЕДЕНИЕ (КОММЕНТАРИЙ ТРЕНЕРА)</label>
            <textarea v-model="form.comment" class="field-textarea" rows="4"></textarea>
          </div>
        </div>

        <div class="attendance-segment">
          <h3>Присутствие и результаты</h3>
          <div v-for="athlete in athletes" :key="athlete.id" class="attendance-row">
            <div class="athlete-info">
              <button 
                class="checkbox" 
                :class="{ checked: athlete.present }"
                @click="athlete.present = !athlete.present"
              >
                <BaseIcon v-if="athlete.present" name="check" :size="12" color="#102522" />
              </button>
              <span>{{ athlete.name }}</span>
            </div>
            <select v-model="athlete.reason" class="reason-select">
              <option value="present">Присутствовал</option>
              <option value="sick">Болеет</option>
              <option value="no-reason">Пропуск без уваж.</option>
              <option value="other">Другое</option>
            </select>
          </div>
        </div>

        <div class="actions-row">
          <button class="btn-outline" @click="handleDraft">Сохранить черновик</button>
          <button class="btn-primary" @click="handleSubmit">Закрыть отчёт</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const form = reactive({
  date: '',
  time: '',
  place: '',
  theme: '',
  comment: ''
})

const athletes = reactive([])

onMounted(() => {
  // Здесь будет: fetch(`/api/trainings/${route.params.id}`)
  form.date = 'Среда, 16 сентября 2026'
  form.time = '11:00 – 12:30'
  form.place = 'Бассейн · дор. 3'
  form.theme = 'Развитие силовой выносливости. Длинный шаг гребка на средних дистанциях.'
  form.comment = 'Все спортсмены выполнили основной объём (2800м). Ковалева Арина показала отличный темп на заключительном отрезке 400м вольным стилем.'

  athletes.push(
    { id: 1, name: 'Ковалева Арина', present: true, reason: 'present' },
    { id: 2, name: 'Литвинов Максим', present: true, reason: 'present' },
    { id: 3, name: 'Орлова Ольга', present: false, reason: 'sick' },
    { id: 4, name: 'Рогов Дмитрий', present: false, reason: 'no-reason' }
  )
})

const handleDraft = () => alert('Черновик сохранён (демо)')
const handleSubmit = () => alert('Отчёт закрыт (демо)')
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.report-form {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 24px;
}

.meta-row {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.field { display: flex; flex-direction: column; gap: 6px; }
.field label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.field-input, .field-textarea {
  padding: 12px; background: #F4F7F8;
  border-radius: 12px; border: 1px solid #E3EAE8;
  font-size: 14px; color: #152421;
  font-family: inherit; outline: none;
  transition: border-color 0.2s;
}
.field-input:focus, .field-textarea:focus {
  border-color: #B7F34B;
  background: white;
}
.field-textarea { resize: vertical; }

.inputs-block { display: flex; flex-direction: column; gap: 16px; }

.attendance-segment { display: flex; flex-direction: column; gap: 12px; }
.attendance-segment h3 { font-size: 14px; font-weight: 700; color: #152421; }

.attendance-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #E3EAE8;
}
.athlete-info { display: flex; align-items: center; gap: 12px; }

.checkbox {
  width: 20px; height: 20px;
  border: 1px solid #E3EAE8; border-radius: 6px;
  display: flex; justify-content: center; align-items: center;
  background: white; cursor: pointer; padding: 0;
}
.checkbox.checked { background: #B7F34B; border-color: #B7F34B; }

.reason-select {
  padding: 6px 12px;
  border: 1px solid #E3EAE8;
  border-radius: 8px;
  font-size: 13px;
  color: #152421;
  background: white;
  cursor: pointer;
  outline: none;
}

.actions-row { display: flex; justify-content: flex-end; gap: 12px; }
.btn-outline {
  padding: 12px 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 14px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.btn-primary {
  padding: 12px 24px; background: #B7F34B;
  border: none; border-radius: 12px;
  font-size: 14px; font-weight: 700; color: #102522;
  cursor: pointer;
}
</style>