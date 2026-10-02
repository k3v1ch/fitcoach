<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Управление пользователями" 
        subtitle="Управление правами доступа и ролями"
      >
        <template #actions>
          <BaseButton @click="handleAddUser">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить пользователя
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs-row">
        <button class="tab-btn active">Пользователи</button>
        <button class="tab-btn" @click="$router.push('/trainer/management/directories')">
          Справочники
        </button>
      </div>

      <div class="filter-bar">
        <select v-model="filters.role" class="filter-select">
          <option value="">Все роли</option>
          <option>Тренер</option>
          <option>Родитель</option>
          <option>Спортсмен</option>
        </select>
        <select v-model="filters.status" class="filter-select">
          <option value="">Все статусы</option>
          <option>Активен</option>
          <option>Неактивен</option>
          <option>Заблокирован</option>
        </select>
      </div>

      <div class="table-card">
        <div class="table-header">
          <div class="col user">Пользователь</div>
          <div class="col email">Email</div>
          <div class="col role">Роль</div>
          <div class="col status">Статус</div>
          <div class="col last">Последний вход</div>
          <div class="col actions"></div>
        </div>
        <div v-for="user in filteredUsers" :key="user.id" class="table-row">
          <div class="col user">
            <div class="avatar">{{ user.initials }}</div>
            <span>{{ user.name }}</span>
          </div>
          <div class="col email">{{ user.email }}</div>
          <div class="col role">
            <span class="role-badge" :class="user.roleClass">{{ user.role }}</span>
          </div>
          <div class="col status">
            <span class="dot" :style="{ background: user.statusColor }"></span>
            {{ user.status }}
          </div>
          <div class="col last">{{ user.lastLogin }}</div>
          <div class="col actions">
            <button class="icon-btn">✏️</button>
            <button class="icon-btn">⋯</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import Sidebar from '../../components/layout/Sidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const filters = reactive({ role: '', status: '' })

const users = [
  { id: 1, initials: 'АК', name: 'Крылов Алексей', email: 'krylov@fitcoach.ru', role: 'Тренер', roleClass: 'role-blue', status: 'Активен', statusColor: '#2E8B57', lastLogin: 'Сегодня, 14:32' },
  { id: 2, initials: 'ЕИ', name: 'Иванова Екатерина', email: 'ivanova@fitcoach.ru', role: 'Родитель', roleClass: 'role-orange', status: 'Активен', statusColor: '#2E8B57', lastLogin: 'Сегодня, 11:15' },
  { id: 3, initials: 'МП', name: 'Петров Михаил', email: 'petrov99@fitcoach.ru', role: 'Спортсмен', roleClass: 'role-green', status: 'Активен', statusColor: '#2E8B57', lastLogin: 'Вчера, 18:40' },
  { id: 4, initials: 'ИС', name: 'Сидоров Игорь', email: 'sidorov@fitcoach.ru', role: 'Тренер', roleClass: 'role-blue', status: 'Активен', statusColor: '#2E8B57', lastLogin: '15.09.2026' },
  { id: 5, initials: 'АС', name: 'Смирнова Анна', email: 'smirnova@fitcoach.ru', role: 'Родитель', roleClass: 'role-orange', status: 'Неактивен', statusColor: '#98A6A2', lastLogin: '01.09.2026' },
  { id: 6, initials: 'ДК', name: 'Кузнецов Дмитрий', email: 'kuznetsov@fitcoach.ru', role: 'Спортсмен', roleClass: 'role-green', status: 'Активен', statusColor: '#2E8B57', lastLogin: 'Сегодня, 09:02' },
  { id: 7, initials: 'ОВ', name: 'Васильев Олег', email: 'vasiliev@fitcoach.ru', role: 'Спортсмен', roleClass: 'role-green', status: 'Заблокирован', statusColor: '#D64545', lastLogin: '12.08.2026' },
  { id: 8, initials: 'ОО', name: 'Орлова Ольга', email: 'orlova@fitcoach.ru', role: 'Родитель', roleClass: 'role-orange', status: 'Активен', statusColor: '#2E8B57', lastLogin: 'Вчера, 12:30' }
]

const filteredUsers = computed(() => {
  return users.filter(u => {
    return (!filters.role || u.role === filters.role) &&
           (!filters.status || u.status === filters.status)
  })
})

const handleAddUser = () => alert('Добавление пользователя (демо)')
</script>

<style scoped>
/* стили как раньше */
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.tabs-row { display: flex; gap: 8px; padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.tab-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 14px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.tab-btn.active { background: #102522; color: white; border-color: #102522; }

.filter-bar { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-select {
  padding: 8px 12px; border: 1px solid #E3EAE8;
  border-radius: 10px; background: white;
  font-size: 13px; color: #152421;
  cursor: pointer; outline: none;
}

.table-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 12px;
  overflow-x: auto;
}
.table-header, .table-row {
  display: flex; align-items: center; gap: 16px;
  min-width: 900px;
}
.table-header { padding-bottom: 12px; border-bottom: 1px solid #E3EAE8; }
.table-row { padding: 12px 0; border-bottom: 1px solid #E3EAE8; }

.col { font-size: 14px; color: #152421; }
.col.user { flex: 1; display: flex; align-items: center; gap: 12px; font-weight: 600; }
.col.email { width: 220px; color: #6D7D79; font-size: 13px; }
.col.role { width: 140px; }
.col.status { width: 140px; display: flex; align-items: center; gap: 6px; font-size: 13px; }
.col.last { width: 160px; color: #6D7D79; font-size: 13px; }
.col.actions { width: 100px; display: flex; justify-content: flex-end; gap: 6px; }

.table-header .col {
  font-size: 12px; font-weight: 700; color: #6D7D79;
  text-transform: uppercase;
}

.avatar {
  width: 32px; height: 32px;
  background: #F4F7F8; border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
  font-size: 11px; font-weight: 700;
  flex-shrink: 0;
}

.role-badge {
  display: inline-block; padding: 4px 8px;
  border-radius: 6px; font-size: 11px; font-weight: 700;
}
.role-blue { background: #E3F2FD; color: #2196F3; }
.role-orange { background: #FFF3E0; color: #FF9800; }
.role-green { background: #E8F5E9; color: #2E8B57; }

.dot { width: 6px; height: 6px; border-radius: 50%; }

.icon-btn {
  padding: 6px; background: #F4F7F8;
  border: none; border-radius: 8px;
  cursor: pointer; font-size: 12px;
}
</style>