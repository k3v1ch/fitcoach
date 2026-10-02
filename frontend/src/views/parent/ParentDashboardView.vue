<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Добро пожаловать, Алексей!" 
        subtitle="Среда, 16 сентября · У Арины сегодня 1 тренировка"
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

      <!-- Метрики -->
      <section class="metrics-grid">
        <MetricCard 
          label="Ближайшее событие" 
          value="Сегодня в 11:00" 
          note="Плавание · Бассейн 'Дельфин'"
          icon="clock"
          color="#E9F7D5"
          icon-color="#2E8B57"
        />
        <MetricCard 
          label="Посещаемость" 
          value="94%" 
          note="28 из 30 занятий посещено"
          icon="check"
          color="#DDECFB"
          icon-color="#35678E"
        />
        <MetricCard 
          label="Объявления" 
          value="3 новых" 
          note="1 требует вашего ответа"
          icon="bell"
          color="#FFF1D6"
          icon-color="#F2B705"
        />
        <MetricCard 
          label="Задолженность" 
          value="0 ₽" 
          note="Оплачено до конца сентября"
          icon="credit-card"
          color="#E9F7D5"
          icon-color="#2E8B57"
        />
      </section>

      <!-- Основной контент -->
      <section class="content-grid">
        <div class="left-column">
          <!-- Расписание на неделю -->
          <div class="card">
            <div class="card-header">
              <div>
                <h3>Ближайшее расписание</h3>
                <p>Сентябрь 14 - 18</p>
              </div>
              <router-link to="/parent/schedule" class="link">Смотреть всё расписание →</router-link>
            </div>
            <div class="week-row">
              <div v-for="day in week" :key="day.label" class="day-block">
                <div class="day-label" :class="{ active: day.active }">
                  <span class="day-name">{{ day.label }}</span>
                  <span class="day-num">{{ day.num }}</span>
                </div>
                <div class="event-mini" :class="day.color">
                  <span class="event-name">{{ day.event }}</span>
                  <span class="event-time">{{ day.time }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Быстрые действия -->
          <div class="card">
            <h3>Быстрые действия</h3>
            <div class="actions-row">
              <button class="action-btn green">
                <BaseIcon name="credit-card" :size="16" color="#102522" />
                Оплатить абонемент
              </button>
              <button class="action-btn dark">
                <BaseIcon name="message" :size="16" color="#B7F34B" />
                Связаться с тренером
              </button>
            </div>
          </div>
        </div>

        <!-- Объявления -->
        <aside class="card announcements-card">
          <div class="card-header">
            <h3>Объявления секции</h3>
            <span class="badge-red">3 Важных</span>
          </div>
          <div class="announcements-list">
            <div v-for="a in announcements" :key="a.id" class="announcement-item" :class="a.class">
              <div class="announcement-meta">
                <span>{{ a.date }}</span>
                <span class="author">{{ a.author }}</span>
              </div>
              <div class="announcement-title">{{ a.title }}</div>
              <div class="announcement-desc">{{ a.description }}</div>
            </div>
          </div>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'

const week = [
  { label: 'Пн', num: 14, event: 'Силовая', time: '09:00', color: 'green' },
  { label: 'Вт', num: 15, event: 'Отдых', time: '--:--', color: 'gray' },
  { label: 'Ср', num: 16, event: 'Вода', time: '11:00', color: 'lime', active: true },
  { label: 'Чт', num: 17, event: 'ОФП', time: '08:30', color: 'yellow' },
  { label: 'Пт', num: 18, event: 'Вода', time: '09:30', color: 'green' }
]

const announcements = [
  { id: 1, date: 'Сегодня, 10:12', author: 'Администрация', title: 'Сбор согласий на медицинский осмотр', description: 'Необходимо заполнить и принести форму согласия до конца недели.', class: 'red' },
  { id: 2, date: 'Вчера, 18:30', author: 'Тренер Алексей К.', title: 'Изменение расписания в пятницу', description: 'Тренировка по плаванию переносится на 15:30 в связи с сантехническими работами.', class: 'gray' },
  { id: 3, date: '12 сентября', author: 'Бухгалтерия', title: 'Оплата сборов «Олимпиец» Сочи', description: 'Открыта регистрация и оплата первого взноса на осенние спортивные сборы.', class: 'red' }
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

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }

.left-column { display: flex; flex-direction: column; gap: 24px; }
.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-header p { font-size: 12px; color: #6D7D79; }

.link { color: #2E8B57; font-size: 13px; font-weight: 600; text-decoration: none; }

.week-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.day-block { display: flex; flex-direction: column; gap: 8px; }
.day-label {
  padding: 8px; background: #F4F7F8; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.day-label.active { background: #102522; }
.day-label.active .day-name { color: white; }
.day-label.active .day-num { color: #B7F34B; }
.day-name { font-size: 11px; font-weight: 600; color: #6D7D79; }
.day-num { font-size: 14px; font-weight: 700; color: #152421; }

.event-mini {
  padding: 8px; border-radius: 8px;
  display: flex; flex-direction: column; gap: 4px;
}
.event-mini.green { background: #E9F7D5; }
.event-mini.gray { background: #F4F7F8; }
.event-mini.lime { background: #B7F34B; }
.event-mini.yellow { background: #FFF1D6; }
.event-name { font-size: 11px; font-weight: 700; color: #152421; }
.event-time { font-size: 10px; color: #6D7D79; }

.actions-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.action-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; border: none; border-radius: 12px;
  font-size: 13px; font-weight: 700; cursor: pointer;
}
.action-btn.green { background: #B7F34B; color: #102522; }
.action-btn.dark { background: #102522; color: white; }

.announcements-card { align-self: start; }
.badge-red {
  padding: 2px 8px; background: #FCE2E5; color: #D64545;
  border-radius: 99px; font-size: 11px; font-weight: 700;
}
.announcements-list { display: flex; flex-direction: column; gap: 16px; }
.announcement-item {
  padding: 12px; border-radius: 12px;
  display: flex; flex-direction: column; gap: 10px;
}
.announcement-item.red { background: #FCE2E5; }
.announcement-item.gray { background: #F4F7F8; }
.announcement-meta { display: flex; justify-content: space-between; font-size: 11px; color: #6D7D79; }
.announcement-meta .author { color: #152421; font-weight: 600; }
.announcement-title { font-size: 13px; font-weight: 700; color: #152421; }
.announcement-desc { font-size: 12px; color: #6D7D79; line-height: 1.4; }
</style>