<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader 
        title="Мероприятия и события" 
        subtitle="Ваши спортивные сборы, соревнования и групповые вебинары"
        :show-search="false"
      >
        <template #actions>
          <div class="coach-card">
            <span>Тренер: Алексей К.</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <div class="events-grid">
        <div 
          v-for="event in events" 
          :key="event.id" 
          class="event-card"
        >
          <div class="event-header">
            <div class="event-meta">
              <span 
                class="event-badge" 
                :style="{ background: event.badgeBg, color: event.badgeColor }"
              >
                {{ event.badge }}
              </span>
              <span class="event-date">{{ event.date }}</span>
            </div>
            <button 
              v-if="event.canConfirm"
              class="event-action"
              :class="{ confirmed: event.confirmed }"
              @click="toggleConfirm(event)"
            >
              {{ event.confirmed ? '✓ Участие подтверждено' : 'Подтвердить участие' }}
            </button>
            <button 
              v-else
              class="event-action"
              @click="showDetails(event)"
            >
              Детали регистрации
            </button>
          </div>

          <h3 class="event-title">{{ event.title }}</h3>
          <p class="event-desc">{{ event.description }}</p>

          <div class="event-divider"></div>

          <div class="event-location">
            <BaseIcon name="map-pin" :size="16" color="#6D7D79" />
            <span>{{ event.location }}</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const events = ref([
  {
    id: 1,
    badge: 'Вы приглашены',
    badgeBg: '#FFF1D6',
    badgeColor: '#A36A16',
    date: '15 октября — 25 октября 2026',
    title: 'Осенние спортивные сборы в Сочи (Красная Поляна)',
    description: 'Интенсивный курс подготовки к зимнему сезону. Программа включает двухразовые ежедневные тренировки ОФП, кроссы по пересеченной местности и восстановительные процедуры.',
    location: "Спортивная база 'Юг Спорт'",
    canConfirm: true,
    confirmed: false
  },
  {
    id: 2,
    badge: 'Вы зарегистрированы',
    badgeBg: '#E9F7D5',
    badgeColor: '#2D5B24',
    date: '12 сентября 2026, 12:00',
    title: "Открытый турнир города по боксу 'Золотые перчатки'",
    description: 'Финальные классификационные поединки. Обязательное присутствие для подтверждения взрослого спортивного разряда. Весовая категория до 75 кг.',
    location: 'Дворец Единоборств им. И. Поддубного',
    canConfirm: false,
    confirmed: true
  }
])

const toggleConfirm = (event) => {
  event.confirmed = !event.confirmed
  alert(event.confirmed 
    ? `Участие в "${event.title}" подтверждено` 
    : `Участие в "${event.title}" отменено`)
}

const showDetails = (event) => {
  alert(`Детали регистрации: ${event.title}\n${event.date}\n${event.location}`)
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.coach-card {
  height: 44px; padding: 0 16px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
}
.online-dot { width: 6px; height: 6px; background: #B7F34B; border-radius: 50%; }

.events-grid {
  display: flex; flex-direction: column; gap: 20px;
}

.event-card {
  padding: 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 16px;
  display: flex; flex-direction: column; gap: 16px;
}

.event-header {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  flex-wrap: wrap;
}
.event-meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.event-badge {
  padding: 4px 12px;
  font-size: 12px; font-weight: 700;
  border-radius: 999px;
}
.event-date { font-size: 14px; font-weight: 600; color: #6D7D79; }

.event-action {
  padding: 8px 16px;
  background: #B7F34B; color: #102522;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 700;
  cursor: pointer; transition: opacity 0.2s;
}
.event-action:hover { opacity: 0.9; }
.event-action.confirmed {
  background: #E9F7D5; color: #2D5B24;
}

.event-title {
  font-size: 20px; font-weight: 700; color: #152421;
  margin: 0;
}
.event-desc {
  font-size: 14px; color: #6D7D79; line-height: 1.5;
  margin: 0;
}
.event-divider { height: 1px; background: #E3EAE8; }
.event-location {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #6D7D79;
}
</style>