<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader 
        title="Задания и прогресс" 
        subtitle="Ваша спортивная успеваемость и персональные рекорды"
        :show-search="false"
      >
        <template #actions>
          <div class="coach-card">
            <span>Тренер: Алексей К.</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <div class="content-split">
        <!-- Rank card -->
        <div class="rank-card">
          <div class="rank-icon">
            <BaseIcon name="award" :size="32" color="#102522" />
          </div>
          <div class="rank-info">
            <div class="rank-title">1-й взрослый разряд</div>
            <div class="rank-subtitle">
              Специализация: Бокс · До КМС осталось <strong>300 очков</strong> рейтинга
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
        </div>

        <!-- Records card -->
        <div class="records-card">
          <h3>Личные рекорды спортсмена</h3>
          <div class="records-list">
            <div v-for="record in records" :key="record.id" class="record-item">
              <div class="record-info">
                <div class="record-name">{{ record.name }}</div>
                <div class="record-date">{{ record.date }}</div>
              </div>
              <div class="record-value">{{ record.value }}</div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const progressPercent = ref(0)

const records = [
  { id: 1, name: 'Макс. сила удара (правый кросс)', date: 'Дата рекорда: Август 2026', value: '320 кг' },
  { id: 2, name: 'Челночный бег 10х10 м', date: 'Дата рекорда: Июль 2026', value: '24.2 сек' },
  { id: 3, name: 'Количество подтягиваний', date: 'Дата рекорда: Сентябрь 2026', value: '28 раз' }
]

// Анимация прогресса при загрузке
onMounted(() => {
  setTimeout(() => { progressPercent.value = 70 }, 200)
})
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

.content-split {
  display: flex; flex-direction: column; gap: 24px;
}

/* Rank card */
.rank-card {
  padding: 24px;
  background: #102522;
  border-radius: 20px;
  display: flex; align-items: center; gap: 16px;
}
.rank-icon {
  width: 56px; height: 56px;
  background: #B7F34B;
  border-radius: 16px;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.rank-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.rank-title { color: white; font-size: 18px; font-weight: 700; }
.rank-subtitle { color: #98A6A2; font-size: 13px; }
.rank-subtitle strong { color: #B7F34B; font-weight: 700; }
.progress-bar {
  height: 6px; background: #19332F;
  border-radius: 999px; overflow: hidden;
  margin-top: 4px;
}
.progress-fill {
  height: 100%;
  background: #B7F34B;
  border-radius: 999px;
  transition: width 1s ease-out;
}

/* Records card */
.records-card {
  padding: 24px; background: white;
  border: 1px solid #E3EAE8; border-radius: 20px;
  display: flex; flex-direction: column; gap: 16px;
}
.records-card h3 {
  font-size: 18px; font-weight: 700; color: #152421;
}
.records-list { display: flex; flex-direction: column; gap: 12px; }
.record-item {
  padding-bottom: 12px;
  border-bottom: 1px solid #E3EAE8;
  display: flex; justify-content: space-between; align-items: center;
}
.record-item:last-child { border-bottom: none; padding-bottom: 0; }
.record-info { display: flex; flex-direction: column; gap: 2px; }
.record-name { font-size: 13px; font-weight: 600; color: #152421; }
.record-date { font-size: 11px; color: #98A6A2; }
.record-value { font-size: 15px; font-weight: 700; color: #2D5B24; }
</style>