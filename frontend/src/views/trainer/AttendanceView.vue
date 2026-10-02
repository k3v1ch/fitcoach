<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Журнал посещаемости" 
        subtitle="Статистика группы А1 · Сентябрь 2026"
      >
        <template #actions>
          <BaseButton @click="handleExport">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Экспорт в Excel
          </BaseButton>
        </template>
      </PageHeader>

      <!-- Статистика -->
      <div class="stats-strip">
        <StatCard label="Занятий проведено" value="18 тренировок" subtext="За сентябрь" />
        <StatCard label="Средняя явка" value="88.4%" trend="Выше плана на 3.4%" />
        <StatCard label="Пропуски по болезни" value="5 случаев" subtext="Все подтверждены справками" />
      </div>

      <!-- Таблица посещаемости -->
      <div class="ledger-card">
        <h3>График присутствия</h3>
        <div class="ledger-header">
          <div class="athlete-name">СПОРТСМЕН</div>
          <div class="dates">
            <span v-for="d in dates" :key="d">{{ d }}</span>
          </div>
          <div class="percent">% ЯВКИ</div>
        </div>
        <div v-for="athlete in attendance" :key="athlete.name" class="ledger-row">
          <div class="athlete-name">{{ athlete.name }}</div>
          <div class="dates">
            <span v-for="(mark, i) in athlete.marks" :key="i" class="mark" :class="mark.color">
              {{ mark.label }}
            </span>
          </div>
          <div class="percent">{{ athlete.percent }}%</div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import StatCard from '../../components/layout/StatCard.vue'

const dates = ['01.09', '04.09', '08.09', '11.09', '15.09', '18.09']

const attendance = [
  { name: 'Ковалева Арина', percent: 100, marks: [
    { label: '✓', color: 'green' }, { label: '✓', color: 'green' }, { label: '✓', color: 'green' },
    { label: '✓', color: 'green' }, { label: '✓', color: 'green' }, { label: '✓', color: 'green' }
  ]},
  { name: 'Литвинов Максим', percent: 83, marks: [
    { label: '✓', color: 'green' }, { label: 'н', color: 'red' }, { label: '✓', color: 'green' },
    { label: '✓', color: 'green' }, { label: '✓', color: 'green' }, { label: '✓', color: 'green' }
  ]},
  { name: 'Орлова Ольга', percent: 66, marks: [
    { label: '✓', color: 'green' }, { label: '✓', color: 'green' }, { label: 'б', color: 'yellow' },
    { label: 'б', color: 'yellow' }, { label: '✓', color: 'green' }, { label: '✓', color: 'green' }
  ]},
  { name: 'Рогов Дмитрий', percent: 50, marks: [
    { label: 'н', color: 'red' }, { label: '✓', color: 'green' }, { label: 'н', color: 'red' },
    { label: 'н', color: 'red' }, { label: '✓', color: 'green' }, { label: '✓', color: 'green' }
  ]}
]

const handleExport = () => alert('Экспорт в Excel (демо)')
</script>

<style scoped>
/* стили аналогичны другим страницам */
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.stats-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }

.ledger-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
  overflow-x: auto;
}
.ledger-card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.ledger-header, .ledger-row {
  display: flex; align-items: center; gap: 16px;
  min-width: 800px;
  padding: 12px 16px;
  border-radius: 12px;
}
.ledger-header { background: #F4F7F8; font-size: 12px; font-weight: 700; color: #6D7D79; }
.ledger-row { border-bottom: 1px solid #E3EAE8; }

.athlete-name { width: 180px; font-size: 14px; font-weight: 700; color: #152421; }
.dates { flex: 1; display: flex; justify-content: center; gap: 12px; }
.mark {
  width: 24px; height: 24px; border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  font-size: 11px; font-weight: 700; color: white;
}
.mark.green { background: #2E8B57; }
.mark.red { background: #D64545; }
.mark.yellow { background: #F2B705; }
.percent { width: 80px; text-align: right; font-size: 14px; font-weight: 700; color: #152421; }
</style>