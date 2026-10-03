<template>
  <div class="page">
    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>市级协调池（联合复核仍无法确认权属的事件）</b>
          <el-tag type="danger" effect="dark">应急基金垫付 · 强制认领 · 追偿</el-tag>
        </div>
      </template>
      <el-alert type="warning" :closable="false" class="mb16"
                title="各单位拒绝签收的理由与证据可在事件详情中审阅；市级协调人员据此决定是否强制认领，应急基金垫付款按结论追偿。" />
      <el-table :data="pool" size="large" empty-text="协调池暂无事件">
        <el-table-column prop="incidentCode" label="事件编号" width="160" />
        <el-table-column prop="incidentAddress" label="地址" min-width="170" />
        <el-table-column label="到场/拒签" width="120">
          <template #default="{ row }">
            <el-tag type="success" size="small">{{ signedCount(row) }} 签字</el-tag>
            <el-tag type="danger" size="small" style="margin-left: 4px">{{ refusedCount(row) }} 拒签</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应急基金垫付" width="130">
          <template #default="{ row }">
            <span v-if="row.advance">{{ money(row.advance.amount) }}</span>
            <span v-else class="text-muted">未垫付</span>
          </template>
        </el-table-column>
        <el-table-column label="入池时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.review.escalatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small"
                       @click="$router.push(`/incidents/${row.review.incidentId}`)">
              审阅处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header><b>应急基金垫付与追偿台账</b></template>
      <el-table :data="advances" size="large" empty-text="暂无垫付记录">
        <el-table-column prop="incidentCode" label="事件编号" width="160" />
        <el-table-column prop="incidentAddress" label="地址" min-width="160" />
        <el-table-column label="垫付金额" width="120">
          <template #default="{ row }">{{ money(row.advance.amount) }}</template>
        </el-table-column>
        <el-table-column prop="advance.purpose" label="用途" min-width="200" />
        <el-table-column label="追偿对象" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.recoverFromOrg" type="warning" size="small">{{ row.recoverFromOrg }}</el-tag>
            <span v-else class="text-muted">待复核结论锁定</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.advance.status === 'RECOVERED' ? 'success' : 'danger'" size="small">
              {{ row.advance.status === 'RECOVERED' ? '已追偿回补' : '待追偿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="垫付/回补时间" width="260">
          <template #default="{ row }">
            <div>垫付：{{ fmtTime(row.advance.createdAt) }}</div>
            <div v-if="row.advance.recoveredAt" class="text-muted">
              回补：{{ fmtTime(row.advance.recoveredAt) }}（{{ money(row.advance.recoveredAmount) }}）
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="canRecover(row)" type="success" size="small" @click="recover(row)">
              追偿到账
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiCityPool, apiCityAdvances, apiFundRecover } from '../api'
import { fmtTime, money } from '../utils/format'

const auth = useAuthStore()
const pool = ref([])
const advances = ref([])

const canRecover = computed(() =>
  ['CITY_COORDINATOR', 'ADMIN'].includes(auth.role))

const signedCount = (row) => row.signoffs.filter(s => s.signoff.status === 'SIGNED').length
const refusedCount = (row) => row.signoffs.filter(s => s.signoff.status === 'REFUSED').length

async function recover(row) {
  await ElMessageBox.confirm(
    `确认已向 ${row.recoverFromOrg || '责任单位'} 追偿回补 ${money(row.advance.amount)}？`,
    '追偿回补', { type: 'success' })
  await apiFundRecover(row.advance.id, { amount: row.advance.amount })
  ElMessage.success('追偿款已回补应急基金')
  load()
}

async function load() {
  pool.value = await apiCityPool()
  advances.value = await apiCityAdvances()
}

onMounted(load)
</script>
