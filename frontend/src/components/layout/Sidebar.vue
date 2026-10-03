<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">
        <svg width="36" height="36" viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="36" height="36" rx="10" fill="#B7F34B"/>
          <path d="M9 14.8352C9.00002 13.658 9.30383 12.5086 9.87132 11.5386C10.4388 10.5687 11.2433 9.82386 12.1784 9.40256C13.1136 8.98127 14.1355 8.90331 15.1092 9.17898C16.0829 9.45466 16.9625 10.071 17.6319 10.9466C17.679 11.0058 17.736 11.0531 17.7993 11.0854C17.8627 11.1177 17.9309 11.1343 18 11.1343C18.069 11.1343 18.1373 11.1177 18.2006 11.0854C18.2639 11.0531 18.3209 11.0058 18.3681 10.9466C19.0354 10.0653 19.9152 9.44379 20.8904 9.16476C21.8657 8.88574 22.8901 8.96245 23.8273 9.38467C24.7646 9.80689 25.5702 10.5546 26.1369 11.5283C26.7037 12.502 27.0047 13.6555 26.9999 14.8352C26.9999 16.2537 26.5371 17.4618 25.8823 18.5381C25.4189 19.2999 24.8595 19.9957 24.3 20.6533L19.3572 26.2736C19.1895 26.5 18.9827 26.6818 18.7506 26.8071C18.5185 26.9323 18.2664 26.9981 18.011 27C17.7557 27.0019 17.5029 26.9399 17.2694 26.8181C17.036 26.6963 16.8273 26.5176 16.6572 26.2937L11.7 20.6533C11.1397 19.9948 10.5794 19.2998 10.1157 18.5381C9.46196 17.4645 9 16.2585 9 14.8352Z" fill="#102522"/>
        </svg>
      </div>
      <div class="brand-copy">
        <div class="brand-name">ФитКоуч</div>
        <div class="brand-sub">CRM для тренера</div>
      </div>
    </div>

    <nav class="nav-links">
      <router-link to="/trainer/dashboard" class="nav-item" active-class="active">
        <BaseIcon name="dashboard" :color="isActive('/trainer/dashboard') ? '#B7F34B' : '#98A6A2'" />
        <span>Dashboard</span>
      </router-link>
      <router-link to="/trainer/sections" class="nav-item" active-class="active">
        <BaseIcon name="layers" :color="isActive('/trainer/sections') ? '#B7F34B' : '#98A6A2'" />
        <span>Секции</span>
      </router-link>
      <router-link to="/trainer/groups" class="nav-item" active-class="active">
        <BaseIcon name="users" :color="isActive('/trainer/groups') ? '#B7F34B' : '#98A6A2'" />
        <span>Группы</span>
      </router-link>
      <router-link to="/trainer/athletes" class="nav-item" active-class="active">
        <BaseIcon name="user" :color="isActive('/trainer/athletes') ? '#B7F34B' : '#98A6A2'" />
        <span>Спортсмены</span>
      </router-link>
      <router-link to="/trainer/schedule" class="nav-item" active-class="active">
        <BaseIcon name="calendar" :color="isActive('/trainer/schedule') ? '#B7F34B' : '#98A6A2'" />
        <span>Расписание</span>
      </router-link>
      <router-link to="/trainer/trainings" class="nav-item" active-class="active">
        <BaseIcon name="activity" :color="isActive('/trainer/trainings') ? '#B7F34B' : '#98A6A2'" />
        <span>Тренировки</span>
      </router-link>
      <router-link to="/trainer/attendance" class="nav-item" active-class="active">
        <BaseIcon name="check" :color="isActive('/trainer/attendance') ? '#B7F34B' : '#98A6A2'" />
        <span>Посещаемость</span>
      </router-link>
      <router-link to="/trainer/progress" class="nav-item" active-class="active">
        <BaseIcon name="trending" :color="isActive('/trainer/progress') ? '#B7F34B' : '#98A6A2'" />
        <span>Прогресс</span>
      </router-link>
      <router-link to="/trainer/events" class="nav-item" active-class="active">
        <BaseIcon name="map" :color="isActive('/trainer/events') ? '#B7F34B' : '#98A6A2'" />
        <span>Сборы</span>
      </router-link>
      <router-link to="/trainer/finance" class="nav-item" active-class="active">
        <BaseIcon name="credit-card" :color="isActive('/trainer/finance') ? '#B7F34B' : '#98A6A2'" />
        <span>Финансы</span>
      </router-link>
      <router-link to="/trainer/reports" class="nav-item" active-class="active">
        <BaseIcon name="file-text" :color="isActive('/trainer/reports') ? '#B7F34B' : '#98A6A2'" />
        <span>Отчётность</span>
      </router-link>
      <router-link to="/trainer/management/users" class="nav-item" active-class="active">
        <BaseIcon name="sliders" :color="isActive('/trainer/management') ? '#B7F34B' : '#98A6A2'" />
        <span>Управление</span>
      </router-link>
    </nav>

    <!-- 👇 ПРОФИЛЬ ТРЕНЕРА — теперь ссылка -->
    <router-link to="/trainer/profile" class="profile" active-class="profile--active">
      <div class="avatar">{{ userInitials }}</div>
      <div class="profile-info">
        <div class="profile-name">{{ userName }}</div>
        <div class="profile-role">{{ userRole }}</div>
      </div>
    </router-link>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BaseIcon from '../ui/BaseIcon.vue'
import { currentUser, currentOrganization, hasRole } from '../../utils/session'
import { initials } from '../../utils/format'

const userName = computed(() => currentUser.value?.fullName?.trim() || currentUser.value?.email || '—')
const userInitials = computed(() => initials(userName.value))
const userRole = computed(() => {
  const role = hasRole('TRAINER') ? 'Тренер' : hasRole('AGENCY') ? 'Представитель ведомства' : ''
  return [role, currentOrganization.value?.organizationName].filter(Boolean).join(' · ')
})

const route = useRoute()
const isActive = (path) => route.path.startsWith(path)
</script>

<style scoped>
.sidebar {
  width: 248px;
  height: 100vh;
  background: #102522;
  padding: 24px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  overflow-y: auto;
}
.brand { display: flex; align-items: center; gap: 12px; }
.brand-mark { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.brand-name { color: white; font-size: 16px; font-weight: 800; }
.brand-sub { color: #98A6A2; font-size: 11px; }
.nav-links { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #98A6A2;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
}
.nav-item:hover { background: rgba(255, 255, 255, 0.05); color: white; }
.nav-item.active { background: #19332F; color: #B7F34B; font-weight: 600; }

/* 👇 Профиль-ссылка */
.profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 12px 0;
  margin-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
  text-decoration: none;
  border-radius: 10px;
  transition: background 0.2s;
  cursor: pointer;
}
.profile:hover { background: rgba(255, 255, 255, 0.05); }
.profile--active { background: #19332F; }

.avatar {
  width: 36px; height: 36px;
  background: #E9F7D5;
  border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
  color: #2D5B24; font-size: 13px; font-weight: 700;
  flex-shrink: 0;
}
.profile-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.profile-name { color: white; font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.profile-role { color: #98A6A2; font-size: 11px; }
</style>