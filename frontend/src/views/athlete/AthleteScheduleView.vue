<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader 
        title="Моё расписание" 
        subtitle="Ваш персональный календарь тренировок и спаррингов"
        :show-search="false"
      >
        <template #actions>
          <div class="coach-card">
            <span>Тренер: Алексей К.</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <!-- Управление календарём -->
      <div class="calendar-control">
        <div class="calendar-nav">
          <button class="nav-btn" @click="prevWeek">
            <BaseIcon name="chevron-left" :size="14" color="#152421" />
          </button>
          <span class="current-month">{{ currentMonthLabel }}</span>
          <button class="nav-btn" @click="nextWeek">
            <BaseIcon name="chevron-right" :size="14" color="#152421" />
          </button>
        </div>
        <div class="view-switcher">
          <button 
            class="switch-btn" 
            :class="{ active: viewMode === 'week' }"
            @click="viewMode = 'week'"
          >Неделя</button>
          <button 
            class="switch-btn" 
            :class="{ active: viewMode === 'month' }"
            @click="viewMode = 'month'"
          >Месяц</button>
        </div>
      </div>

      <!-- Вид: НЕДЕЛЯ -->
      <div v-if="viewMode === 'week'" class="schedule-grid">
        <div class="table-header">
          <div 
            v-for="day in days" 
            :key="day.label" 
            class="day-header"
            :class="{ active: day.active }"
          >
            <span>{{ day.label }} {{ day.num }}</span>
          </div>
        </div>

        <div class="slots-row">
          <div v-for="(slot, idx) in slots" :key="idx" class="slot-column">
            <div v-if="!slot" class="empty-slot">Нет тренировок</div>
            <div 
              v-else 
              class="event-slot"
              :style="{ background: slot.bg, borderColor: slot.border }"
              @click="openTraining(slot)"
            >
              <div class="slot-time" :style="{ color: slot.timeColor }">{{ slot.time }}</div>
              <div class="slot-title">{{ slot.title }}</div>
              <div class="slot-location" :style="{ color: slot.locColor }">{{ slot.location }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Вид: МЕСЯЦ -->
      <div v-else class="month-grid">
        <div 
          v-for="day in monthDays" 
          :key="day.num" 
          class="month-cell"
          :class="{ today: day.today, hasEvent: day.event }"
        >
          <div class="month-num">{{ day.num }}</div>
          <div v-if="day.event" class="month-event" @click="openTraining(day.event)">
            <span class="month-event-time">{{ day.event.time }}</span>
            <span class="month-event-title">{{ day.event.title }}</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const router = useRouter()
const viewMode = ref('week')
const weekOffset = ref(0)

const currentMonthLabel = computed(() => {
  const months = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь']
  return `${months[8]} 2026` // Сентябрь 2026
})

const days = computed(() => [
  { label: 'Пн', num: 14 + weekOffset.value * 7 },
  { label: 'Вт', num: 15 + weekOffset.value * 7 },
  { label: 'Ср', num: 16 + weekOffset.value * 7 },
  { label: 'Чт', num: 17 + weekOffset.value * 7, active: weekOffset.value === 0 },
  { label: 'Пт', num: 18 + weekOffset.value * 7 },
  { label: 'Сб', num: 19 + weekOffset.value * 7 }
])

const slots = ref([
  null,
  {
    id: 2,
    time: '18:00 - 19:30',
    title: 'ОФП Силовая',
    location: 'Зал ОФП · тренер А. Крылов',
    bg: '#DDECFB', border: '#E3EAE8',
    timeColor: '#35678E', locColor: '#35678E'
  },
  null,
  {
    id: 1,
    time: '17:00 - 18:30',
    title: 'Юниоры Бокс',
    location: 'Зал 2 · тренер А. Крылов',
    bg: '#B7F34B', border: '#B7F34B',
    timeColor: '#102522', locColor: '#102522'
  },
  {
    id: 3,
    time: '18:00 - 19:30',
    title: 'Техника боя',
    location: 'Ринг 1 · тренер А. Крылов',
    bg: '#E9F7D5', border: '#E3EAE8',
    timeColor: '#2D5B24', locColor: '#2D5B24'
  },
  {
    id: 4,
    time: '12:00 - 14:00',
    title: 'Спарринги',
    location: 'Главный ринг · Е. Булатов',
    bg: '#FFF1D6', border: '#E3EAE8',
    timeColor: '#A36A16', locColor: '#A36A16'
  }
])

// Заглушка для месячного вида
const monthDays = ref(
  Array.from({ length: 30 }, (_, i) => ({
    num: i + 1,
    today: i + 1 === 10,
    event: (i + 1 === 10) ? { id: 1, time: '17:00', title: 'Юниоры Бокс' } :
           (i + 1 === 8)  ? { id: 2, time: '18:00', title: 'ОФП' } :
           (i + 1 === 11) ? { id: 3, time: '18:00', title: 'Техника' } :
           (i + 1 === 12) ? { id: 4, time: '12:00', title: 'Спарринги' } : null
  }))
)

const prevWeek = () => { weekOffset.value-- }
const nextWeek = () => { weekOffset.value++ }

const openTraining = (slot) => {
  router.push(`/athlete/schedule/${slot.id}`)
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.icon-btn {
  position: relative;
  width: 44px; height: 44px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.icon-btn .dot {
  position: absolute; top: 11px; right: 10px;
  width: 8px; height: 8px;
  background: #A44450; border-radius: 50%;
}
.coach-card {
  height: 44px; padding: 0 16px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
}
.online-dot { width: 6px; height: 6px; background: #B7F34B; border-radius: 50%; }

.calendar-control {
  display: flex; justify-content: space-between; align-items: center;
  background: white; border: 1px solid #E3EAE8; border-radius: 16px;
  padding: 16px;
}
.calendar-nav { display: flex; align-items: center; gap: 12px; }
.nav-btn {
  width: 36px; height: 36px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 8px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.nav-btn:hover { background: #F4F7F8; }
.current-month { font-size: 16px; font-weight: 700; color: #152421; }

.view-switcher {
  display: flex; gap: 4px; padding: 4px;
  background: #F4F7F8; border-radius: 8px;
}
.switch-btn {
  padding: 6px 16px; border: none; background: transparent;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  border-radius: 6px; cursor: pointer;
}
.switch-btn.active { background: white; color: #152421; }

/* Неделя */
.schedule-grid { display: flex; flex-direction: column; gap: 16px; }
.table-header { display: flex; gap: 12px; padding: 0 16px; }
.day-header {
  flex: 1; padding: 12px;
  background: white; border: 1px solid #E3EAE8; border-radius: 12px;
  text-align: center; font-size: 14px; font-weight: 700; color: #152421;
}
.day-header.active { background: #102522; color: #B7F34B; }

.slots-row { display: flex; gap: 12px; padding: 0 16px; }
.slot-column { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.empty-slot { text-align: center; color: #98A6A2; font-size: 12px; font-weight: 600; padding: 8px 0; }

.event-slot {
  padding: 12px; border-radius: 12px; border: 1px solid;
  display: flex; flex-direction: column; gap: 8px;
  cursor: pointer; transition: transform 0.15s;
}
.event-slot:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(23,52,46,0.1); }
.slot-time { font-size: 11px; font-weight: 700; }
.slot-title { font-size: 13px; font-weight: 700; color: #152421; }
.slot-location { font-size: 11px; font-weight: 400; }

/* Месяц */
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #E3EAE8;
}
.month-cell {
  min-height: 90px;
  padding: 8px;
  background: #F4F7F8;
  border-radius: 10px;
  display: flex; flex-direction: column; gap: 6px;
}
.month-cell.today { background: #102522; }
.month-cell.today .month-num { color: #B7F34B; }
.month-cell.hasEvent { background: #E9F7D5; }
.month-num { font-size: 13px; font-weight: 700; color: #152421; }
.month-event {
  padding: 4px 6px; background: #B7F34B; border-radius: 6px;
  display: flex; flex-direction: column; gap: 2px; cursor: pointer;
}
.month-event-time { font-size: 9px; font-weight: 700; color: #102522; }
.month-event-title { font-size: 10px; font-weight: 600; color: #152421; }
</style>