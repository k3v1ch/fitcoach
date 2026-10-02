<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Финансы" 
        subtitle="Учёт и классификация расходов спортивных секций"
        v-model="searchQuery"
        search-placeholder="Поиск расхода..."
      >
        <template #actions>
          <BaseButton @click="showModal = true">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить расход
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="toolbar">
        <div class="filters">
          <select v-model="filters.period" class="filter-select">
            <option>Сентябрь 2026</option>
            <option>Август 2026</option>
          </select>
          <select v-model="filters.category" class="filter-select">
            <option value="">Все категории</option>
            <option>Аренда</option>
            <option>Инвентарь</option>
            <option>Транспорт</option>
            <option>Прочее</option>
          </select>
        </div>
      </div>

      <div class="table-card">
        <div class="table-header">
          <div class="col num">№</div>
          <div class="col date">ДАТА</div>
          <div class="col cat">КАТЕГОРИЯ</div>
          <div class="col desc">ОПИСАНИЕ</div>
          <div class="col amount">СУММА</div>
          <div class="col doc">ДОКУМЕНТ</div>
        </div>
        <div v-for="exp in filteredExpenses" :key="exp.id" class="table-row">
          <div class="col num">{{ exp.id }}</div>
          <div class="col date">{{ exp.date }}</div>
          <div class="col cat">
            <span class="status-badge" :class="exp.catClass">{{ exp.category }}</span>
          </div>
          <div class="col desc">{{ exp.description }}</div>
          <div class="col amount">{{ exp.amount }} ₽</div>
          <div class="col doc">
            <BaseIcon name="file-text" :size="14" color="#6D7D79" />
            <span class="doc-link">{{ exp.document }}</span>
          </div>
        </div>
        <div v-if="filteredExpenses.length === 0" class="empty-state">
          Расходов по выбранным фильтрам не найдено
        </div>
        <div class="table-footer">
          <div class="footer-total">
            Итого расходов за период: <strong>{{ totalAmount }} ₽</strong>
          </div>
          <div class="footer-detail">Подтверждено документами: 100%</div>
        </div>
      </div>

      <BaseModal v-model="showModal" title="Добавить расход" @submit="handleSubmit">
        <BaseInput v-model="form.category" label="Категория" placeholder="Аренда" />
        <BaseInput v-model="form.description" label="Описание" placeholder="Аренда дорожки №3" />
        <div class="row-2">
          <BaseInput v-model="form.amount" label="Сумма, ₽" placeholder="12000" />
          <BaseInput v-model="form.document" label="Документ" placeholder="Счет-договор №45" />
        </div>
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import BaseModal from '../../../components/ui/BaseModal.vue'
import BaseInput from '../../../components/ui/BaseInput.vue'

const showModal = ref(false)
const searchQuery = ref('')
const form = reactive({ category: '', description: '', amount: '', document: '' })
const filters = reactive({ period: 'Сентябрь 2026', category: '' })

const expenses = ref([
  { id: 1, date: '15.09.2026', category: 'Аренда', catClass: 'cat-blue', description: 'Аренда дорожки №3 в бассейне СКА', amount: '12 000', document: 'Счет-договор №45' },
  { id: 2, date: '12.09.2026', category: 'Инвентарь', catClass: 'cat-yellow', description: 'Покупка плавательных досок и ласт', amount: '5 400', document: 'Накладная Ч-12' },
  { id: 3, date: '10.09.2026', category: 'Транспорт', catClass: 'cat-red', description: 'Автобус для поездки на товарищеские старты', amount: '8 000', document: 'Акт №88' },
  { id: 4, date: '05.09.2026', category: 'Инвентарь', catClass: 'cat-yellow', description: 'Боксерские перчатки (тренировочные)', amount: '4 200', document: 'Чек К-32' },
  { id: 5, date: '02.09.2026', category: 'Прочее', catClass: 'cat-red', description: 'Питьевая вода в зал ОФП', amount: '1 500', document: 'Чек К-12' },
  { id: 6, date: '01.09.2026', category: 'Аренда', catClass: 'cat-blue', description: 'Аренда зала единоборств (Сентябрь)', amount: '15 000', document: 'Договор А-10' }
])

// ЕДИНСТВЕННЫЙ computed для отфильтрованных расходов
const filteredExpenses = computed(() => {
  let result = expenses.value
  if (filters.category) {
    result = result.filter(e => e.category === filters.category)
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(e =>
      e.description.toLowerCase().includes(q) ||
      e.category.toLowerCase().includes(q) ||
      e.document.toLowerCase().includes(q)
    )
  }
  return result
})

const totalAmount = computed(() => {
  return filteredExpenses.value.reduce((sum, e) => {
    return sum + Number(e.amount.replace(/\s/g, ''))
  }, 0).toLocaleString('ru-RU')
})

const handleSubmit = () => {
  alert('Расход добавлен (демо)')
  showModal.value = false
  Object.keys(form).forEach(k => form[k] = '')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }
.toolbar { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 16px; }
.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.filter-select {
  padding: 10px 16px; background: white;
  border: 1px solid #E3EAE8; border-radius: 8px;
  font-size: 13px; color: #6D7D79; cursor: pointer; outline: none;
}
.table-card { background: white; border: 1px solid #E3EAE8; border-radius: 16px; overflow-x: auto; }
.table-header, .table-row {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 24px; min-width: 1000px;
}
.table-header {
  background: #F4F7F8; border-bottom: 1px solid #E3EAE8;
  font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase;
}
.table-row { padding: 16px 24px; border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col { color: #152421; }
.col.num { width: 40px; color: #6D7D79; }
.col.date { width: 100px; color: #6D7D79; }
.col.cat { width: 150px; }
.col.desc { width: 340px; font-weight: 500; font-size: 14px; }
.col.amount { width: 120px; color: #D64545; font-weight: 700; font-size: 14px; }
.col.doc { flex: 1; display: flex; align-items: center; gap: 6px; }
.doc-link { color: #35678E; text-decoration: underline; font-size: 13px; }
.status-badge { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; }
.cat-blue { background: #DDECFB; color: #35678E; }
.cat-yellow { background: #FFF8E6; color: #F2B705; }
.cat-red { background: #FEE2E2; color: #D64545; }
.table-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: #F4F7F8;
  font-size: 13px; flex-wrap: wrap; gap: 12px;
}
.footer-total { color: #152421; }
.footer-total strong { font-weight: 800; color: #D64545; }
.footer-detail { color: #6D7D79; font-size: 12px; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>