<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Отчётность" 
        subtitle="Формирование и выгрузка аналитических отчетов"
        :show-bell="true"
      />

      <div class="reports-grid">
        <div v-for="report in reports" :key="report.code" class="report-card">
          <div class="report-header">
            <div class="report-icon">
              <BaseIcon name="file-text" :size="18" color="#2E8B57" />
            </div>
            <div class="report-meta">
              <h3>{{ report.title }}</h3>
              <span class="report-code">{{ report.code }}</span>
            </div>
          </div>
          <p class="report-desc">{{ report.description }}</p>
          <div class="report-form">
            <select v-model="report.format" class="format-select">
              <option>PDF / XLSX</option>
              <option>CSV / PDF</option>
            </select>
            <button class="build-btn" @click="handleBuild(report)">
              Сформировать
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const reports = reactive([
  { code: 'REP-ATT', title: 'Посещаемость', description: 'Анализ посещаемости тренировок по группам, спортсменам и направлениям за период.', format: 'PDF / XLSX' },
  { code: 'REP-PROG', title: 'Прогресс', description: 'Динамика спортивных показателей, нормативов и достижений воспитанников.', format: 'PDF / XLSX' },
  { code: 'REP-FIN', title: 'Финансовый отчёт', description: 'Сводные данные по начислениям, оплатам, задолженностям и расходам CRM.', format: 'PDF / XLSX' },
  { code: 'REP-TRAIN', title: 'Журнал тренировок', description: 'Детализированный реестр проведенных занятий с нагрузками и планами.', format: 'CSV / PDF' },
  { code: 'REP-GROUP', title: 'Сводка по группам', description: 'Общая статистика укомплектованности групп и спортивной занятости тренеров.', format: 'CSV / PDF' },
  { code: 'REP-NORM', title: 'Нормативы', description: 'Оценка результатов сдачи контрольных переводных нормативов по возрастам.', format: 'CSV / PDF' }
])

const handleBuild = (report) => {
  alert(`Формирование отчёта "${report.title}" (демо)`)
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 24px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.reports-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.report-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.report-header { display: flex; align-items: center; gap: 12px; }
.report-icon {
  padding: 8px; background: #E9F7D5;
  border-radius: 8px; display: flex;
  justify-content: center; align-items: center;
}
.report-meta h3 { font-size: 16px; font-weight: 700; color: #152421; }
.report-code { font-size: 11px; color: #98A6A2; }
.report-desc { font-size: 13px; color: #6D7D79; line-height: 1.4; }

.report-form {
  display: flex; gap: 8px; margin-top: auto;
}
.format-select {
  flex: 1; padding: 8px 12px;
  background: #F4F7F8; border: none;
  border-radius: 8px; font-size: 12px;
  font-weight: 600; color: #152421;
  cursor: pointer; outline: none;
}
.build-btn {
  padding: 8px 16px; background: #102522;
  border: none; border-radius: 8px;
  color: #C4F000; font-size: 12px;
  font-weight: 700; cursor: pointer;
}
.build-btn:hover { opacity: 0.9; }
</style>