<template>
  <div ref="root" class="bell">
    <button class="icon-btn" :aria-label="`Уведомления${unread ? `: ${unread} новых` : ''}`" @click="toggle">
      <BaseIcon name="bell" :size="18" color="#152421" />
      <span v-if="unread" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
    </button>

    <div v-if="open" class="panel">
      <div class="panel-head">
        <span class="panel-title">Уведомления</span>
        <button v-if="unread" class="link-btn" :disabled="busy" @click="readAll">Прочитать все</button>
      </div>
      <div v-if="loading && !items.length" class="panel-state">Загрузка…</div>
      <div v-else-if="error" class="panel-state error">{{ error }}</div>
      <div v-else-if="!items.length" class="panel-state">Уведомлений пока нет</div>
      <ul v-else class="list">
        <li v-for="n in items" :key="n.id" class="item" :class="{ unread: !n.readAt }" @click="openItem(n)">
          <span class="dot" />
          <div class="item-body">
            <div class="item-title">{{ n.title }}</div>
            <div class="item-text">{{ n.text }}</div>
            <div class="item-time">{{ formatDateTime(n.createdAt) }}</div>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseIcon from '../ui/BaseIcon.vue'
import { notificationsApi } from '../../api/notifications'
import { currentOrganization } from '../../utils/session'
import { errorText, formatDateTime } from '../../utils/format'

const POLL_MS = 60000

const router = useRouter()
const root = ref(null)
const open = ref(false)
const unread = ref(0)
const items = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
let timer = null

const orgId = () => currentOrganization.value?.organizationId

async function refreshCount() {
  if (!orgId() || document.hidden) return
  try {
    const res = await notificationsApi.list(orgId(), { unread: true, size: 1 })
    unread.value = res.unreadCount
  } catch (_) { /* счётчик не критичен: следующая попытка через минуту */ }
}

async function load() {
  if (!orgId()) return
  loading.value = true
  error.value = ''
  try {
    const res = await notificationsApi.list(orgId(), { size: 20 })
    items.value = res.items
    unread.value = res.unreadCount
  } catch (e) {
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function toggle() {
  open.value = !open.value
  if (open.value) load()
}

// Куда ведёт уведомление: экран, где эту запись видит текущая роль
function linkFor(n) {
  const roles = currentOrganization.value?.roles || []
  const role = roles.includes('TRAINER') || roles.includes('AGENCY') ? 'trainer' : roles.includes('PARENT') ? 'parent' : 'athlete'
  const links = {
    TRAINING: { trainer: `/trainer/trainings/${n.entityId}/report`, parent: '/parent/schedule', athlete: `/athlete/schedule/${n.entityId}` },
    EVENT: { trainer: '/trainer/events', parent: '/parent/camps', athlete: '/athlete/events' },
    ANNOUNCEMENT: { trainer: '/trainer/announcements', parent: '/parent/announcements', athlete: '/athlete/dashboard' }
  }
  return links[n.entityType]?.[role] || null
}

async function openItem(n) {
  if (!n.readAt) {
    try {
      await notificationsApi.read(orgId(), n.id)
      n.readAt = new Date().toISOString()
      unread.value = Math.max(0, unread.value - 1)
    } catch (_) { /* переход важнее отметки */ }
  }
  open.value = false
  const to = linkFor(n)
  if (to && router.currentRoute.value.fullPath !== to) router.push(to)
}

async function readAll() {
  busy.value = true
  try {
    await notificationsApi.readAll(orgId())
    const now = new Date().toISOString()
    items.value.forEach(n => { if (!n.readAt) n.readAt = now })
    unread.value = 0
  } catch (e) {
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}

function onDocumentClick(e) {
  if (open.value && root.value && !root.value.contains(e.target)) open.value = false
}

onMounted(() => {
  refreshCount()
  timer = setInterval(refreshCount, POLL_MS)
  document.addEventListener('click', onDocumentClick)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  document.removeEventListener('click', onDocumentClick)
})
</script>

<style scoped>
.bell { position: relative; }
.icon-btn {
  position: relative;
  width: 44px; height: 44px;
  background: white;
  border: 1px solid #E3EAE8;
  border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  cursor: pointer;
}
.badge {
  position: absolute; top: -4px; right: -4px;
  background: #D64545; color: white;
  font-size: 10px; font-weight: 700;
  padding: 2px 6px; border-radius: 99px;
}
.panel {
  position: absolute; right: 0; top: 52px; z-index: 50;
  width: 380px; max-width: calc(100vw - 32px); max-height: 460px;
  background: white; border-radius: 16px; overflow: hidden;
  box-shadow: 0 12px 32px rgba(23, 52, 46, 0.16); outline: 1px solid #E3EAE8;
  display: flex; flex-direction: column;
}
.panel-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid #F0F4F3;
}
.panel-title { font-size: 14px; font-weight: 700; color: #152421; }
.link-btn { background: none; border: none; font-size: 12px; font-weight: 600; color: #35678E; cursor: pointer; }
.panel-state { padding: 24px 16px; font-size: 13px; color: #98A6A2; text-align: center; }
.panel-state.error { color: #D64545; }
.list { list-style: none; margin: 0; padding: 0; overflow-y: auto; }
.item { display: flex; gap: 10px; padding: 12px 16px; cursor: pointer; border-bottom: 1px solid #F4F7F8; }
.item:hover { background: #F9FBFA; }
.dot { width: 8px; height: 8px; margin-top: 6px; border-radius: 50%; background: transparent; flex-shrink: 0; }
.item.unread .dot { background: #B7F34B; box-shadow: 0 0 0 2px #102522 inset; }
.item-body { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.item-title { font-size: 13px; font-weight: 600; color: #152421; }
.item.unread .item-title { font-weight: 700; }
.item-text { font-size: 12px; color: #6D7D79; line-height: 1.45; overflow-wrap: anywhere; }
.item-time { font-size: 11px; color: #98A6A2; }
</style>
