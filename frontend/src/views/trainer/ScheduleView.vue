<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Расписание занятий" 
        subtitle="Календарь тренировок и сборов"
      >
        <template #actions>
          <BaseButton @click="handleAddEvent">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить событие
          </BaseButton>
        </template>
      </PageHeader>

      <div class="schedule-controls">
        <div class="view-tabs">
          <button 
            v-for="view in views" 
            :key="view.id"
            class="view-tab"
            :class="{ active: currentView === view.id }"
            @click="switchView(view.id)"
          >
            {{ view.label }}
          </button>
        </div>
        <div class="date-nav">
          <button class="nav-btn" @click="prevPeriod">
            <BaseIcon name="chevron-left" :size="14" color="#152421" />
          </button>
          <span class="date-range">{{ currentDateLabel }}</span>
          <button class="nav-btn" @click="nextPeriod">
            <BaseIcon name="chevron-right" :size="14" color="#152421" />
          </button>
        </div>
      </div>

      <!-- Неделя -->
      <div v-if="currentView === 'week'" class="schedule-card">
        <div class="schedule-header">
          <div class="time-col">ВРЕМЯ</div>
          <div v-for="day in weekDays" :key="day.label" class="day-col">{{ day.label }}</div>
        </div>
        <div v-for="slot in schedule" :key="slot.time" class="schedule-row">
          <div class="time-col">{{ slot.time }}</div>
          <div v-for="(cell, i) in slot.cells" :key="i" class="day-col">
            <div v-if="cell" class="event" :class="cell.color">{{ cell.title }}</div>
          </div>
        </div>
        <div class="legend">
          <span class="legend-title">Легенда разделов:</span>
          <span class="legend-item"><span class="dot" style="background: #2E8B57"></span> Плавание</span>
          <span class="legend-item"><span class="dot" style="background: #D64545"></span> Бокс</span>
          <span class="legend-item"><span class="dot" style="background: #F2B705"></span> Гимнастика</span>
          <span class="legend-item"><span class="dot" style="background: #35678E"></span> Индивидуальные / Сборы</span>
        </div>
      </div>

      <!-- Месяц -->
      <div v-else-if="currentView === 'month'" class="schedule-card">
        <h3>Месяц: {{ currentDateLabel }}</h3>
        <div class="empty-state">Календарь на месяц (демо). Скоро здесь появятся данные.</div>
      </div>

      <!-- День -->
      <div v-else class="schedule-card">
        <h3>День: {{ currentDateLabel }}</h3>
        <div class="empty-state">Детальное расписание на день (демо). Скоро здесь появятся данные.</div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const views = [
  { id: 'week', label: 'Неделя' },
  { id: 'month', label: 'Месяц' },
  { id: 'day', label: 'День' }
]
const currentView = ref('week')

const currentDate = ref(new Date(2026, 8, 14)) // 14 сентября 2026

const weekDays = computed(() => {
  const start = new Date(currentDate.value)
  const day = start.getDay()
  const diff = day === 0 ? -6 : 1 - day
  start.setDate(start.getDate() + diff)
  
  const days = []
  const dayNames = ['ПН', 'ВТ', 'СР', 'ЧТ', 'ПТ', 'СБ', 'ВС']
  for (let i = 0; i < 7; i++) {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    days.push({ label: `${dayNames[i]} ${d.getDate()}` })
  }
  return days
})

const currentDateLabel = computed(() => {
  const d = currentDate.value
  if (currentView.value === 'week') {
    const start = new Date(d)
    const day = start.getDay()
    const diff = day === 0 ? -6 : 1 - day
    start.setDate(start.getDate() + diff)
    const end = new Date(start)
    end.setDate(end.getDate() + 6)
    const monthNames = ['Января', 'Февраля', 'Марта', 'Апреля', 'Мая', 'Июня', 'Июля', 'Августа', 'Сентября', 'Октября', 'Ноября', 'Декабря']
    return `${start.getDate()} — ${end.getDate()} ${monthNames[end.getMonth()]} ${end.getFullYear()}`
  }
  if (currentView.value === 'month') {
    const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']
    return `${monthNames[d.getMonth()]} ${d.getFullYear()}`
  }
  return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })
})

