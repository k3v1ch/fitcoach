<template>
  <div class="layout">
    <Sidebar />
    <main class="workspace">
      <PageHeader
        title="Финансы"
        subtitle="Учёт и классификация расходов спортивных секций"
        :show-search="false"
      >
        <template #actions>
          <BaseButton disabled title="Учёт расходов появится позже">
            <BaseIcon name="plus" :size="16" color="#102522" />
            Добавить расход
          </BaseButton>
        </template>
      </PageHeader>

      <FinanceTabs />

      <!-- В API нет расходов (только начисления и поступления), поэтому раздел — честная заглушка без цифр -->
      <div class="table-card">
        <div class="table-header">
          <div class="col num">№</div>
          <div class="col date">ДАТА</div>
          <div class="col cat">КАТЕГОРИЯ</div>
          <div class="col desc">ОПИСАНИЕ</div>
          <div class="col amount">СУММА</div>
          <div class="col doc">ДОКУМЕНТ</div>
        </div>
        <StateBlock
          kind="empty"
          message="Учёт расходов появится позже. Сейчас в системе ведутся начисления, оплаты и целевые сборы."
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import Sidebar from '../../../components/layout/Sidebar.vue'
import PageHeader from '../../../components/layout/PageHeader.vue'
import FinanceTabs from '../../../components/layout/FinanceTabs.vue'
import BaseButton from '../../../components/ui/BaseButton.vue'
import BaseIcon from '../../../components/ui/BaseIcon.vue'
import StateBlock from '../../../components/ui/StateBlock.vue'
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