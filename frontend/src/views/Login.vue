<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="login-title">城市无主井盖协同治理平台</div>
      <div class="login-sub">认领 · 巡检 · 维修 · 赔付 一体化协同</div>
      <el-form :model="form" @keyup.enter="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                    show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登 录
        </el-button>
        <div class="flex-between mt16">
          <el-link type="primary" @click="$router.push('/register')">市民注册</el-link>
          <el-popover placement="bottom" width="360" trigger="click">
            <template #reference><el-link type="info">演示账号</el-link></template>
            <el-table :data="accounts" size="small">
              <el-table-column prop="u" label="账号" width="90" />
              <el-table-column prop="p" label="密码" width="90" />
              <el-table-column prop="r" label="角色" />
            </el-table>
          </el-popover>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const accounts = [
  { u: 'citizen', p: 'citizen123', r: '市民' },
  { u: 'street', p: 'street123', r: '街道值班员' },
  { u: 'gas', p: 'owner123', r: '权属单位-燃气' },
  { u: 'telecom', p: 'owner123', r: '权属单位-通信' },
  { u: 'drainage', p: 'owner123', r: '权属单位-排水' },
  { u: 'municipal', p: 'owner123', r: '权属单位-市政' },
  { u: 'repair1', p: 'repair123', r: '维修队' },
  { u: 'inspector', p: 'inspect123', r: '城管巡查员' },
  { u: 'comp', p: 'comp123', r: '赔付专员' },
  { u: 'city', p: 'city123', r: '市级协调人员' },
  { u: 'admin', p: 'admin123', r: '城管管理员' }
]

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
