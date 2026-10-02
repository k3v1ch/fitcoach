<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader 
        title="Начисления" 
        subtitle="Реестр выставленных начислений спортсменам"
        v-model="searchQuery"
        search-placeholder="Поиск начисления..."
      >
        <template #actions>
          <BaseButton @click="openCreate">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Создать начисление
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <div class="filters">
        <select v-model="filters.paymentStatus" class="filter-select" @change="load">
          <option value="">Любой статус</option>
          <option value="UNPAID">Не оплачено</option>
          <option value="PARTIALLY_PAID">Частично оплачено</option>
          <option value="PAID">Оплачено</option>
        </select>
        <select v-model="filters.type" class="filter-select" @change="load">
          <option value="">Все типы</option>
          <option value="SUBSCRIPTION">Абонемент</option>
          <option value="TRAINING">Занятие</option>
          <option value="EVENT">Мероприятие / Сбор</option>
        </select>
        <label class="checkbox">
          <input type="checkbox" v-model="filters.isOverdue" @change="load" />
          Только просроченные
        </label>
      </div>

      <div v-if="loading" class="empty-state">Загрузка...</div>

      <div v-else class="table-card">
        <div class="table-header">
          <div class="col num">№</div>
          <div class="col date">ДАТА</div>
          <div class="col name">СПОРТСМЕН</div>
          <div class="col title">НАЗНАЧЕНИЕ</div>
          <div class="col amount">СУММА</div>
          <div class="col paid">ОПЛАЧЕНО</div>
          <div class="col status">СТАТУС</div>
          <div class="col actions">ДЕЙСТВИЕ</div>
        </div>

        <div 
          v-for="(c, idx) in filteredCharges" 
          :key="c.id" 
          class="table-row"
        >
          <div class="col num">{{ idx + 1 }}</div>
          <div class="col date">{{ formatDate(c.dueOn) }}</div>
          <div class="col name">{{ c.athleteName || c.athleteId }}</div>
          <div class="col title">
            {{ c.title }}
            <span v-if="c.isOverdue" class="overdue-tag">Просрочено</span>
          </div>
          <div class="col amount">{{ formatMoney(c.amount) }} ₽</div>
          <div class="col paid">{{ formatMoney(c.paidAmount) }} ₽</div>
          <div class="col status">
            <span class="badge" :class="statusClass(c.paymentStatus)">
              {{ statusLabel(c.paymentStatus) }}
            </span>
          </div>
          <div class="col actions">
            <button class="action-btn" @click="openPayment(c)">Оплата</button>
            <button class="action-btn outline" @click="openEdit(c)">Изменить</button>
            <button 
              v-if="c.paymentStatus !== 'PAID'" 
              class="action-btn danger" 
              @click="cancelCharge(c)"
            >
              Отменить
            </button>
          </div>
        </div>

        <div v-if="filteredCharges.length === 0" class="empty-state">
          Начислений не найдено
        </div>

        <div class="table-footer">
          <div class="footer-total">
            Итого начислено: <strong>{{ totalCharged }} ₽</strong>
          </div>
          <div class="footer-detail">
            Оплачено: {{ totalPaid }} ₽ · Долг: {{ totalDebt }} ₽
          </div>
        </div>
      </div>

      <!-- Создание начисления -->
      <BaseModal v-model="showCreate" title="Создать начисление" submit-label="Создать" @submit="submitCreate">
        <BaseInput v-model="createForm.athleteId" label="ID спортсмена (UUID)" />
        <BaseInput v-model="createForm.sectionId" label="ID секции (UUID)" />
        <BaseInput v-model="createForm.title" label="Название" placeholder="Абонемент за Октябрь" />
        <BaseInput v-model="createForm.amount" label="Сумма, ₽" placeholder="12000" />
        <BaseInput v-model="createForm.dueOn" label="Оплатить до (YYYY-MM-DD)" placeholder="2026-10-25" />
        <div class="field">
          <label class="field-label">Тип</label>
          <select v-model="createForm.type" class="role-select">
            <option value="SUBSCRIPTION">Абонемент</option>
            <option value="TRAINING">Занятие</option>
            <option value="EVENT">Мероприятие / Сбор</option>
          </select>
        </div>
        <BaseInput v-if="createForm.type === 'SUBSCRIPTION'" v-model="createForm.periodFrom" label="Период с (YYYY-MM-DD)" />
        <BaseInput v-if="createForm.type === 'SUBSCRIPTION'" v-model="createForm.periodTo" label="Период по (YYYY-MM-DD)" />
      </BaseModal>

      <!-- Платёж -->
      <BaseModal v-model="showPayment" title="Записать платёж" submit-label="Сохранить" @submit="submitPayment">
        <div v-if="selectedCharge" class="info-block">
          <div class="info-row"><span>Начисление</span><strong>{{ selectedCharge.title }}</strong></div>
          <div class="info-row"><span>Остаток</span><strong>{{ formatMoney(selectedCharge.remainingAmount) }} ₽</strong></div>
        </div>
        <BaseInput v-model="paymentForm.amount" label="Сумма, ₽" />
        <BaseInput v-model="paymentForm.paidOn" label="Дата (YYYY-MM-DD)" />
        <div class="field">
          <label class="field-label">Способ</label>
          <select v-model="paymentForm.method" class="role-select">
            <option value="SBP">СБП</option>
            <option value="TRANSFER">Перевод</option>
            <option value="CASH">Наличные</option>
            <option value="OTHER">Другое</option>
          </select>
        </div>
      </BaseModal>

      <!-- Редактирование -->
      <BaseModal v-model="showEdit" title="Изменить начисление" submit-label="Сохранить" @submit="submitEdit">
        <BaseInput v-model="editForm.title" label="Название" />
        <BaseInput v-model="editForm.amount" label="Сумма, ₽" />
        <BaseInput v-model="editForm.dueOn" label="Оплатить до (YYYY-MM-DD)" />
        <BaseInput v-model="editForm.comment" label="Комментарий" />
      </BaseModal>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import BaseModal from '../../../components/ui/BaseModal.vue'
