<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Прогресс спортсменов" 
        subtitle="Динамика результатов и выполнения нормативов"
      >
        <template #actions>
          <BaseButton @click="handleAddNorm">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Внести нормативы
          </BaseButton>
        </template>
      </PageHeader>

      <!-- Выбор группы и спортсмена -->
      <div class="selector-bar">
        <div class="selector">
          <span>Выбор группы:</span>
          <select v-model="selectedGroup">
            <option>Группа А1 (Старшие)</option>
            <option>Юниоры Бокс</option>
          </select>
        </div>
        <div class="selector">
          <span>Спортсмен:</span>
          <select v-model="selectedAthlete">
            <option>Ковалева Арина</option>
            <option>Литвинов Максим</option>
          </select>
        </div>
      </div>

      <!-- Карточки метрик -->
      <div class="metrics-strip">
        <StatCard label="Средний балл ОФП" value="7.8 / 10" trend="+0.6" subtext="за последний тест" />
        <StatCard label="Динамика результатов" value="+12%" trend="Выше нормы" subtext="за последние 3 месяца" />
        <StatCard label="Лучший результат" value="100м вольный" trend="Личный рекорд" subtext="58.20 сек (КМС)" />
      </div>

      <!-- График + Нормативы -->
      <div class="content-grid">
        <div class="card chart-card">
          <h3>График успеваемости (тесты выносливости)</h3>
          <div class="bars-chart">
            <div v-for="(bar, i) in chartData" :key="i" class="bar-item">
              <div class="bar" :style="{ height: bar.height + 'px' }"></div>
              <span class="bar-label">{{ bar.label }}</span>
            </div>
          </div>
        </div>

        <div class="card norms-card">
          <h3>Нормативы</h3>
          <table class="norms-table">
            <thead>
              <tr>
                <th>ПОКАЗАТЕЛЬ</th>
                <th>НОРМА</th>
                <th>ФАКТ</th>
                <th>ОТКЛОНЕНИЕ</th>
                <th>ДИНАМИКА</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="norm in norms" :key="norm.name">
                <td>{{ norm.name }}</td>
                <td>{{ norm.norm }}</td>
                <td>{{ norm.fact }}</td>
                <td :class="norm.deviation > 0 ? 'green' : 'red'">
                  {{ norm.deviation > 0 ? '+' : '' }}{{ norm.deviation }}
                </td>
                <td :class="norm.deviation > 0 ? 'green' : 'red'">
                  {{ norm.deviation > 0 ? '↑' : '↓' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StatCard from '../../components/layout/StatCard.vue'

const selectedGroup = ref('Группа А1 (Старшие)')
const selectedAthlete = ref('Ковалева Арина')

const chartData = [
  { height: 24, label: 'Т1' }, { height: 42, label: 'Т2' },
  { height: 54, label: 'Т3' }, { height: 36, label: 'Т4' },
  { height: 72, label: 'Т5' }, { height: 96, label: 'Т6' },
  { height: 90, label: 'Т7' }, { height: 114, label: 'Т8' }
]

const norms = [
  { name: 'Подтягивания', norm: '12 раз', fact: '15 раз', deviation: 3 },
  { name: 'Отжимания', norm: '30 раз', fact: '35 раз', deviation: 5 },
  { name: 'Прыжок в длину', norm: '2.10 м', fact: '2.15 м', deviation: 0.05 },
  { name: 'Пресс (1 мин)', norm: '45 раз', fact: '42 раз', deviation: -3 },
  { name: 'Плавание 50м', norm: '28.50 с', fact: '27.10 с', deviation: 1.4 },
  { name: 'Гибкость (наклон)', norm: '+10 см', fact: '+12 см', deviation: 2 }
]

const handleAddNorm = () => alert('Внесение нормативов (демо)')
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.selector-bar {
  display: flex; gap: 24px; flex-wrap: wrap;
  background: white; padding: 16px; border-radius: 12px;
  border: 1px solid #E3EAE8;
}
.selector { display: flex; align-items: center; gap: 8px; }
.selector span { font-size: 14px; color: #6D7D79; }
.selector select {
  padding: 8px 12px; border: 1px solid #E3EAE8;
  border-radius: 8px; background: #F4F7F8;
  font-size: 14px; font-weight: 600; color: #152421;
  cursor: pointer; outline: none;
}

.metrics-strip {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 1024px) {
  .content-grid { grid-template-columns: 1fr; }
}

.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.bars-chart {
  display: flex; justify-content: space-between;
  align-items: flex-end; gap: 12px;
  height: 200px; padding: 16px;
  background: #F4F7F8; border-radius: 12px;
}
.bar-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 8px;
  flex: 1;
}
.bar {
  width: 12px; background: #102522;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 11px; color: #6D7D79; }

.norms-table {
  width: 100%; border-collapse: collapse;
}
.norms-table th {
  text-align: left; font-size: 12px; font-weight: 700;
  color: #6D7D79; text-transform: uppercase;
  padding-bottom: 8px;
}
.norms-table td {
  padding: 8px 0; font-size: 14px; color: #152421;
  border-bottom: 1px solid #E3EAE8;
}
.norms-table .green { color: #2E8B57; font-weight: 700; }
.norms-table .red { color: #D64545; font-weight: 700; }
</style>