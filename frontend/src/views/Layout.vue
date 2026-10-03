<template>
  <el-container style="height: 100vh">
    <el-header class="layout-header">
      <div style="display: flex; align-items: center; gap: 28px">
        <span class="layout-title">🕳️ 城市无主井盖协同治理平台</span>
        <el-menu mode="horizontal" :default-active="$route.path" router style="min-width: 600px">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item v-if="role === 'CITIZEN'" index="/report">我要上报</el-menu-item>
          <el-menu-item v-if="showWorkbench" index="/workbench">{{ workbenchName }}</el-menu-item>
          <el-menu-item v-if="['COMPENSATOR', 'ADMIN'].includes(role)" index="/compensation">赔付管理</el-menu-item>
          <el-menu-item v-if="['CITY_COORDINATOR', 'ADMIN'].includes(role)" index="/city-pool">市级协调池</el-menu-item>
          <el-menu-item v-if="['ADMIN', 'STREET_DUTY'].includes(role)" index="/cost">费用分摊</el-menu-item>
          <el-menu-item v-if="['ADMIN', 'STREET_DUTY', 'INSPECTOR', 'COMPENSATOR'].includes(role)" index="/stats">统计分析</el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/routes">巡检路线</el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/rules">移交规则</el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/users">账号管理</el-menu-item>
        </el-menu>
      </div>
      <div class="user-chip">
        <el-popover placement="bottom-end" width="380" trigger="click" @show="loadNotifications">
          <template #reference>
            <el-badge :value="unread" :hidden="!unread" style="cursor: pointer; margin-right: 12px">
              <el-icon :size="20" color="#fff"><Bell /></el-icon>
            </el-badge>
          </template>
          <div class="flex-between mb16">
            <b>消息通知</b>
            <el-link type="primary" @click="readAll">全部已读</el-link>
          </div>
          <el-scrollbar max-height="360px">
            <div v-if="!notifications.length" class="text-muted">暂无消息</div>
            <div v-for="n in notifications" :key="n.id"
                 style="padding: 8px 0; border-bottom: 1px solid #eee; cursor: pointer"
                 @click="goIncident(n)">
              <div :style="{ fontWeight: n.readFlag ? 400 : 600 }">{{ n.title }}</div>
              <div class="text-muted">{{ n.content }}</div>
              <div class="text-muted">{{ fmtTime(n.createdAt) }}</div>
            </div>
          </el-scrollbar>
        </el-popover>
        <el-tag size="small" effect="dark">{{ auth.roleName }}</el-tag>
        <span>{{ auth.user?.name }}（{{ auth.user?.orgName }}）</span>
        <el-button size="small" text style="color: #fff" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-main style="padding: 0; overflow-y: auto">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { apiNotifications, apiReadAll } from '../api'
import { fmtTime } from '../utils/format'

const router = useRouter()
const auth = useAuthStore()
const role = computed(() => auth.role)
const notifications = ref([])
const unread = ref(0)

const workbenchName = computed(() => ({
  CITIZEN: '我的上报',
  STREET_DUTY: '应急处置台',
  OWNER_UNIT: '认领与派单',
  REPAIR_TEAM: '维修工单',
  INSPECTOR: '验收工作台',
  CITY_COORDINATOR: '市级协调台'
}[role.value] || '工作台'))

const showWorkbench = computed(() =>
  ['CITIZEN', 'STREET_DUTY', 'OWNER_UNIT', 'REPAIR_TEAM', 'INSPECTOR', 'CITY_COORDINATOR'].includes(role.value))

async function loadNotifications() {
  const data = await apiNotifications()
  notifications.value = data.list
  unread.value = data.unread
}

async function readAll() {
  await apiReadAll()
  loadNotifications()
}

function goIncident(n) {
  if (n.incidentId) router.push(`/incidents/${n.incidentId}`)
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(loadNotifications)
</script>