import BaseInput from '../../../components/ui/BaseInput.vue'
import { chargesApi } from '../../../api/charges'
import { paymentsApi } from '../../../api/payments'
import { getOrganizationId } from '../../../utils/session'

const loading = ref(false)
const searchQuery = ref('')
const charges = ref([])

const filters = reactive({
  paymentStatus: '',
  type: '',
  isOverdue: false
})

const filteredCharges = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return charges.value
  return charges.value.filter(c =>
    (c.title || '').toLowerCase().includes(q) ||
    (c.athleteName || '').toLowerCase().includes(q)
  )
})

const totalCharged = computed(() => 
  charges.value.reduce((s, c) => s + Number(c.amount || 0), 0).toLocaleString('ru-RU')
)
const totalPaid = computed(() => 
  charges.value.reduce((s, c) => s + Number(c.paidAmount || 0), 0).toLocaleString('ru-RU')
)
const totalDebt = computed(() => 
  charges.value.reduce((s, c) => s + Number(c.remainingAmount || 0), 0).toLocaleString('ru-RU')
)

// ─────────── load ───────────
async function load() {
  loading.value = true
  try {
    const params = {
      size: 100,
      q: searchQuery.value || undefined,
      paymentStatus: filters.paymentStatus || undefined,
      type: filters.type || undefined,
      isOverdue: filters.isOverdue || undefined
    }
    const res = await chargesApi.list(getOrganizationId(), params)
    charges.value = res.items || res
  } catch (e) {
    console.warn('Не удалось загрузить начисления, используем демо:', e.message)
    charges.value = demoData()
  } finally {
    loading.value = false
  }
}

function demoData() {
  return [
    { id: '1', title: 'Абонемент за Сентябрь', athleteName: 'Ковалева Арина', dueOn: '2026-09-25', amount: 12000, paidAmount: 12000, remainingAmount: 0, paymentStatus: 'PAID', isOverdue: false },
    { id: '2', title: 'Сбор Сочи', athleteName: 'Ковалева Арина', dueOn: '2026-09-25', amount: 45000, paidAmount: 0, remainingAmount: 45000, paymentStatus: 'UNPAID', isOverdue: true },
    { id: '3', title: 'Индивидуальные занятия', athleteName: 'Литвинов Максим', dueOn: '2026-10-01', amount: 8000, paidAmount: 3000, remainingAmount: 5000, paymentStatus: 'PARTIALLY_PAID', isOverdue: false }
  ]
}

// ─────────── Создание ───────────
const showCreate = ref(false)
const createForm = reactive({
  athleteId: '', sectionId: '', title: '', amount: '', dueOn: '',
  type: 'SUBSCRIPTION', periodFrom: '', periodTo: ''
})

function openCreate() {
  Object.keys(createForm).forEach(k => createForm[k] = '')
  createForm.type = 'SUBSCRIPTION'
  showCreate.value = true
}

