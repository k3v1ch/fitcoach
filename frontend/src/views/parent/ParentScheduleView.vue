<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Расписание тренировок" 
        subtitle="Полный календарь спортивных занятий и мероприятий"
        :show-search="false"
      >
        <template #actions>
          <div class="child-selector">
            <div class="child-avatar">АК</div>
            <span>Арина Ковалева (Плавание)</span>
            <BaseIcon name="chevron-down" :size="14" />
          </div>
          <router-link to="/parent/announcements" class="icon-btn">
            <BaseIcon name="bell" :size="19" color="#152421" />
            <span class="dot"></span>
          </router-link>
        </template>
      </PageHeader>

      <div class="control-bar">
        <div class="tabs">
          <button 
            class="tab-btn" 
            :class="{ active: view === 'upcoming' }"
            @click="view = 'upcoming'"
          >
            Предстоящие
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: view === 'past' }"
            @click="view = 'past'"
          >
            Прошедшие
          </button>
        </div>
        <div class="month-control">
          <button class="nav-btn" @click="prevWeek">
            <BaseIcon name="chevron-left" :size="14" color="#152421" />
          </button>
          <span class="month-label">{{ weekLabel }}</span>
          <button class="nav-btn" @click="nextWeek">
            <BaseIcon name="chevron-right" :size="14" color="#152421" />
          </button>
        </div>
      </div>

      <div v-if="view === 'upcoming'" class="calendar-card">
        <div class="calendar-header">
          <div v-for="day in currentWeek" :key="day.num" class="col-header" :class="{ active: day.active }">
            <div class="day-name">{{ day.name }}</div>
            <div class="day-num">{{ day.num }}</div>
          </div>
        </div>

        <div v-for="slot in slots" :key="slot.time" class="calendar-row">
          <div class="time-col">{{ slot.time }}</div>
          <div class="events-row">
            <div v-for="(cell, i) in slot.cells" :key="i" class="event-cell" :class="cell ? cell.color : 'empty'">
              <template v-if="cell">
                <div class="event-title">{{ cell.title }}</div>
                <div class="event-meta">{{ cell.meta }}</div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p>Прошедшие тренировки за эту неделю</p>
        <div class="past-list">
          <div class="past-item">
            <span class="past-date">11 сентября</span>
            <span>Плавание (Техника)</span>
            <span class="past-status">Посещено</span>
          </div>
          <div class="past-item">
            <span class="past-date">09 сентября</span>
            <span>Плавание (Длинная вода)</span>
            <span class="past-status">Посещено</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const view = ref('upcoming')
const weekOffset = ref(0)

const baseDays = [
  { name: 'Пон', num: 14 },
  { name: 'Вто', num: 15 },
  { name: 'Сре', num: 16, active: true },
  { name: 'Чет', num: 17 },
  { name: 'Пят', num: 18 },
  { name: 'Суб', num: 19 }
]

const currentWeek = computed(() => {
  const shift = weekOffset.value * 7
  return baseDays.map((d, i) => ({
    ...d,
    num: d.num + shift,
    active: weekOffset.value === 0 && i === 2
  }))
})

const weekLabel = computed(() => {
  const start = 14 + weekOffset.value * 7
  const end = 19 + weekOffset.value * 7
  return `${start} – ${end} Сентября 2026`
})

const prevWeek = () => weekOffset.value--
const nextWeek = () => weekOffset.value++

const slots = [
  { time: '09:00 – 10:30', cells: [
    { title: 'Плавание (Техника)', meta: 'Бассейн «Дельфин» · Алексей К.', color: 'green' },
    null,
    { title: 'Персональная тренировка', meta: 'Зал сухого плавания · Алексей К.', color: 'lime' },
    null,
    { title: 'Плавание (Длинная вода)', meta: 'Бассейн «Дельфин» · Алексей К.', color: 'green' },
    null
  ]},
  { time: '15:30 – 17:00', cells: [
    null,
    { title: 'Сухое плавание / ОФП', meta: 'Малый зал · Егор Б.', color: 'yellow' },
    null,
    { title: 'Сухое плавание / ОФП', meta: 'Малый зал · Егор Б.', color: 'yellow' },
    null,
    { title: 'Командный сбор / Игры', meta: 'Главный зал · Егор Б.', color: 'blue' }
  ]}
]
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.child-selector {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.child-avatar {
  width: 24px; height: 24px;
  background: #DDECFB; color: #35678E;
  border-radius: 99px;
  display: flex; justify-content: center; align-items: center;
  font-size: 10px; font-weight: 700;
}

.control-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.tabs { display: flex; gap: 12px; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.month-control { display: flex; align-items: center; gap: 12px; }
.nav-btn {
  padding: 8px; background: white; border: 1px solid #E3EAE8;
  border-radius: 8px; cursor: pointer;
  display: flex; justify-content: center; align-items: center;
}
.month-label { font-size: 15px; font-weight: 700; color: #152421; }

.calendar-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
  overflow-x: auto;
}
.calendar-header {
  display: flex; gap: 12px; min-width: 900px;
}
.col-header {
  flex: 1; padding: 12px;
  background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.col-header.active { background: #B7F34B; }
.day-name { font-size: 12px; font-weight: 600; color: #6D7D79; }
.day-num { font-size: 18px; font-weight: 800; color: #152421; }

.calendar-row {
  display: flex; gap: 12px; min-width: 900px;
  align-items: center;
}
.time-col {
  width: 100px; font-size: 11px; font-weight: 700;
  color: #6D7D79; text-align: center;
}
.events-row {
  flex: 1; display: flex; gap: 12px;
}
.event-cell {
  flex: 1; height: 96px; border-radius: 12px;
  padding: 12px;
  display: flex; flex-direction: column; gap: 6px;
  outline: 1px solid #E3EAE8; outline-offset: -1px;
}
.event-cell.empty { background: transparent; outline-color: transparent; }
.event-cell.green { background: #E9F7D5; }
.event-cell.lime { background: #B7F34B; }
.event-cell.yellow { background: #FFF1D6; }
.event-cell.blue { background: #DDECFB; }
.event-title { font-size: 12px; font-weight: 700; color: #152421; }
.event-meta { font-size: 11px; color: #6D7D79; }

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
  background: #D64545; border-radius: 50%;
}

.empty-state {
  background: white; border-radius: 20px; padding: 40px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.empty-state p { color: #6D7D79; font-size: 14px; }
.past-list { display: flex; flex-direction: column; gap: 12px; }
.past-item {
  display: flex; justify-content: space-between;
  padding: 12px 16px; background: #F4F7F8;
  border-radius: 12px; font-size: 14px;
}
.past-date { color: #6D7D79; }
.past-status { color: #2E8B57; font-weight: 700; }
</style>