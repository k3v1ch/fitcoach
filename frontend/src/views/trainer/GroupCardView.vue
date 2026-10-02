<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Группа А1 (Старшие)" 
        subtitle="Специализация: спортивное плавание · 12 атлетов"
        :show-back="true"
        :show-search="false"
        @back="$router.push('/trainer/groups')"
      />

      <div class="tabs-row">
        <div class="tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab"
            class="tab-btn"
            :class="{ active: activeTab === tab }"
            @click="activeTab = tab"
          >
            {{ tab }}
          </button>
        </div>
        <div class="actions">
          <button class="btn-outline">Редактировать группу</button>
          <button class="btn-dark">Написать всем</button>
        </div>
      </div>

      <div class="main-blocks">
        <!-- Состав группы -->
        <div class="card athletes-card">
          <h3>Зарегистрированные спортсмены</h3>
          <div class="athlete-list">
            <div v-for="athlete in athletes" :key="athlete.id" class="athlete-line">
              <div class="avatar" :style="{ background: athlete.avatarBg, color: athlete.avatarColor }">
                {{ athlete.initials }}
              </div>
              <div class="details">
                <div class="name">{{ athlete.name }}</div>
                <div class="rank">{{ athlete.rank }}</div>
              </div>
              <div class="attendance">
                <div class="label">ПОСЕЩАЕМОСТЬ</div>
                <div class="value">{{ athlete.attendance }}%</div>
              </div>
              <div class="note">{{ athlete.note }}</div>
            </div>
          </div>
        </div>

        <!-- Боковая статистика -->
        <div class="sidebar-stats">
          <div class="card info-card">
            <h3>Статистика посещаемости</h3>
            <div class="radial">
              <div class="circle"></div>
              <div>
                <div class="percent">88.4%</div>
                <div class="desc">Средний показатель группы</div>
              </div>
            </div>
          </div>

          <div class="card next-trainings">
            <h3>Ближайшие занятия</h3>
            <div class="training-item">
              <div class="date">Пятница, 18 сентября</div>
              <div class="info">09:30 · Силовая тренировка (Зал 1)</div>
            </div>
            <div class="training-item">
              <div class="date">Понедельник, 21 сентября</div>
              <div class="info">09:00 · Техника длинного гребка (Бассейн)</div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'

const tabs = ['Состав группы', 'Расписание', 'Посещаемость', 'Мероприятия']
const activeTab = ref('Состав группы')

const athletes = [
  { id: 1, initials: 'АК', name: 'Ковалева Арина', rank: '1-й юношеский разряд', attendance: 92, note: 'Отличный прогресс', avatarBg: '#E9F7D5', avatarColor: '#2D5B24' },
  { id: 2, initials: 'МЛ', name: 'Литвинов Максим', rank: 'КМС', attendance: 88, note: 'Сфокусирован на выносливости', avatarBg: '#DDECFB', avatarColor: '#35678E' },
  { id: 3, initials: 'ОО', name: 'Орлова Ольга', rank: '2-й взрослый разряд', attendance: 75, note: 'Есть пропуски по болезни', avatarBg: '#FFF8E6', avatarColor: '#F2B705' },
  { id: 4, initials: 'ДР', name: 'Рогов Дмитрий', rank: 'Без разряда', attendance: 100, note: 'Посетил все тренировки', avatarBg: '#FCE2E5', avatarColor: '#D64545' }
]
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.tabs-row {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 16px;
}
.tabs { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.actions { display: flex; gap: 12px; }
.btn-outline {
  padding: 10px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 600; color: #152421;
  cursor: pointer;
}
.btn-dark {
  padding: 10px 16px; background: #102522;
  border: none; border-radius: 12px;
  font-size: 13px; font-weight: 700; color: white;
  cursor: pointer;
}

.main-blocks {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 1024px) {
  .main-blocks { grid-template-columns: 1fr; }
}

.card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.athlete-list { display: flex; flex-direction: column; gap: 12px; }
.athlete-line {
  display: flex; align-items: center; gap: 16px;
  padding-bottom: 12px; border-bottom: 1px solid #E3EAE8;
}
.avatar {
  width: 40px; height: 40px; border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
  font-size: 13px; font-weight: 700;
  flex-shrink: 0;
}
.details { width: 220px; }
.name { font-size: 15px; font-weight: 700; color: #152421; }
.rank { font-size: 12px; color: #6D7D79; }

.attendance { width: 100px; }
.attendance .label {
  font-size: 11px; font-weight: 700; color: #98A6A2;
  text-transform: uppercase;
}
.attendance .value { font-size: 14px; font-weight: 700; color: #2E8B57; }

.note { flex: 1; font-size: 13px; color: #6D7D79; }

.sidebar-stats { display: flex; flex-direction: column; gap: 20px; }

.radial {
  display: flex; align-items: center; gap: 16px;
}
.circle {
  width: 72px; height: 72px;
  background: #E9F7D5; border-radius: 9999px;
  border: 6px solid #B7F34B;
  flex-shrink: 0;
}
.percent { font-size: 22px; font-weight: 700; color: #152421; }
.desc { font-size: 13px; color: #6D7D79; }

.training-item {
  padding: 12px; background: #F4F7F8;
  border-radius: 12px; display: flex;
  flex-direction: column; gap: 4px;
}
.training-item .date { font-size: 14px; font-weight: 700; color: #152421; }
.training-item .info { font-size: 13px; color: #6D7D79; }
</style>