<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader 
        title="Добро пожаловать, Иван!" 
        subtitle="Четверг, 10 сентября · У вас 1 тренировка сегодня"
        :show-search="false"
      >
        <template #actions>
          <div class="coach-card">
            <span>Тренер: Алексей К.</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <!-- Quick info -->
      <section class="metrics-grid">
        <MetricCard 
          label="Следующая тренировка" 
          value="17:00" 
          note="Юниоры Бокс · Зал 2"
          icon="activity"
          color="#E9F7D5"
          icon-color="#2E8B57"
        />
        <MetricCard 
          label="Ближайшее событие" 
          value="12 Сен" 
          note='Турнир "Золотые перчатки"'
          icon="award"
          color="#FFF1D6"
          icon-color="#A36A16"
        />
      </section>

      <!-- Content Split -->
      <section class="content-grid">
        <!-- Левая колонка: Расписание на неделю -->
        <div class="left-column">
          <div class="card">
            <div class="card-header">
              <h3>Расписание на эту неделю</h3>
              <router-link to="/athlete/schedule" class="link">Показать всё →</router-link>
            </div>
            <div class="week-mini">
              <div 
                v-for="day in week" 
                :key="day.label" 
                class="day-block"
                :class="{ active: day.active }"
              >
                <div class="day-label">
                  <span class="day-name">{{ day.label }}</span>
                  <span class="day-num">{{ day.num }}</span>
                </div>
                <div class="day-event">
                  <span class="event-time">{{ day.time }}</span>
                  <span class="event-name">{{ day.event }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Правая колонка: События и Сборы -->
        <aside class="right-column">
          <div class="card">
            <h3>События и Сборы</h3>
            <div class="events-list">
              <div v-for="event in events" :key="event.id" class="event-card">
                <div class="event-meta">
                  <span class="event-tag">{{ event.tag }}</span>
                  <span class="event-date">{{ event.date }}</span>
                </div>
                <div class="event-title">{{ event.title }}</div>
                <div class="event-location">{{ event.location }}</div>
              </div>
            </div>
          </div>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'

const week = [
  { label: 'Пн', num: 7, time: '--:--', event: 'Выходной' },
  { label: 'Вт', num: 8, time: '18:00', event: 'ОФП тренировка' },
  { label: 'Ср', num: 9, time: '--:--', event: 'Выходной' },
  { label: 'Чт', num: 10, time: '17:00', event: 'Юниоры Бокс', active: true },
  { label: 'Пт', num: 11, time: '18:00', event: 'Техника боя' },
  { label: 'Сб', num: 12, time: '12:00', event: 'Спарринги' },
  { label: 'Вс', num: 13, time: '--:--', event: 'Выходной' }
]

const events = [
  {
    id: 1,
    tag: 'Сборы',
    date: '15-25 Окт',
    title: 'Осенний лагерь ОФП',
    location: 'Локация: Сочи'
  },
  {
    id: 2,
    tag: 'Соревнования',
    date: '05 Ноя',
    title: 'Городское первенство',
    location: 'Локация: Дворец Спорта'
  }
]
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
.online-dot {
  width: 6px; height: 6px;
  background: #B7F34B; border-radius: 50%;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}
@media (max-width: 1024px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card h3 { font-size: 20px; font-weight: 700; color: #152421; }
.link { color: #35678E; font-size: 13px; font-weight: 600; text-decoration: none; }

.week-mini {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}
.day-block {
  padding: 8px;
  background: #F4F7F8;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.day-block.active {
  background: #102522;
  border-color: #B7F34B;
}
.day-label { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.day-name { font-size: 12px; font-weight: 600; color: #6D7D79; }
.day-num { font-size: 16px; font-weight: 700; color: #152421; }
.day-block.active .day-name { color: #B7F34B; }
.day-block.active .day-num { color: white; }
.day-event {
  min-height: 42px; padding: 4px;
  background: white; border-radius: 8px;
  display: flex; flex-direction: column; gap: 2px;
}
.day-block.active .day-event { background: #19332F; }
.event-time { font-size: 10px; font-weight: 700; color: #152421; }
.day-block.active .event-time { color: #B7F34B; }
.event-name { font-size: 9px; font-weight: 500; color: #6D7D79; }

.events-list { display: flex; flex-direction: column; gap: 12px; }
.event-card {
  padding: 16px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 12px;
}
.event-meta { display: flex; justify-content: space-between; align-items: center; }
.event-tag { font-size: 12px; font-weight: 700; color: #2D5B24; }
.event-date { font-size: 12px; font-weight: 600; color: #6D7D79; }
.event-title { font-size: 15px; font-weight: 700; color: #152421; }
.event-location { font-size: 13px; color: #6D7D79; }
</style>