const switchView = (id) => {
  currentView.value = id
}

const prevPeriod = () => {
  const d = new Date(currentDate.value)
  if (currentView.value === 'week') {
    d.setDate(d.getDate() - 7)
  } else if (currentView.value === 'month') {
    d.setMonth(d.getMonth() - 1)
  } else {
    d.setDate(d.getDate() - 1)
  }
  currentDate.value = d
}

const nextPeriod = () => {
  const d = new Date(currentDate.value)
  if (currentView.value === 'week') {
    d.setDate(d.getDate() + 7)
  } else if (currentView.value === 'month') {
    d.setMonth(d.getMonth() + 1)
  } else {
    d.setDate(d.getDate() + 1)
  }
  currentDate.value = d
}

const schedule = [
  { time: '08:00', cells: [null, null, null, null, null, null, null] },
  { time: '10:00', cells: [
    { title: 'Плавание (А1)', color: 'green' }, null,
    { title: 'Плавание (А1)', color: 'green' }, null,
    { title: 'Силовая А1', color: 'blue' },
    { title: 'ОФП Дети', color: 'green' }, null
  ]},
  { time: '12:00', cells: [
    null, null,
    { title: 'Спец-плавание', color: 'blue' },
    null, null,
    { title: 'Бокс Юниоры', color: 'red' }, null
  ]},
  { time: '14:00', cells: [
    null,
    { title: 'ОФП Начало', color: 'green' }, null,
    { title: 'ОФП Начало', color: 'green' },
    null, null, null
  ]},
  { time: '16:00', cells: [
    { title: 'Гимнастика', color: 'yellow' }, null, null,
    { title: 'Гимнастика', color: 'yellow' },
    null, null, null
  ]},
  { time: '18:00', cells: [
    null,
    { title: 'Бокс Юниоры', color: 'red' }, null,
    { title: 'Бокс Юниоры', color: 'red' },
    null, null, null
  ]}
]

const handleAddEvent = () => alert('Добавление события (демо)')
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.schedule-controls {
  display: flex; justify-content: space-between;
  align-items: center; flex-wrap: wrap; gap: 16px;
}
.view-tabs { display: flex; gap: 8px; }
.view-tab {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.view-tab.active { background: #102522; color: white; border-color: #102522; }

.date-nav { display: flex; align-items: center; gap: 16px; }
.nav-btn {
  padding: 8px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  cursor: pointer; display: flex;
  justify-content: center; align-items: center;
}
.nav-btn:hover { background: #F4F7F8; }
.date-range { font-size: 16px; font-weight: 700; color: #152421; }

.schedule-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
  overflow-x: auto;
}
.schedule-header, .schedule-row {
  display: flex; gap: 12px;
  min-width: 900px;
}
.schedule-header { padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.time-col {
  width: 80px; font-size: 13px; font-weight: 700;
  color: #6D7D79; flex-shrink: 0;
}
.day-col {
  flex: 1; text-align: center;
  font-size: 12px; font-weight: 700; color: #6D7D79;
}
.schedule-row {
  padding: 16px 0; border-bottom: 1px solid #E3EAE8;
  align-items: center;
}
.schedule-row .day-col {
  height: 40px; border-radius: 8px;
  border: 1px solid #E3EAE8;
  display: flex; justify-content: center; align-items: center;
}
.event {
  width: 100%; height: 100%; border-radius: 8px;
  display: flex; justify-content: center; align-items: center;
  font-size: 12px; font-weight: 700;
}
.event.green { background: #E9F7D5; color: #2E8B57; }
.event.red { background: #FCE2E5; color: #D64545; }
.event.yellow { background: #FFF8E6; color: #F2B705; }
.event.blue { background: #DDECFB; color: #35678E; }

.empty-state {
  padding: 60px 20px; text-align: center;
  color: #6D7D79; font-size: 14px;
  background: #F4F7F8; border-radius: 12px;
}

.legend {
  display: flex; gap: 24px; flex-wrap: wrap;
  padding-top: 12px; align-items: center;
}
.legend-title { font-size: 13px; font-weight: 600; color: #152421; }
.legend-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #6D7D79;
}
.dot { width: 12px; height: 12px; border-radius: 50%; }
</style>