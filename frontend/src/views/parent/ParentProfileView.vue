<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader
        title="Профиль родителя"
        subtitle="Контакты, семейные связи и уведомления"
        :show-search="false"
      >
        <template #actions>
            <button class="icon-btn" title="Уведомления">
                <BaseIcon name="bell" :size="18" color="#152421" />
                <span class="icon-badge"></span>
            </button>
            <BaseButton @click="showEditModal = true">
                <BaseIcon name="edit" :size="16" color="#102522" />
                Редактировать
            </BaseButton>
        </template>
      </PageHeader>

      <!-- Profile Summary Card -->
      <section class="profile-summary-card card">
        <div class="avatar-large">АК</div>
        <div class="profile-info">
          <div class="profile-badges">
            <span class="badge badge--dark">РОДИТЕЛЬ</span>
            <span class="badge badge--green">Доступ подтверждён</span>
          </div>
          <h2 class="profile-name">Алексей Андреевич Ковалев</h2>
          <div class="profile-contacts">
            <div class="contact-item">
              <BaseIcon name="mail" :size="15" color="#6D7D79" />
              <span>alexey.kovalev@gmail.com</span>
            </div>
            <div class="contact-item">
              <BaseIcon name="phone" :size="15" color="#6D7D79" />
              <span>+7 (911) 204-75-33</span>
            </div>
          </div>
        </div>
        <div class="profile-facts">
          <div class="fact-row">
            <span class="fact-label">Статус профиля</span>
            <span class="fact-value fact-value--green">Подтверждён</span>
          </div>
          <div class="fact-row">
            <span class="fact-label">Последний вход</span>
            <span class="fact-value">Сегодня, 09:42</span>
          </div>
        </div>
      </section>

      <!-- Main Content Grid -->
      <div class="content-grid">
        <!-- Left Column: Details -->
        <div class="main-column">
          <!-- Contact Info Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Контактная информация</h3>
              <p class="card-subtitle">Основные данные представителя</p>
            </div>
            <div class="info-grid">
              <div class="info-column">
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="phone" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Телефон</span>
                    <span class="field-value">+7 (911) 204-75-33</span>
                  </div>
                </div>
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="send" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Предпочтительный канал</span>
                    <span class="field-value">Telegram</span>
                  </div>
                </div>
              </div>
              <div class="info-column">
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="users" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Родство</span>
                    <span class="field-value">Отец</span>
                  </div>
                </div>
                <div class="info-field">
                  <div class="field-icon"><BaseIcon name="map-pin" :size="16" color="#6D7D79" /></div>
                  <div class="field-copy">
                    <span class="field-label">Город</span>
                    <span class="field-value">Санкт-Петербург</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Linked Children Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Связанные дети</h3>
              <p class="card-subtitle">2 ребёнка имеют доступ к секциям</p>
            </div>
            <div class="children-list">
              <div v-for="child in children" :key="child.id" class="linked-child-item">
                <div class="child-avatar" :class="child.avatarColor">{{ child.initials }}</div>
                <div class="child-details">
                  <span class="child-name">{{ child.name }}</span>
                  <span class="child-age">{{ child.age }}</span>
                </div>
                <div class="child-program">
                  <span class="program-name">{{ child.program }}</span>
                  <span class="program-group">{{ child.group }}</span>
                </div>
                <BaseButton variant="outline" class="open-btn">Открыть</BaseButton>
              </div>
            </div>
          </div>

          <!-- Emergency Contact Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Экстренный контакт</h3>
            </div>
            <div class="emergency-contact">
              <div class="emergency-icon"><BaseIcon name="siren" :size="19" color="#D64545" /></div>
              <div class="emergency-details">
                <span class="emergency-name">Елена Викторовна Ковалева</span>
                <span class="emergency-info">Мама · +7 (921) 630-44-08</span>
              </div>
              <span class="tag tag--green">Основной</span>
            </div>
          </div>
        </div>

        <!-- Right Column: Settings -->
        <div class="settings-column">
          <!-- Notifications Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Настройки уведомлений</h3>
              <p class="card-subtitle">Выберите, какие события не пропустить</p>
            </div>
            <div class="notification-settings">
              <div v-for="setting in notificationSettings" :key="setting.id" class="notification-setting">
                <div class="setting-copy">
                  <span class="setting-title">{{ setting.title }}</span>
                  <span class="setting-description">{{ setting.description }}</span>
                </div>
                <label class="toggle-switch">
                  <input type="checkbox" v-model="setting.enabled">
                  <span class="slider"></span>
                </label>
              </div>
            </div>
          </div>

          <!-- Family Access Status Card -->
          <div class="status-card">
            <div class="status-icon">
              <BaseIcon name="users-round" :size="19" color="#B7F34B" />
            </div>
            <div class="status-copy">
              <span class="status-label">Семейный доступ</span>
              <span class="status-value">2 профиля ребёнка</span>
              <span class="status-description">Арина · Плавание, Максим · ОФП</span>
            </div>
            <a href="#" class="status-action">Управлять →</a>
          </div>

          <!-- Security Card -->
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Безопасность и доступ</h3>
            </div>
            <div class="security-actions">
              <div class="security-action-item">
                <BaseIcon name="key" :size="17" color="#152421" />
                <div class="action-copy">
                  <span class="action-title">Изменить пароль</span>
                  <span class="action-description">Обновлён 3 месяца назад</span>
                </div>
                <BaseIcon name="chevron-right" :size="15" color="#98A6A2" />
              </div>
              <div class="security-action-item">
                <BaseIcon name="shield-check" :size="17" color="#152421" />
                <div class="action-copy">
                  <span class="action-title">Двухфакторная защита</span>
                  <span class="action-description">Подключена по SMS</span>
                </div>
                <BaseIcon name="chevron-right" :size="15" color="#98A6A2" />
              </div>
              <div class="security-action-item security-action-item--danger">
                <BaseIcon name="log-out" :size="17" color="#D64545" />
                <div class="action-copy">
                  <span class="action-title">Выйти из аккаунта</span>
                  <span class="action-description">Завершить текущую сессию</span>
                </div>
                <BaseIcon name="chevron-right" :size="15" color="#98A6A2" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Modal -->
      <BaseModal v-model="showEditModal" title="Редактировать профиль" @submit="handleSaveProfile">
        <BaseInput v-model="editForm.fullName" label="ФИО" placeholder="Алексей Андреевич Ковалев" />
        <BaseInput v-model="editForm.phone" label="Телефон" placeholder="+7 (911) 204-75-33" />
        <BaseInput v-model="editForm.channel" label="Предпочтительный канал" placeholder="Telegram" />
        <BaseInput v-model="editForm.relationship" label="Родство" placeholder="Отец" />
        <BaseInput v-model="editForm.city" label="Город" placeholder="Санкт-Петербург" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseButton from '../../components/ui/BaseButton.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'
