<template>
  <div class="layout">
    <ParentSidebar />
    <main class="workspace">
      <PageHeader 
        title="Связь с секцией" 
        subtitle="Контакты администрации, тренеров плавания и прямая форма обратной связи"
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

      <div class="content-grid">
        <!-- ЛЕВАЯ КОЛОНКА -->
        <div class="left-column">
          <!-- Карточка: Контакты секции -->
          <div class="card">
            <h3>Контакты секции</h3>
            <div class="info-list">
              <div class="info-item">
                <BaseIcon name="map-pin" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">АДРЕС БАССЕЙНА</div>
                  <div class="info-value">Бассейн 'Дельфин', ул. Спортивная, д. 24</div>
                </div>
              </div>
              <div class="info-item">
                <BaseIcon name="phone" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">ТЕЛЕФОН АДМИНИСТРАЦИИ</div>
                  <div class="info-value">+7 911 234-56-78</div>
                </div>
              </div>
              <div class="info-item">
                <BaseIcon name="clock" :size="16" color="#6D7D79" />
                <div class="info-text">
                  <div class="info-label">ВРЕМЯ РАБОТЫ</div>
                  <div class="info-value">Пн – Сб: 08:00 – 21:00 · Вс: Выходной</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Карточка: Тренерский состав -->
          <div class="card">
            <h3>Тренерский состав</h3>
            <div class="coaches-list">
              <div class="coach-item">
                <div class="coach-avatar" style="background: #E9F7D5; color: #2E8B57;">АК</div>
                <div class="coach-details">
                  <div class="coach-name">Алексей Крылов</div>
                  <div class="coach-role">Старший тренер · Плавание</div>
                </div>
                <div class="coach-phone">+7 911 234-56-78</div>
              </div>
              <div class="coach-item">
                <div class="coach-avatar" style="background: #DDECFB; color: #35678E;">МС</div>
                <div class="coach-details">
                  <div class="coach-name">Мария Сидорова</div>
                  <div class="coach-role">Тренер по ОФП · Гимнастика</div>
                </div>
                <div class="coach-phone">+7 911 234-56-78</div>
              </div>
            </div>
          </div>
        </div>

        <!-- ПРАВАЯ КОЛОНКА (Форма) -->
        <div class="right-column">
          <div class="card">
            <h3>Написать сообщение</h3>
            <form @submit.prevent="sendMessage" class="contact-form">
              <div class="form-group">
                <label>Кому</label>
                <select v-model="form.recipient" class="form-select">
                  <option value="admin">Администрации</option>
                  <option value="coach">Тренеру Алексею Крылову</option>
                  <option value="coach2">Тренеру Марии Сидоровой</option>
                </select>
              </div>
              <div class="form-group">
                <label>Тема</label>
                <input type="text" v-model="form.subject" placeholder="Кратко о чем вопрос" class="form-input" />
              </div>
              <div class="form-group">
                <label>Сообщение</label>
                <textarea v-model="form.message" placeholder="Введите ваше сообщение..." class="form-textarea"></textarea>
              </div>
              <button type="submit" class="submit-btn">Отправить сообщение</button>
            </form>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import ParentSidebar from '../../components/layout/ParentSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const form = reactive({
  recipient: 'admin',
  subject: '',
  message: ''
})

const sendMessage = () => {
  console.log('Отправка сообщения:', form)
  alert('Сообщение отправлено!')
  form.subject = ''
  form.message = ''
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

/* Селектор ребенка */
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

/* Сетка */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 1024px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 16px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

/* Левая колонка */
.info-list { display: flex; flex-direction: column; gap: 16px; }
.info-item { display: flex; gap: 12px; align-items: flex-start; }
.info-text { display: flex; flex-direction: column; gap: 2px; }
.info-label { font-size: 12px; font-weight: 600; color: #98A6A2; text-transform: uppercase; }
.info-value { font-size: 14px; font-weight: 500; color: #152421; }

.coaches-list { display: flex; flex-direction: column; gap: 16px; }
.coach-item { display: flex; align-items: center; gap: 12px; }
.coach-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 13px; flex-shrink: 0;
}
.coach-details { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.coach-name { font-size: 14px; font-weight: 600; color: #152421; }
.coach-role { font-size: 12px; color: #6D7D79; }
.coach-phone { font-size: 13px; font-weight: 500; color: #152421; }

/* Правая колонка (форма) */
.contact-form { display: flex; flex-direction: column; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; font-weight: 600; color: #152421; }
.form-select, .form-input, .form-textarea {
  width: 100%; padding: 12px 16px;
  border: 1px solid #E3EAE8; border-radius: 12px;
  font-size: 14px; font-family: inherit; color: #152421;
  outline: none; background: #F9FBFB; box-sizing: border-box;
}
.form-select:focus, .form-input:focus, .form-textarea:focus {
  border-color: #B7F34B; background: white;
}
.form-textarea { min-height: 120px; resize: vertical; }
.submit-btn {
  width: 100%; margin-top: 8px;
  padding: 12px; background: #B7F34B;
  border: none; border-radius: 12px;
  font-size: 14px; font-weight: 700; color: #102522;
  cursor: pointer;
}
.submit-btn:hover { opacity: 0.9; }
</style>