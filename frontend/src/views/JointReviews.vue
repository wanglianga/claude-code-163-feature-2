<template>
  <div class="page">
    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>联合现场复核与市级协调池</b>
          <span class="text-muted">
            燃气、通信、排水等单位互相推诿时，街道发起联合现场复核；结论须各单位代表签字，
            仍无法确认则进入市级协调池，应急基金先行垫付
          </span>
        </div>
      </template>

      <el-tabs v-model="tab">
        <el-tab-pane v-for="t in tabs" :key="t.label" :label="`${t.label}(${t.list.length})`" :name="t.label" />
      </el-tabs>

      <el-table :data="currentList" size="large"
                @row-click="(r) => $router.push(`/incidents/${r.review.incidentId}`)" style="cursor: pointer">
        <el-table-column label="事件编号" width="150">
          <template #default="{ row }">
            <el-link type="primary">{{ row.incidentCode }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="170" />
        <el-table-column label="井盖编号" min-width="130">
          <template #default="{ row }">{{ row.review.manholeCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="危险度" width="80">
          <template #default="{ row }">
            <el-tag :type="dangerType(row.dangerLevel)" size="small">{{ row.dangerLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="REVIEW_STATUS[row.review.status]?.type" size="small">
              {{ REVIEW_STATUS[row.review.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="代表签收" width="170">
          <template #default="{ row }">
            <el-tag type="success" size="small" effect="plain">签 {{ row.signedCount }}</el-tag>
            <el-tag type="danger" size="small" effect="plain" style="margin-left: 4px">拒 {{ row.refusedCount }}</el-tag>
            <el-tag type="info" size="small" effect="plain" style="margin-left: 4px">待 {{ row.pendingCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应急基金" width="110">
          <template #default="{ row }">
            <span v-if="row.review.fundAdvanceAmount" style="color: #f56c6c; font-weight: 600">
              {{ money(row.review.fundAdvanceAmount) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="权属结论" min-width="140">
          <template #default="{ row }">
            <span v-if="row.review.status === 'CONFIRMED'" style="color: #67c23a">
              {{ row.confirmedOwnerOrg }}{{ row.review.forcedClaim ? '（强制认领）' : '' }}
            </span>
            <span v-else-if="row.review.status === 'ESCALATED'" class="text-muted">待市级研判</span>
            <span v-else class="text-muted">复核中</span>
          </template>
        </el-table-column>
        <el-table-column label="发起时间" width="140">
          <template #default="{ row }">{{ fmtTime(row.review.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="$router.push(`/incidents/${row.review.incidentId}`)">
              {{ actionLabel(row) }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../store/auth'
import { apiJointReviews } from '../api'
import { REVIEW_STATUS, dangerType, fmtTime, money } from '../utils/format'

const auth = useAuthStore()
const role = computed(() => auth.role)
const list = ref([])
const tab = ref('')

function myPending(row) {
  const mine = (row.signatures || []).find(s => s.mine)
  return mine && mine.signature.status === 'PENDING' && row.review.status === 'IN_PROGRESS'
}

const tabs = computed(() => {
  const all = list.value
  const mine = role.value === 'OWNER_UNIT'
  return [
    ...(mine ? [{ label: '待我签收', list: all.filter(myPending) }] : [
      { label: '复核中', list: all.filter(r => r.review.status === 'IN_PROGRESS') },
      { label: '市级协调池', list: all.filter(r => r.review.status === 'ESCALATED') }
    ]),
    { label: '已形成结论', list: all.filter(r => r.review.status === 'CONFIRMED') },
    { label: '全部', list: all }
  ]
})

const currentList = computed(() => {
  const t = tabs.value.find(x => x.label === tab.value) || tabs.value[0]
  return t ? t.list : []
})

function actionLabel(row) {
  if (myPending(row)) return '去签收'
  if (role.value === 'ADMIN' && row.review.status === 'ESCALATED') return '市级处理'
  if (['STREET_DUTY', 'ADMIN'].includes(role.value) && row.review.status === 'IN_PROGRESS') return '复核处理'
  return '详情'
}

onMounted(async () => {
  list.value = await apiJointReviews()
  if (!tab.value && tabs.value.length) tab.value = tabs.value[0].label
})
</script>
