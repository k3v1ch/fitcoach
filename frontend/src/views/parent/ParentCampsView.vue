<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Сборы и поездки" 
        subtitle="Спортивные выезды, лагеря и тренировочные лагеря"
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

      <div class="camps-list">
        <div v-for="camp in camps" :key="camp.id" class="camp-card">
          <div class="camp-header">
            <div>
              <h3>{{ camp.title }}</h3>
              <div class="camp-meta">
                <span>{{ camp.dates }}</span>
                <span class="dot">•</span>
                <span>{{ camp.location }}</span>
              </div>
            </div>
            <div class="camp-price">
              <div class="price">{{ camp.price }}</div>
              <span class="status-badge" :class="camp.statusClass">{{ camp.status }}</span>
            </div>
          </div>

          <div class="divider"></div>

          <div class="checklist">
            <div class="checklist-label">НЕОБХОДИМЫЕ ДОКУМЕНТЫ И ЧЕК-ЛИСТ:</div>
            <div v-for="item in camp.checklist" :key="item.label" class="checklist-item">
              <div class="checkbox" :class="{ checked: item.done }">
                <BaseIcon v-if="item.done" name="check" :size="12" color="#2E8B57" />
              </div>
              <span>{{ item.label }}</span>
            </div>
          </div>

          <div class="camp-actions">
            <button class="btn-primary" @click="confirmCamp(camp)">Подтвердить участие</button>
            <button class="btn-outline">Оплатить взнос онлайн</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const camps = [
  {
    id: 1,
    title: "Осенний тренировочный сбор 'Олимпиец' Сочи",
    dates: '12 октября - 25 октября 2026',
    location: "Сочи, Спортивный комплекс 'Юг Спорт'",
    price: '45 000 ₽',
    status: 'Не оплачено',
    statusClass: 'status-red',
    checklist: [
      { label: 'Медицинская справка по форме 079/у', done: false },
      { label: 'Страховой спортивный полис', done: true },
      { label: 'Согласие родителя на выезд', done: false }
    ]
  },
  {
    id: 2,
    title: "Интенсив по ОФП и вольной воде 'Красная Поляна'",
    dates: '28 ноября - 05 декабря 2026',
    location: 'Краснодарский край, Красная Поляна',
    price: '32 000 ₽',
    status: 'Зарегистрирован',
    statusClass: 'status-green',
    checklist: [
      { label: 'Справка о неконтакте', done: true },
      { label: 'Спортивная страховка', done: true }
    ]
  }
]

const confirmCamp = (camp) => {
  alert(`Участие в "${camp.title}" подтверждено (демо)`)
}
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

.camps-list { display: flex; flex-direction: column; gap: 20px; }
.camp-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.camp-header { display: flex; justify-content: space-between; gap: 24px; }
.camp-header h3 { font-size: 20px; font-weight: 700; color: #152421; margin-bottom: 6px; }
.camp-meta { display: flex; gap: 12px; font-size: 13px; color: #6D7D79; }
.camp-meta .dot { color: #98A6A2; }

.camp-price { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.price { font-size: 18px; font-weight: 800; color: #152421; }
.status-badge { padding: 4px 12px; border-radius: 999px; font-size: 11px; font-weight: 700; }
.status-red { background: #FCE2E5; color: #D64545; }
.status-green { background: #E9F7D5; color: #2E8B57; }

.divider { height: 1px; background: #E3EAE8; }

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

.checklist { display: flex; flex-direction: column; gap: 12px; }
.checklist-label {
  font-size: 12px; font-weight: 700; color: #6D7D79;
  text-transform: uppercase;
}
.checklist-item {
  display: flex; align-items: center; gap: 12px;
  font-size: 13px; color: #152421;
}
.checkbox {
  width: 20px; height: 20px;
  border: 1.5px solid #98A6A2; border-radius: 6px;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.checkbox.checked { background: #E9F7D5; border-color: #2E8B57; }

.camp-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.btn-primary {
  padding: 10px 16px; background: #B7F34B; border: none;
  border-radius: 8px; font-size: 13px; font-weight: 700;
  color: #102522; cursor: pointer;
}
.btn-outline {
  padding: 10px 16px; background: #F4F7F8; border: none;
  border-radius: 8px; font-size: 13px; font-weight: 600;
  color: #6D7D79; cursor: pointer;
}
</style>