async function submitCreate() {
  try {
    await chargesApi.create(getOrganizationId(), {
      athleteId: createForm.athleteId,
      sectionId: createForm.sectionId,
      type: createForm.type,
      title: createForm.title,
      amount: createForm.amount,
      dueOn: createForm.dueOn,
      periodFrom: createForm.type === 'SUBSCRIPTION' ? createForm.periodFrom : null,
      periodTo: createForm.type === 'SUBSCRIPTION' ? createForm.periodTo : null,
      trainingId: null,
      eventId: null,
      comment: null
    })
    showCreate.value = false
    load()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}

// ─────────── Платёж ───────────
const showPayment = ref(false)
const selectedCharge = ref(null)
const paymentForm = reactive({ amount: '', paidOn: '', method: 'SBP' })

function openPayment(charge) {
  selectedCharge.value = charge
  paymentForm.amount = String(charge.remainingAmount)
  paymentForm.paidOn = new Date().toISOString().slice(0, 10)
  paymentForm.method = 'SBP'
  showPayment.value = true
}

async function submitPayment() {
  try {
    await paymentsApi.create(getOrganizationId(), {
      chargeId: selectedCharge.value.id,
      amount: paymentForm.amount,
      paidOn: paymentForm.paidOn,
      method: paymentForm.method
    })
    showPayment.value = false
    load()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}

// ─────────── Редактирование ───────────
const showEdit = ref(false)
const editForm = reactive({ title: '', amount: '', dueOn: '', comment: '' })

function openEdit(charge) {
  selectedCharge.value = charge
  editForm.title = charge.title
  editForm.amount = String(charge.amount)
  editForm.dueOn = charge.dueOn
  editForm.comment = charge.comment || ''
  showEdit.value = true
}

async function submitEdit() {
  try {
    await chargesApi.update(getOrganizationId(), selectedCharge.value.id, {
      title: editForm.title,
      amount: editForm.amount,
      dueOn: editForm.dueOn,
      comment: editForm.comment || null
    })
    showEdit.value = false
    load()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}

// ─────────── Отмена ───────────
async function cancelCharge(charge) {
  const reason = prompt('Укажите причину отмены:')
  if (!reason) return
  try {
    await chargesApi.cancel(getOrganizationId(), charge.id, reason)
    load()
  } catch (e) {
    alert(`Ошибка: ${e.message}`)
  }
}

// ─────────── Утилиты ───────────
function formatMoney(n) {
  return Number(n || 0).toLocaleString('ru-RU')
}
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ru-RU')
}
function statusLabel(s) {
  return { PAID: 'Оплачено', UNPAID: 'Не оплачено', PARTIALLY_PAID: 'Частично' }[s] || s
}
function statusClass(s) {
  return { PAID: 'green', UNPAID: 'red', PARTIALLY_PAID: 'yellow' }[s] || 'yellow'
}

onMounted(load)
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F4F7F8; }
.workspace { flex: 1; padding: 28px 32px 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

.filters { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.filter-select { padding: 10px 16px; background: white; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 13px; color: #6D7D79; cursor: pointer; outline: none; }
.checkbox { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #6D7D79; }

.table-card { background: white; border: 1px solid #E3EAE8; border-radius: 16px; overflow-x: auto; }
.table-header, .table-row { display: flex; align-items: center; gap: 16px; padding: 12px 24px; min-width: 1200px; }
.table-header { background: #F4F7F8; border-bottom: 1px solid #E3EAE8; font-size: 12px; font-weight: 700; color: #6D7D79; text-transform: uppercase; }
.table-row { padding: 16px 24px; border-bottom: 1px solid #E3EAE8; font-size: 13px; }
.col { color: #152421; }
.col.num { width: 40px; color: #6D7D79; }
.col.date { width: 100px; color: #6D7D79; }
.col.name { width: 180px; font-weight: 600; }
.col.title { width: 220px; font-weight: 500; position: relative; }
.col.amount { width: 110px; font-weight: 700; }
.col.paid { width: 110px; color: #2E8B57; font-weight: 600; }
.col.status { width: 130px; }
.col.actions { flex: 1; display: flex; justify-content: flex-end; gap: 6px; }
.overdue-tag { display: inline-block; margin-left: 6px; padding: 2px 6px; background: #FCE2E5; color: #D64545; border-radius: 4px; font-size: 10px; font-weight: 700; }
.badge { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 700; }
.badge.green { background: #E9F7D5; color: #2E8B57; }
.badge.yellow { background: #FFF8E6; color: #F2B705; }
.badge.red { background: #FEE2E2; color: #D64545; }
.action-btn { padding: 6px 10px; background: #F4F7F8; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; color: #152421; cursor: pointer; }
.action-btn.outline { background: white; border: 1px solid #E3EAE8; }
.action-btn.danger { background: #FCE2E5; color: #D64545; }
.empty-state { padding: 40px; text-align: center; color: #98A6A2; font-size: 14px; }
.table-footer { display: flex; justify-content: space-between; padding: 16px 24px; background: #F4F7F8; font-size: 13px; flex-wrap: wrap; gap: 12px; }
.footer-total strong { font-weight: 800; }
.footer-detail { color: #6D7D79; font-size: 12px; }
.info-block { padding: 12px; background: #F4F7F8; border-radius: 8px; }
.info-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 12px; color: #6D7D79; font-weight: 500; }
.role-select { padding: 10px 12px; border: 1px solid #E3EAE8; border-radius: 8px; font-size: 14px; background: white; cursor: pointer; outline: none; }
</style>