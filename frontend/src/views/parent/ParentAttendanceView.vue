<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Посещаемость занятий" 
        subtitle="Отслеживайте визиты, отработки и пропущенные тренировки"
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

      <section class="metrics-grid">
        <MetricCard 
          label="Общая статистика" 
          value="94% посещений" 
          note="Всего 28 тренировок из 30 запланированных"
          icon="check"
          color="#E9F7D5"
          icon-color="#2E8B57"
        />
        <MetricCard 
          label="Пропуски" 
          value="2 занятия" 
          note="1 по болезни, 1 без уважительной причины"
          icon="alert"
          color="#FCE2E5"
          icon-color="#D64545"
        />
        <MetricCard 
          label="Доступно отработок" 
          value="1 отработка" 
          note="Можно записаться на компенсационное занятие"
          icon="plus-circle"
          color="#FFF1D6"
          icon-color="#F2B705"
        />
      </section>

      <section class="details-grid">
        <!-- Календарь визитов -->
        <div class="card heatmap-card">
          <h3>Календарь визитов: Сентябрь</h3>
          <div class="heatmap">
            <div class="heatmap-header">
              <span v-for="d in ['Пн','Вт','Ср','Чт','Пт','Сб','Вс']" :key="d">{{ d }}</span>
            </div>
            <div v-for="(week, wi) in weeks" :key="wi" class="heatmap-row">
              <div 
                v-for="(day, di) in week" 
                :key="di" 
                class="heatmap-day"
                :class="day.status"
              ></div>
            </div>
          </div>
          <div class="legend">
            <div class="legend-item"><span class="dot green"></span> Посещено</div>
            <div class="legend-item"><span class="dot yellow"></span> Уважительная причина</div>
            <div class="legend-item"><span class="dot red"></span> Пропущено без причины</div>
          </div>
        </div>

        <!-- История посещений -->
        <div class="card table-card">
          <h3>История посещений</h3>
          <div class="table">
            <div class="table-header">
              <div class="col date">ДАТА</div>
              <div class="col training">ТРЕНИРОВКА</div>
              <div class="col coach">ТРЕНЕР</div>
              <div class="col status">СТАТУС</div>
            </div>
            <div v-for="row in history" :key="row.date" class="table-row">
              <div class="col date">{{ row.date }}</div>
              <div class="col training">{{ row.training }}</div>
              <div class="col coach">{{ row.coach }}</div>
              <div class="col status">
                <span class="status-badge" :class="row.statusClass">{{ row.status }}</span>
              </div>
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

const weeks = [
  [
    { status: 'empty' }, { status: 'empty' }, { status: 'green' }, { status: 'green' }, { status: 'green' }, { status: 'empty' }, { status: 'empty' }
  ],
  [
    { status: 'green' }, { status: 'green' }, { status: 'green' }, { status: 'green' }, { status: 'red' }, { status: 'empty' }, { status: 'empty' }
  ],
  [
    { status: 'green' }, { status: 'green' }, { status: 'green' }, { status: 'yellow' }, { status: 'green' }, { status: 'empty' }, { status: 'empty' }
  ],
  [
    { status: 'green' }, { status: 'green' }, { status: 'gray' }, { status: 'gray' }, { status: 'gray' }, { status: 'empty' }, { status: 'empty' }
  ]
]

const history = [
  { date: '16 сентября', training: 'Плавание (Техника)', coach: 'Алексей К.', status: 'Посещено', statusClass: 'status-green' },
  { date: '14 сентября', training: 'Плавание (Техника)', coach: 'Алексей К.', status: 'Посещено', statusClass: 'status-green' },
  { date: '11 сентября', training: 'Сухое плавание', coach: 'Егор Б.', status: 'Уважительная причина', statusClass: 'status-yellow' },
  { date: '09 сентября', training: 'Плавание (Длинная вода)', coach: 'Алексей К.', status: 'Посещено', statusClass: 'status-green' },
  { date: '07 сентября', training: 'Плавание (Техника)', coach: 'Алексей К.', status: 'Пропуск', statusClass: 'status-red' }
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

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.details-grid {
  display: grid;
  grid-template-columns: 450px 1fr;
  gap: 24px;
}
@media (max-width: 1200px) { .details-grid { grid-template-columns: 1fr; } }

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

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 16px; font-weight: 700; color: #152421; }

.heatmap { display: flex; flex-direction: column; gap: 8px; }
.heatmap-header {
  display: grid; grid-template-columns: repeat(7, 1fr);
  gap: 8px; font-size: 11px; font-weight: 700;
  color: #6D7D79; text-align: center;
}
.heatmap-row {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px;
}
.heatmap-day {
  height: 40px; border-radius: 8px;
}
.heatmap-day.green { background: #2E8B57; }
.heatmap-day.yellow { background: #F2B705; }
.heatmap-day.red { background: #D64545; }
.heatmap-day.gray { background: #F4F7F8; }
.heatmap-day.empty { background: transparent; }

.legend { display: flex; flex-direction: column; gap: 8px; }
.legend-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: #152421;
}
.dot { width: 16px; height: 16px; border-radius: 4px; }
.dot.green { background: #2E8B57; }
.dot.yellow { background: #F2B705; }
.dot.red { background: #D64545; }

.table { display: flex; flex-direction: column; gap: 0; }
.table-header, .table-row {
  display: grid;
  grid-template-columns: 120px 180px 150px 1fr;
  gap: 12px; padding: 12px 0;
  border-bottom: 1px solid #E3EAE8;
}
.table-header {
  font-size: 11px; font-weight: 700;
  color: #6D7D79; text-transform: uppercase;
  padding-bottom: 8px;
}
.table-row { font-size: 13px; align-items: center; }
.col.date { font-weight: 600; color: #152421; }
.col.training { color: #6D7D79; }
.col.coach { color: #6D7D79; }
.col.status { display: flex; }

.status-badge {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
}
.status-green { background: #E9F7D5; color: #2E8B57; }
.status-yellow { background: #FFF1D6; color: #8B6914; }
.status-red { background: #FCE2E5; color: #D64545; }
</style>