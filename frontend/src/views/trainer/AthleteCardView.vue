<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Личное дело спортсмена" 
        subtitle="Подробные показатели тренировочного процесса"
        :show-back="true"
        :show-search="false"
        @back="$router.push('/trainer/athletes')"
      />

      <div class="split-view">
        <aside class="profile-card">
          <div class="avatar-large">АК</div>
          <h2>{{ athlete.name }}</h2>
          <span class="tag">{{ athlete.group }}</span>

          <div class="divider"></div>

          <div class="meta-list">
            <div class="meta-item">
              <span class="meta-label">Дата рождения</span>
              <span class="meta-value">{{ athlete.birthDate }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Разряд</span>
              <span class="meta-value">{{ athlete.rank }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Контакты родителей</span>
              <span class="meta-value">{{ athlete.parent }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Мед. допуск</span>
              <span class="meta-value">{{ athlete.medical }}</span>
            </div>
          </div>
        </aside>

        <div class="right-content">
          <div class="tabs">
            <button class="tab-btn">История тренировок</button>
            <button class="tab-btn">Посещаемость</button>
            <button class="tab-btn active">Прогресс и нормативы</button>
            <button class="tab-btn">Документы</button>
          </div>

          <div class="card">
            <h3>Результаты контрольных срезов</h3>

            <div class="metrics-row">
              <div class="metric-card">
                <span class="metric-label">50м вольный стиль</span>
                <span class="metric-value">27.4 сек</span>
                <span class="metric-trend up">-0.8с (Улучшение)</span>
              </div>
              <div class="metric-card">
                <span class="metric-label">100м комплекс</span>
                <span class="metric-value">1:12.10</span>
                <span class="metric-trend neutral">+0.2с (Стабильно)</span>
              </div>
              <div class="metric-card">
                <span class="metric-label">Подтягивания</span>
                <span class="metric-value">24 раза</span>
                <span class="metric-trend up">+4 повторения</span>
              </div>
            </div>

            <div class="divider"></div>

            <div class="chart-block">
              <h4>Динамика результатов: 50м вольный стиль</h4>
              <div class="bars-chart">
                <div v-for="(bar, i) in chartData" :key="i" class="bar-item">
                  <span class="bar-value">{{ bar.value }}</span>
                  <div class="bar" :style="{ height: bar.height + 'px', background: bar.active ? '#B7F34B' : '#102522' }"></div>
                  <span class="bar-label">{{ bar.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'

const route = useRoute()

// ─────────── Данные спортсмена ───────────
// Готово к интеграции: заменить на fetch(`/api/athletes/${route.params.id}`)
const athlete = ref({
  name: '—',
  group: '—',
  birthDate: '—',
  rank: '—',
  parent: '—',
  medical: '—'
})

const chartData = ref([])

onMounted(() => {
  // Моковые данные (убрать после подключения к API)
  athlete.value = {
    name: 'Ковалева Арина',
    group: 'Группа А1',
    birthDate: '12.05.2010 (16 лет)',
    rank: '1-й юношеский',
    parent: 'Ольга К. (Мать) · +7 921 555-1234',
    medical: 'До 12.12.2026 · Активен'
  }
  chartData.value = [
    { value: '28.9с', height: 100, label: 'Май' },
    { value: '28.2с', height: 85, label: 'Июнь' },
    { value: '27.9с', height: 70, label: 'Июль' },
    { value: '27.4с', height: 50, label: 'Сентябрь', active: true }
  ]
})
</script>

<style scoped>
/* стили как раньше */
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace {
  flex: 1; padding: 28px 32px 32px;
  display: flex; flex-direction: column; gap: 24px;
  overflow-y: auto;
}

.split-view {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
  align-items: start;
}
@media (max-width: 1024px) {
  .split-view { grid-template-columns: 1fr; }
}

.profile-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column;
  align-items: center; gap: 16px;
}
.avatar-large {
  width: 100px; height: 100px; background: #E9F7D5;
  border-radius: 999px; display: flex;
  justify-content: center; align-items: center;
  font-size: 32px; font-weight: 700; color: #2D5B24;
}
.profile-card h2 { font-size: 20px; font-weight: 700; color: #152421; text-align: center; }
.tag {
  padding: 4px 12px; background: #E9F7D5;
  border-radius: 999px; font-size: 12px;
  font-weight: 700; color: #2E8B57;
}
.divider { width: 100%; height: 1px; background: #E3EAE8; }

.meta-list { width: 100%; display: flex; flex-direction: column; gap: 12px; }
.meta-item { display: flex; flex-direction: column; gap: 4px; }
.meta-label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.meta-value { font-size: 14px; font-weight: 600; color: #152421; }

.right-content { display: flex; flex-direction: column; gap: 20px; }

.tabs { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}
.metric-card {
  background: #F4F7F8; border-radius: 12px;
  padding: 16px; display: flex; flex-direction: column; gap: 8px;
}
.metric-label {
  font-size: 12px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.metric-value { font-size: 22px; font-weight: 700; color: #152421; }
.metric-trend { font-size: 13px; font-weight: 600; }
.metric-trend.up { color: #2E8B57; }
.metric-trend.neutral { color: #6D7D79; }

.chart-block { display: flex; flex-direction: column; gap: 12px; }
.chart-block h4 { font-size: 14px; font-weight: 700; color: #152421; }

.bars-chart {
  display: flex; justify-content: center;
  align-items: flex-end; gap: 32px;
  height: 180px; padding: 20px;
  background: #F4F7F8; border-radius: 12px;
}
.bar-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 8px;
}
.bar-value { font-size: 12px; font-weight: 700; color: #152421; }
.bar {
  width: 40px; border-radius: 6px 6px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 11px; color: #6D7D79; }
</style>