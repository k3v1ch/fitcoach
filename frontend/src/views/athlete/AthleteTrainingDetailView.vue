<template>
  <div class="layout">
    <AthleteSidebar />
    <main class="workspace">
      <PageHeader 
        title="Детали тренировки" 
        subtitle="Информация, план и полезные материалы к занятию"
        :show-search="false"
      >
        <template #actions>
          <div class="coach-card">
            <span>Тренер: Алексей К.</span>
            <span class="online-dot"></span>
          </div>
        </template>
      </PageHeader>

      <div class="content-grid">
        <!-- Left column: Session summary -->
        <div class="left-column">
          <div class="card">
            <div class="session-header">
              <div>
                <h2>Юниоры Бокс · Техническая работа</h2>
                <p class="session-meta">Четверг, 10 сентября · 17:00 - 18:30 · Зал 2</p>
              </div>
              <span class="status-badge">Посещение подтверждено</span>
            </div>

            <div class="divider"></div>

            <h3 class="section-title">План тренировки</h3>
            <div class="training-blocks">
              <div v-for="block in plan" :key="block.id" class="training-block">
                <div class="block-header">
                  <span class="block-title">{{ block.title }}</span>
                  <span class="block-hr">{{ block.hr }}</span>
                </div>
                <p class="block-desc">{{ block.desc }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Right column: Coach feedback -->
        <aside class="right-column">
          <div class="card">
            <h3>Комментарий тренера</h3>
            <p class="feedback-text">{{ feedback }}</p>
            <div class="coach-info">
              <div class="coach-avatar">АК</div>
              <span class="coach-name">Алексей Крылов</span>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import AthleteSidebar from '../../components/layout/AthleteSidebar.vue'
import PageHeader from '../../components/layout/PageHeader.vue'
import BaseIcon from '../../components/ui/BaseIcon.vue'

const plan = [
  {
    id: 1,
    title: '1. Разминка (15 минут)',
    hr: 'ЧСС: 110-130 уд/мин',
    desc: 'Суставная разминка, легкий бег, скакалка (3 раунда по 2 мин), имитация работы в защите перед зеркалом.'
  },
  {
    id: 2,
    title: '2. Основная часть (50 минут)',
    hr: 'ЧСС: 140-165 уд/мин',
    desc: 'Отработка двоек на лапах с тренером, работа в парах (уклон-контрудар), вольные спарринги средней интенсивности (4 раунда).'
  },
  {
    id: 3,
    title: '3. Заминка (15 минут)',
    hr: 'ЧСС: восстановление',
    desc: 'Стретчинг основных мышечных групп, упражнения на расслабление шейно-плечевого отдела, заминка на ковриках.'
  }
]

const feedback = '"Иван, сегодня сделаем упор на уходы с линии атаки и работу джебом. Принеси с собой тренировочные перчатки 14/16 унций."'
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

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
  background: #A44450; border-radius: 50%;
}
.coach-card {
  height: 44px; padding: 0 16px;
  background: white; border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #6D7D79;
}
.online-dot {
  width: 6px; height: 6px;
  background: #B7F34B; border-radius: 50%;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}
@media (max-width: 1024px) { .content-grid { grid-template-columns: 1fr; } }

.card {
  background: white; border-radius: 20px; padding: 24px;
  box-shadow: 0 8px 24px rgba(23, 52, 46, 0.05);
  display: flex; flex-direction: column; gap: 20px;
}
.card h3 { font-size: 18px; font-weight: 700; color: #152421; }

.session-header {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;
}
.session-header h2 { font-size: 22px; font-weight: 700; color: #152421; }
.session-meta { font-size: 14px; color: #6D7D79; margin-top: 4px; }
.status-badge {
  padding: 6px 12px; background: #E9F7D5; color: #2D5B24;
  font-size: 12px; font-weight: 700; border-radius: 999px;
  white-space: nowrap;
}
.divider { height: 1px; background: #E3EAE8; }
.section-title { font-size: 16px; font-weight: 700; color: #152421; }
.training-blocks { display: flex; flex-direction: column; gap: 16px; }
.training-block {
  padding: 16px; background: #F4F7F8; border-radius: 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.block-header {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
}
.block-title { font-size: 14px; font-weight: 700; color: #152421; }
.block-hr { font-size: 12px; color: #6D7D79; }
.block-desc { font-size: 13px; color: #6D7D79; line-height: 1.5; }

.feedback-text {
  font-size: 14px; color: #6D7D79; line-height: 1.5;
  font-style: italic;
}
.coach-info {
  display: flex; align-items: center; gap: 8px;
}
.coach-avatar {
  width: 24px; height: 24px;
  background: #E9F7D5; color: #2D5B24;
  font-size: 10px; font-weight: 700; border-radius: 999px;
  display: flex; justify-content: center; align-items: center;
}
.coach-name { font-size: 12px; font-weight: 600; color: #6D7D79; }
</style>