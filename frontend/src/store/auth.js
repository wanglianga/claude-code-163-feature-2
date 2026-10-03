import { defineStore } from 'pinia'
import { apiLogin, apiRegister } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token && !!s.user,
    role: (s) => s.user?.role || '',
    roleName: (s) => ROLE_NAMES[s.user?.role] || s.user?.role || ''
  },
  actions: {
    async login(username, password) {
      const data = await apiLogin({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    async register(form) {
      await apiRegister(form)
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})

export const ROLE_NAMES = {
  CITIZEN: '市民',
  STREET_DUTY: '街道值班员',
  OWNER_UNIT: '权属单位',
  REPAIR_TEAM: '维修队',
  INSPECTOR: '城管巡查员',
  COMPENSATOR: '赔付专员',
  ADMIN: '城管管理员',
  CITY_COORDINATOR: '市级协调人员'
}
