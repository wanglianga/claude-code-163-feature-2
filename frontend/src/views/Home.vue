<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="24">
        <el-card class="page-card">
          <h2 style="margin: 0 0 4px">{{ greeting }}，{{ auth.user?.name }}</h2>
          <div class="text-muted">
            {{ auth.roleName }} · {{ auth.user?.orgName }} · 今天是 {{ today }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.label" :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="page-card" shadow="hover" style="text-align: center; cursor: pointer" @click="c.to && $router.push(c.to)">
          <div class="text-muted">{{ c.label }}</div>
          <div class="stat-num" :style="{ color: c.color || '#1f3a5f' }">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>{{ todoTitle }}</b>
          <el-button v-if="role === 'CITIZEN'" type="primary" @click="$router.push('/report')">
            <el-icon><Plus /></el-icon>&nbsp;上报井盖问题
          </el-button>
        </div>
      </template>
      <el-table :data="todos" size="large" @row-click="(r) => $router.push(`/incidents/${r.id}`)" style="cursor: pointer">
        <el-table-column prop="code" label="事件编号" width="160" />
        <el-table-column prop="issueType" label="问题" width="80" />
        <el-table-column prop="address" label="地址" min-width="180" />
        <el-table-column label="危险度" width="90">
          <template #default="{ row }">
            <el-tag :type="dangerType(row.dangerLevel)" size="small">{{ row.dangerLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主状态" width="120">
          <template #default="{ row }">
            <el-tag :type="INCIDENT_STATUS[row.status]?.type" size="small">
              {{ INCIDENT_STATUS[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应急线" width="110">
          <template #default="{ row }">
            <el-tag :type="EMERGENCY_STATUS[row.emergencyStatus]?.type" size="small" effect="plain">
              {{ EMERGENCY_STATUS[row.emergencyStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权属线" width="110">
          <template #default="{ row }">
            <el-tag :type="OWNER_STATUS[row.ownerStatus]?.type" size="small" effect="plain">
              {{ OWNER_STATUS[row.ownerStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上报时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useAuthStore } from '../store/auth'
import { apiIncidents, apiMyOrders, apiCompensations, apiCostAllocations, apiCityPool, apiCityAdvances } from '../api'
import { INCIDENT_STATUS, EMERGENCY_STATUS, OWNER_STATUS, dangerType, fmtTime } from '../utils/format'

const auth = useAuthStore()
const role = computed(() => auth.role)
const today = dayjs().format('YYYY年MM月DD日')
const greeting = computed(() => {
  const h = dayjs().hour()
  return h < 6 ? '凌晨好' : h < 12 ? '上午好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
})

const incidents = ref([])
const orders = ref([])
const comps = ref([])
const costs = ref([])
const pool = ref([])
const advances = ref([])

const cards = computed(() => {
  const list = incidents.value
  switch (role.value) {
    case 'CITIZEN':
      return [
        { label: '我的上报', value: list.length, to: '/workbench' },
        { label: '处理中', value: list.filter(i => !['CLOSED'].includes(i.status)).length, color: '#e6a23c' },
        { label: '已结案', value: list.filter(i => i.status === 'CLOSED').length, color: '#67c23a' }
      ]
    case 'STREET_DUTY':
      return [
        { label: '待应急处置', value: list.filter(i => i.emergencyStatus !== 'SECURED').length, color: '#f56c6c', to: '/workbench' },
        { label: '权属排查中', value: list.filter(i => i.ownerStatus === 'INVESTIGATING').length, color: '#e6a23c' },
        { label: '责任待定', value: list.filter(i => i.ownerStatus === 'PENDING').length, color: '#f56c6c' },
        { label: '全部事件', value: list.length }
      ]
    case 'OWNER_UNIT':
      return [
        { label: '待认领', value: list.filter(i => i.ownerStatus !== 'CONFIRMED').length, color: '#f56c6c', to: '/workbench' },
        { label: '我单位事件', value: list.filter(i => i.ownerStatus === 'CONFIRMED').length, color: '#409eff' },
        { label: '待派单', value: list.filter(i => i.status === 'OWNER_CONFIRMED').length, color: '#e6a23c' }
      ]
    case 'REPAIR_TEAM':
      return [
        { label: '待接单', value: orders.value.filter(o => ['ASSIGNED', 'RETURNED'].includes(o.status)).length, color: '#f56c6c', to: '/workbench' },
        { label: '维修中', value: orders.value.filter(o => o.status === 'ACCEPTED').length, color: '#e6a23c' },
        { label: '已完成', value: orders.value.filter(o => o.status === 'FINISHED').length, color: '#67c23a' }
      ]
    case 'INSPECTOR':
      return [
        { label: '待验收', value: list.filter(i => i.status === 'REPAIRED').length, color: '#f56c6c', to: '/workbench' },
        { label: '维修中', value: list.filter(i => i.status === 'REPAIRING').length, color: '#e6a23c' },
        { label: '验收通过', value: list.filter(i => ['ACCEPTED', 'CLOSED'].includes(i.status)).length, color: '#67c23a' }
      ]
    case 'COMPENSATOR':
      return [
        { label: '待审核案件', value: comps.value.filter(c => c.status === 'FILED').length, color: '#f56c6c', to: '/compensation' },
        { label: '待赔付', value: comps.value.filter(c => c.status === 'APPROVED').length, color: '#e6a23c' },
        { label: '已赔付', value: comps.value.filter(c => c.status === 'PAID').length, color: '#67c23a' }
      ]
    case 'ADMIN':
      return [
        { label: '全部事件', value: list.length, to: '/stats' },
        { label: '待应急处置', value: list.filter(i => i.emergencyStatus !== 'SECURED').length, color: '#f56c6c' },
        { label: '责任待定', value: list.filter(i => i.ownerStatus === 'PENDING').length, color: '#f56c6c' },
        { label: '待费用复核', value: costs.value.filter(c => c.status === 'PENDING').length, color: '#e6a23c', to: '/cost' },
        { label: '已结案', value: list.filter(i => i.status === 'CLOSED').length, color: '#67c23a' }
      ]
    case 'CITY_COORDINATOR':
      return [
        { label: '市级协调池', value: pool.value.length, color: '#f56c6c', to: '/city-pool' },
        { label: '应急基金垫付待追偿', value: advances.value.filter(a => a.advance.status === 'PENDING').length, color: '#e6a23c', to: '/city-pool' },
        { label: '已追偿回补', value: advances.value.filter(a => a.advance.status === 'RECOVERED').length, color: '#67c23a' },
        { label: '全部事件', value: list.length }
      ]
    default:
      return []
  }
})

const todoTitle = computed(() => ({
  CITIZEN: '我的上报记录',
  STREET_DUTY: '待办事件（应急处置与权属排查）',
  OWNER_UNIT: '待认领与我单位事件',
  REPAIR_TEAM: '相关事件',
  INSPECTOR: '待验收事件',
  COMPENSATOR: '涉及赔付的事件',
  ADMIN: '最新事件',
  CITY_COORDINATOR: '市级协调与推诿事件'
}[role.value] || '事件列表'))

const todos = computed(() => {
  const list = incidents.value
  switch (role.value) {
    case 'STREET_DUTY':
      return list.filter(i => i.emergencyStatus !== 'SECURED'
        || ['INVESTIGATING', 'PENDING', 'JOINT_REVIEW', 'ESCALATED'].includes(i.ownerStatus)).slice(0, 10)
    case 'OWNER_UNIT':
      return list.filter(i => i.ownerStatus !== 'CONFIRMED' || i.status === 'OWNER_CONFIRMED').slice(0, 10)
    case 'INSPECTOR':
      return list.filter(i => i.status === 'REPAIRED').slice(0, 10)
    case 'COMPENSATOR': {
      const ids = new Set(comps.value.filter(c => c.status !== 'PAID' && c.status !== 'REJECTED').map(c => c.incidentId))
      return list.filter(i => ids.has(i.id)).slice(0, 10)
    }
    default:
      return list.slice(0, 10)
  }
})

onMounted(async () => {
  incidents.value = await apiIncidents()
  if (role.value === 'REPAIR_TEAM') orders.value = await apiMyOrders()
  if (role.value === 'COMPENSATOR') comps.value = await apiCompensations()
  if (role.value === 'ADMIN') {
    costs.value = await apiCostAllocations()
    comps.value = await apiCompensations()
  }
  if (role.value === 'CITY_COORDINATOR') {
    pool.value = await apiCityPool()
    advances.value = await apiCityAdvances()
  }
})
</script>
