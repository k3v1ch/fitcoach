<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Спортивный прогресс" 
        subtitle="Оценка показателей, комментарии тренера и разряды"
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

      <section class="top-section">
        <!-- Карточка разряда -->
        <div class="rank-card">
          <div class="rank-header">
            <span class="rank-label">ТЕКУЩИЙ РАЗРЯД</span>
            <BaseIcon name="award" :size="24" color="#B7F34B" />
          </div>
          <div class="rank-value">II Юношеский разряд</div>
          <div class="rank-note">
            Утвержден Федерацией плавания от 12.04.2026. До I разряда осталось улучшить время на 2.4 сек.
          </div>
        </div>

        <!-- Быстрые метрики -->
        <div class="metrics">
          <MetricCard 
            label="Лучшее время (50м вольный)" 
            value="34.12 сек" 
            note="Обновлено 2 недели назад"
            icon="award"
            color="#E9F7D5"
            icon-color="#2E8B57"
          />
          <MetricCard 
            label="Посещаемость нормативов" 
            value="100%" 
            note="Сдано 5 из 5 базовых нормативов"
            icon="check"
            color="#DDECFB"
            icon-color="#35678E"
          />
        </div>
      </section>

      <section class="content-grid">
        <div class="card chart-card">
          <h3>Динамика результатов (50м вольный стиль, сек)</h3>
          <div class="chart">
            <div class="y-axis">
              <span>38</span>
              <span>36</span>
              <span>34</span>
            </div>
            <div class="chart-area">
              <div v-for="line in 3" :key="line" class="grid-line"></div>
              <div class="bars">
                <div v-for="bar in bars" :key="bar.label" class="bar-item">
                  <div class="bar" :style="{ height: bar.height + '%' }"></div>
                  <span class="bar-label">{{ bar.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card feedback-card">
          <h3>Отзывы тренера</h3>
          <div class="feedback-list">
            <div v-for="fb in feedbacks" :key="fb.id" class="feedback-item">
              <div class="feedback-header">
                <span class="feedback-date">{{ fb.date }}</span>
                <span class="feedback-author">{{ fb.author }}</span>
              </div>
              <p>{{ fb.text }}</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import MetricCard from '../../components/layout/MetricCard.vue'

const bars = [
  { label: 'Май', height: 20 },
  { label: 'Июнь', height: 40 },
  { label: 'Июль', height: 55 },
  { label: 'Август', height: 75 },
  { label: 'Сентябрь', height: 90 }
]

const feedbacks = [
  { id: 1, date: '15 сентября', author: 'Алексей Крылов', text: 'Арина отлично отработала технику гребка на сегодняшней воде. Заметен прогресс в выносливости.' },
  { id: 2, date: '10 сентября', author: 'Алексей Крылов', text: 'Рекомендуется уделить внимание стартовому толчку. Теряем драгоценные доли секунды на выходе.' },
  { id: 3, date: '01 сентября', author: 'Егор Булатов', text: 'Нормативы ОФП сданы на твердую четверку с плюсом. Физическая форма перед сезоном хорошая.' }
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
}
.child-avatar {
  width: 24px; height: 24px;
  background: #DDECFB; color: #35678E;
  border-radius: 99px;
  display: flex; justify-content: center; align-items: center;
  font-size: 10px; font-weight: 700;
}

.top-section { display: flex; gap: 24px; flex-wrap: wrap; }
.rank-card {
  width: 360px; padding: 24px;
  background: #102522; border-radius: 20px;
  display: flex; flex-direction: column; gap: 16px;
}
.rank-header { display: flex; justify-content: space-between; align-items: center; }
.rank-label { color: #B7F34B; font-size: 13px; font-weight: 700; }
.rank-value { color: white; font-size: 24px; font-weight: 800; }
.rank-note { color: #98A6A2; font-size: 12px; line-height: 1.5; }

.metrics { flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 16px; min-width: 320px; }

.content-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.card h3 { font-size: 16px; font-weight: 700; color: #152421; }

.chart { display: flex; gap: 12px; height: 200px; }
.y-axis {
  display: flex; flex-direction: column; justify-content: space-between;
  font-size: 11px; color: #6D7D79; padding: 0 4px;
}
.chart-area { flex: 1; position: relative; }
.grid-line {
  position: absolute; left: 0; right: 0;
  height: 1px; background: #E3EAE8;
}
.grid-line:nth-child(1) { top: 0; }
.grid-line:nth-child(2) { top: 50%; }
.grid-line:nth-child(3) { top: 100%; }

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

.bars {
  position: absolute; bottom: 0; left: 0; right: 0;
  display: flex; justify-content: space-around;
  align-items: flex-end; height: 100%;
}
.bar-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 8px; width: 40px;
  height: 100%; justify-content: flex-end;
}
.bar {
  width: 24px; background: #102522;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 11px; color: #6D7D79; }

.feedback-list { display: flex; flex-direction: column; gap: 16px; }
.feedback-item {
  padding: 12px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.feedback-header { display: flex; justify-content: space-between; }
.feedback-date { font-size: 11px; color: #6D7D79; font-weight: 600; }
.feedback-author { font-size: 11px; color: #2E8B57; font-weight: 700; }
.feedback-item p { font-size: 13px; color: #152421; line-height: 1.4; }
</style>