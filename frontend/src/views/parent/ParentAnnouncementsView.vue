<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Важные объявления" 
        subtitle="Информационная лента новостей, изменений и регламентов"
        :show-search="false"
      >
        <template #actions>
          <div class="child-selector">
            <div class="child-avatar">АК</div>
            <span>Арина Ковалева (Плавание)</span>
            <BaseIcon name="chevron-down" :size="14" />
          </div>
        </template>
      </PageHeader>

      <div class="filters">
        <button 
          v-for="f in filters" 
          :key="f"
          class="filter-btn"
          :class="{ active: activeFilter === f }"
          @click="activeFilter = f"
        >
          {{ f }}
        </button>
      </div>

      <div class="feed">
        <div v-for="a in filteredAnnouncements" :key="a.id" class="announce-card" :class="{ important: a.confirmRequired }">
          <div class="announce-header">
            <div class="author">
              <span class="author-name">{{ a.author }}</span>
              <span class="author-date">{{ a.date }}</span>
            </div>
            <div class="badges">
              <span v-if="a.isNew" class="badge-new">Новое</span>
              <span v-if="a.confirmRequired" class="badge-confirm">Требуется согласие</span>
            </div>
          </div>

          <div class="announce-body">
            <h3>{{ a.title }}</h3>
            <p>{{ a.description }}</p>
          </div>

          <div v-if="a.confirmRequired" class="confirm-area">
            <span>Вы ознакомились с объявлением и согласны с условиями?</span>
            <button class="btn-confirm" @click="confirm(a)">Я согласен</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const filters = ['Все', 'Группа А1 (Старшие)', 'Администрация', 'Важные подтверждения']
const activeFilter = ref('Все')

const announcements = [
  {
    id: 1,
    author: 'Администрация',
    date: 'Сегодня, 10:12',
    title: 'Сбор согласий на медицинский осмотр',
    description: 'Уважаемые родители! В связи с плановым медицинским обследованием перед городскими соревнованиями, каждому спортсмену необходимо предоставить согласие установленного образца. Пожалуйста, распечатайте прикрепленный файл и передайте тренеру Алексею Крылову до пятницы.',
    isNew: true,
    confirmRequired: true,
    category: 'Администрация'
  },
  {
    id: 2,
    author: 'Тренер Алексей Крылов',
    date: 'Вчера, 18:30',
    title: 'Изменение расписания тренировок в пятницу (18 сентября)',
    description: "Внимание! Из-за плановой дезинфекции бассейна 'Дельфин' в пятницу, тренировка на воде переносится на 15:30. Зал сухого плавания пройдет по расписанию в 14:30. Пожалуйста, скорректируйте график детей.",
    isNew: true,
    confirmRequired: false,
    category: 'Группа А1 (Старшие)'
  },
  {
    id: 3,
    author: 'Старший тренер',
    date: '12 сентября 2026',
    title: "Осенние спортивные сборы 'Олимпиец' в Сочи",
    description: 'Открыт прием заявок на ежегодные выездные сборы в Сочи. В программе: двухразовые тренировки на воде, ОФП, мастер-классы от мастеров спорта и восстановительные процедуры. Заявку необходимо подать в личном кабинете до 20 сентября.',
    isNew: false,
    confirmRequired: true,
    category: 'Администрация'
  }
]

const filteredAnnouncements = computed(() => {
  if (activeFilter.value === 'Все') return announcements
  if (activeFilter.value === 'Важные подтверждения') {
    return announcements.filter(a => a.confirmRequired)
  }
  return announcements.filter(a => a.category === activeFilter.value)
})

const confirm = (a) => {
  alert(`Согласие с "${a.title}" подтверждено (демо)`)
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

.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-btn {
  padding: 8px 16px; border-radius: 8px;
  background: white; border: 1px solid #E3EAE8;
  font-size: 13px; font-weight: 600; color: #6D7D79;
  cursor: pointer;
}
.filter-btn.active { background: #102522; color: white; border-color: #102522; }

.feed { display: flex; flex-direction: column; gap: 16px; }
.announce-card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  outline: 1px solid #E3EAE8; outline-offset: -1px;
  display: flex; flex-direction: column; gap: 16px;
}
.announce-card.important {
  outline: 2px solid #B7F34B; outline-offset: -2px;
}

.announce-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.author { display: flex; gap: 12px; align-items: center; }
.author-name { font-size: 13px; font-weight: 700; color: #152421; }
.author-date { font-size: 12px; color: #98A6A2; }
.badges { display: flex; gap: 8px; }
.badge-new, .badge-confirm {
  padding: 4px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700;
}
.badge-new { background: #B7F34B; color: #102522; }
.badge-confirm { background: #FFF1D6; color: #8B6914; }

.announce-body h3 { font-size: 18px; font-weight: 700; color: #152421; margin-bottom: 8px; }
.announce-body p { font-size: 13px; color: #6D7D79; line-height: 1.6; }

.confirm-area {
  padding: 16px; background: #F4F7F8;
  border-radius: 12px;
  display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap;
}
.confirm-area span { font-size: 13px; font-weight: 500; color: #152421; }
.btn-confirm {
  padding: 8px 16px; background: #102522;
  border: none; border-radius: 8px;
  font-size: 12px; font-weight: 700; color: white;
  cursor: pointer;
}
</style>