import BaseModal from '../../components/ui/BaseModal.vue'
import BaseInput from '../../components/ui/BaseInput.vue'

const showEditModal = ref(false)

const editForm = reactive({
  fullName: 'Алексей Андреевич Ковалев',
  phone: '+7 (911) 204-75-33',
  channel: 'Telegram',
  relationship: 'Отец',
  city: 'Санкт-Петербург'
})

const handleSaveProfile = () => {
  alert('Профиль сохранён (демо)')
  showEditModal.value = false
}

const children = ref([])
const notificationSettings = ref([])

onMounted(() => {
  children.value = [
    { id: 1, name: 'Арина Ковалева', age: '12 лет · дочь', initials: 'АК', avatarColor: 'blue', program: 'Плавание', group: 'Группа A1 · старшие' },
    { id: 2, name: 'Максим Ковалев', age: '9 лет · сын', initials: 'МК', avatarColor: 'green', program: 'ОФП', group: 'Группа B2 · начальная' },
  ]

  notificationSettings.value = [
    { id: 'schedule', title: 'Изменения расписания', description: 'По занятиям обоих детей', enabled: true },
    { id: 'announcements', title: 'Объявления секций', description: 'Важные сообщения и сборы', enabled: true },
    { id: 'payments', title: 'Оплаты и начисления', description: 'Квитанции и напоминания', enabled: true },
    { id: 'reports', title: 'Отчёты о прогрессе', description: 'Еженедельная сводка', enabled: false },
  ]
})
</script>

