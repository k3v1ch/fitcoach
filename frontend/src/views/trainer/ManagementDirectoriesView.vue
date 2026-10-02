<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Справочники системы" 
        subtitle="Настройка основных параметров и классификаторов"
      >
        <template #actions>
          <BaseButton @click="handleAdd">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить запись
          </BaseButton>
        </template>
      </PageHeader>

      <div class="tabs-row">
        <button class="tab-btn" @click="$router.push('/trainer/management/users')">
          Пользователи
        </button>
        <button class="tab-btn active">Справочники</button>
      </div>

      <div class="directories-grid">
        <div v-for="dir in directories" :key="dir.id" class="directory-card">
          <div class="card-header">
            <div class="icon-box">
              <BaseIcon :name="dir.icon" :size="22" color="#102522" />
            </div>
            <span class="count">{{ dir.count }} записей</span>
          </div>
          <div class="card-body">
            <h3>{{ dir.title }}</h3>
            <p>{{ dir.description }}</p>
          </div>
          <div class="card-footer">
            <button class="edit-btn">
              <BaseIcon name="edit" :size="14" color="#152421" />
              Редактировать
            </button>
          </div>
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

const directories = [
  { id: 1, title: 'Виды спорта', description: 'Системный классификатор для группировки данных в профилях и отчётах.', count: 6, icon: 'award' },
  { id: 2, title: 'Типы тренировок', description: 'Системный классификатор для группировки данных в профилях и отчётах.', count: 8, icon: 'activity' },
  { id: 3, title: 'Площадки и залы', description: 'Системный классификатор для группировки данных в профилях и отчётах.', count: 4, icon: 'map' },
  { id: 4, title: 'Категории расходов', description: 'Системный классификатор для группировки данных в профилях и отчётах.', count: 5, icon: 'credit-card' },
  { id: 5, title: 'Типы документов', description: 'Системный классификатор для группировки данных в профилях и отчётах.', count: 3, icon: 'file-text' },
  { id: 6, title: 'Статусы спортсменов', description: 'Системный классификатор для группировки данных в профилях и отчётах.', count: 4, icon: 'users' }
]

const handleAdd = () => alert('Добавление записи (демо)')
</script>

<style scoped>
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

.directories-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.directory-card {
  background: white; border: 1px solid #E3EAE8;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.icon-box {
  width: 48px; height: 48px;
  background: #E8F5E9; border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
}
.count {
  padding: 4px 10px; background: #F4F7F8;
  border-radius: 20px; font-size: 12px;
  font-weight: 600; color: #6D7D79;
}
.card-body { display: flex; flex-direction: column; gap: 8px; }
.card-body h3 { font-size: 18px; font-weight: 700; color: #152421; }
.card-body p { font-size: 13px; color: #6D7D79; }

.card-footer { padding-top: 16px; border-top: 1px solid #E3EAE8; }
.edit-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 13px; font-weight: 700; color: #152421;
  cursor: pointer;
}
</style>