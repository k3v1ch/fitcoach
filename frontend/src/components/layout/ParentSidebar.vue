<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">
        <!-- Inline SVG логотип, чтобы не зависеть от BaseLogo -->
        <svg width="36" height="36" viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="36" height="36" rx="10" fill="#B7F34B"/>
          <path d="M9 14.8352C9.00002 13.658 9.30383 12.5086 9.87132 11.5386C10.4388 10.5687 11.2433 9.82386 12.1784 9.40256C13.1136 8.98127 14.1355 8.90331 15.1092 9.17898C16.0829 9.45466 16.9625 10.071 17.6319 10.9466C17.679 11.0058 17.736 11.0531 17.7993 11.0854C17.8627 11.1177 17.9309 11.1343 18 11.1343C18.069 11.1343 18.1373 11.1177 18.2006 11.0854C18.2639 11.0531 18.3209 11.0058 18.3681 10.9466C19.0354 10.0653 19.9152 9.44379 20.8904 9.16476C21.8657 8.88574 22.8901 8.96245 23.8273 9.38467C24.7646 9.80689 25.5702 10.5546 26.1369 11.5283C26.7037 12.502 27.0047 13.6555 26.9999 14.8352C26.9999 16.2537 26.5371 17.4618 25.8823 18.5381C25.4189 19.2999 24.8595 19.9957 24.3 20.6533L19.3572 26.2736C19.1895 26.5 18.9827 26.6818 18.7506 26.8071C18.5185 26.9323 18.2664 26.9981 18.011 27C17.7557 27.0019 17.5029 26.9399 17.2694 26.8181C17.036 26.6963 16.8273 26.5176 16.6572 26.2937L11.7 20.6533C11.1397 19.9948 10.5794 19.2998 10.1157 18.5381C9.46196 17.4645 9 16.2585 9 14.8352Z" fill="#102522"/>
        </svg>
      </div>
      <div class="brand-copy">
        <div class="brand-name">ФитКоуч</div>
        <div class="brand-sub">Кабинет родителя</div>
      </div>
    </div>

    <OrganizationSwitcher />
    <div v-if="myAthletes.length" class="child-switch">
      <label class="section-label" for="child-select">РЕБЁНОК</label>
      <select v-if="myAthletes.length > 1" id="child-select" class="child-select"
              :value="selectedAthleteId" @change="selectAthlete($event.target.value)">
        <option v-for="a in myAthletes" :key="a.id" :value="a.id">{{ fullName(a) }}</option>
      </select>
      <div v-else class="child-name">{{ fullName(myAthletes[0]) }}</div>
    </div>
    <div v-else-if="childrenLoaded" class="child-switch child-empty">Ребёнок ещё не привязан — обратитесь к тренеру</div>

    <nav class="nav-links">
      <div class="section-label">МЕНЮ РОДИТЕЛЯ</div>
      <router-link to="/parent/dashboard" class="nav-item" active-class="active">
        <BaseIcon name="dashboard" /> <span>Обзор</span>
      </router-link>
      <router-link to="/parent/schedule" class="nav-item" active-class="active">
        <BaseIcon name="calendar" /> <span>Расписание</span>
      </router-link>
      <router-link to="/parent/attendance" class="nav-item" active-class="active">
        <BaseIcon name="check" /> <span>Посещаемость</span>
      </router-link>
      <router-link to="/parent/progress" class="nav-item" active-class="active">
        <BaseIcon name="trending" /> <span>Прогресс</span>
      </router-link>
      <router-link to="/parent/camps" class="nav-item" active-class="active">
        <BaseIcon name="map" /> <span>Сборы</span>
      </router-link>
      <router-link to="/parent/announcements" class="nav-item" active-class="active">
        <BaseIcon name="bell" /> <span>Объявления</span>
      </router-link>
      <router-link to="/parent/payments" class="nav-item" active-class="active">
        <BaseIcon name="credit-card" /> <span>Оплаты</span>
      </router-link>
      <router-link to="/parent/contact" class="nav-item" active-class="active">
        <BaseIcon name="message" /> <span>Связь с секцией</span>
      </router-link>
    </nav>

    <!-- 👇 Блок профиля теперь ссылка на /parent/profile -->
    <router-link to="/parent/profile" class="profile" active-class="profile--active">
      <div class="avatar">{{ initials }}</div>
      <div class="profile-info">
        <div class="profile-name">{{ userName }}</div>
        <div class="profile-role">{{ userRole }}</div>
      </div>
    </router-link>
  </aside>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import BaseIcon from '../ui/BaseIcon.vue'
import OrganizationSwitcher from './OrganizationSwitcher.vue'
import { currentUser, currentOrganization, myAthletes, selectedAthleteId, selectAthlete, loadMyAthletes } from '../../utils/session'
import { fullName, initials as toInitials } from '../../utils/format'

const userName = computed(() => currentUser.value?.fullName?.trim() || currentUser.value?.email || '—')
const userRole = computed(() => ['Родитель', currentOrganization.value?.organizationName].filter(Boolean).join(' · '))
const initials = computed(() => toInitials(userName.value))

// Список детей общий для всех экранов родителя (utils/session.js); экраны следят за selectedAthleteId
const childrenLoaded = ref(false)
onMounted(async () => {
  try {
    if (!myAthletes.value.length) await loadMyAthletes()
  } catch (_) {
    // ошибку покажет сам экран при загрузке данных
  } finally {
    childrenLoaded.value = true
  }
})
</script>

<style scoped>
.sidebar {
  width: 248px;
  height: 100vh;
  background: #102522;
  padding: 28px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  overflow-y: auto;
}
.brand { display: flex; align-items: center; gap: 12px; }
.brand-mark {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #B7F34B;
  border-radius: 12px;
  flex-shrink: 0;
}
.brand-copy { display: flex; flex-direction: column; gap: 2px; }
.brand-name { color: white; font-size: 16px; font-weight: 800; }
.brand-sub { color: #98A6A2; font-size: 11px; }

.nav-links { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.section-label {
  color: #98A6A2;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  padding: 4px 0;
  letter-spacing: 0.5px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  color: #98A6A2;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: background .2s, color .2s;
}
.nav-item:hover { background: rgba(255,255,255,0.05); color: white; }
.nav-item.active { background: #19332F; color: white; font-weight: 600; }

/* 👇 Профиль теперь ссылка */
.profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-top: 8px;
  border-top: 1px solid rgba(255,255,255,0.09);
  padding-top: 16px;
  text-decoration: none;
  border-radius: 10px;
  transition: background .2s;
  cursor: pointer;
}
.profile:hover { background: rgba(255,255,255,0.05); }
.profile--active { background: #19332F; }

.avatar {
  width: 40px;
  height: 40px;
  background: #E9F7D5;
  color: #2D5B24;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.profile-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.profile-name {
  color: white;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.profile-role { color: #98A6A2; font-size: 11px; }
.child-switch { margin: 0 0 16px; display: flex; flex-direction: column; gap: 6px; }
.child-select {
  width: 100%; padding: 8px 10px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.12);
  background: #19332F; color: white; font-size: 13px; cursor: pointer;
}
.child-select:focus { outline: 2px solid #C4F000; outline-offset: 1px; }
.child-name { color: white; font-size: 13px; font-weight: 600; }
.child-empty { color: #98A6A2; font-size: 12px; line-height: 1.4; }
</style>