<style scoped>
/* --- Стили аналогичны профилю тренера, с небольшими отличиями --- */
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.card { background: white; border: 1px solid #E3EAE8; border-radius: 20px; padding: 24px; box-shadow: 0 8px 24px rgba(23, 52, 46, 0.04); display: flex; flex-direction: column; gap: 18px; }
.profile-summary-card { display: flex; align-items: center; gap: 20px; }
.avatar-large { width: 104px; height: 104px; background: #E9F7D5; border-radius: 999px; display: flex; justify-content: center; align-items: center; font-size: 30px; font-weight: 800; color: #2E8B57; flex-shrink: 0; }
.profile-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.profile-badges { display: flex; gap: 8px; }
.badge { padding: 5px 10px; border-radius: 999px; font-size: 10px; font-weight: 700; text-transform: uppercase; }
.badge--dark { background: #102522; color: #B7F34B; }
.badge--green { background: #E9F7D5; color: #2E8B57; }
.profile-name { font-size: 24px; font-weight: 700; color: #152421; }
.profile-contacts { display: flex; gap: 20px; align-items: center; }
.contact-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #6D7D79; }
.profile-facts { width: 230px; display: flex; flex-direction: column; gap: 10px; }
.fact-row { display: flex; justify-content: space-between; align-items: center; }
.fact-label { font-size: 11px; color: #6D7D79; }
.fact-value { font-size: 11px; font-weight: 600; color: #152421; }
.fact-value--green { color: #2E8B57; font-weight: 700; }
.content-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
@media (max-width: 1200px) { .content-grid { grid-template-columns: 1fr; } }
.main-column, .settings-column { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; flex-direction: column; gap: 4px; }
.card-title { font-size: 18px; font-weight: 700; color: #152421; }
.card-subtitle { font-size: 12px; color: #6D7D79; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.info-column { display: flex; flex-direction: column; }
.info-field { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #E3EAE8; }
.info-field:last-child { border-bottom: none; }
.field-icon { width: 34px; height: 34px; background: #F0F4F4; border-radius: 8px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.field-copy { display: flex; flex-direction: column; gap: 3px; }
.field-label { font-size: 10px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.field-value { font-size: 14px; font-weight: 600; color: #152421; }
.children-list { display: flex; flex-direction: column; gap: 10px; }
.linked-child-item { display: flex; align-items: center; gap: 12px; padding: 14px; background: #F0F4F4; border-radius: 12px; }
.child-avatar { width: 44px; height: 44px; border-radius: 999px; display: flex; justify-content: center; align-items: center; font-size: 13px; font-weight: 700; flex-shrink: 0; }
.child-avatar.blue { background: #DDECFB; color: #35678E; }
.child-avatar.green { background: #E9F7D5; color: #2E8B57; }
.child-details { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.child-name { font-size: 14px; font-weight: 700; color: #152421; }
.child-age { font-size: 11px; color: #6D7D79; }
.child-program { width: 180px; display: flex; flex-direction: column; gap: 3px; }
.program-name { font-size: 12px; font-weight: 600; color: #152421; }
.program-group { font-size: 11px; color: #6D7D79; }
.open-btn { height: 44px; padding: 0 16px; font-size: 13px; font-weight: 700; }
.emergency-contact { display: flex; align-items: center; gap: 14px; }
.emergency-icon { width: 42px; height: 42px; background: #FCE2E5; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.emergency-details { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.emergency-name { font-size: 14px; font-weight: 700; color: #152421; }
.emergency-info { font-size: 11px; color: #6D7D79; }
.tag { padding: 5px 9px; border-radius: 999px; font-size: 10px; font-weight: 700; }
.tag--green { background: #E9F7D5; color: #2E8B57; }
.notification-settings { display: flex; flex-direction: column; gap: 16px; }
.notification-setting { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.setting-copy { display: flex; flex-direction: column; gap: 3px; }
.setting-title { font-size: 13px; font-weight: 600; color: #152421; }
.setting-description { font-size: 11px; color: #6D7D79; }
.toggle-switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #E3EAE8; transition: .4s; border-radius: 24px; }
.slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 2px; bottom: 2px; background-color: white; transition: .4s; border-radius: 50%; }
input:checked + .slider { background-color: #B7F34B; }
input:checked + .slider:before { transform: translateX(20px); background-color: #102522; }
.status-card { background: #102522; padding: 18px; border-radius: 16px; display: flex; align-items: center; gap: 14px; }
.status-icon { width: 42px; height: 42px; background: #19332F; border-radius: 12px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; }
.status-copy { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.status-label { font-size: 10px; font-weight: 700; color: #98A6A2; text-transform: uppercase; }
.status-value { font-size: 16px; font-weight: 700; color: white; }
.status-description { font-size: 10px; color: #98A6A2; }
.status-action { font-size: 11px; font-weight: 700; color: #B7F34B; text-decoration: none; }
.security-actions { display: flex; flex-direction: column; gap: 8px; }
.security-action-item { display: flex; align-items: center; gap: 10px; padding: 12px; background: #F0F4F4; border-radius: 12px; cursor: pointer; transition: background 0.2s; }
.security-action-item:hover { background: #E9F0EE; }
.action-copy { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.action-title { font-size: 13px; font-weight: 600; color: #152421; }
.action-description { font-size: 10px; color: #6D7D79; }
.security-action-item--danger .action-title { color: #D64545; }
.icon-btn { position: relative; width: 44px; height: 44px; background: white; border: 1px solid #E3EAE8; border-radius: 12px; display: flex; justify-content: center; align-items: center; cursor: pointer; }
.icon-btn {
  position: relative;
  width: 44px;
  height: 44px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  flex-shrink: 0;
}

.icon-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: #D64545;
  border-radius: 50%;
  border: 2px solid white;
  box-sizing: content-box;
  pointer-events: none;
}
</style>