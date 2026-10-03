import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', component: () => import('../views/Home.vue') },
      { path: 'report', component: () => import('../views/Report.vue'), meta: { roles: ['CITIZEN'] } },
      { path: 'incidents/:id', component: () => import('../views/IncidentDetail.vue') },
      { path: 'workbench', component: () => import('../views/Workbench.vue') },
      { path: 'compensation', component: () => import('../views/Compensation.vue'), meta: { roles: ['COMPENSATOR', 'ADMIN'] } },
      { path: 'city-pool', component: () => import('../views/CityPool.vue'), meta: { roles: ['CITY_COORDINATOR', 'ADMIN'] } },
      { path: 'cost', component: () => import('../views/Cost.vue'), meta: { roles: ['ADMIN', 'STREET_DUTY'] } },
      { path: 'stats', component: () => import('../views/Stats.vue'), meta: { roles: ['ADMIN', 'STREET_DUTY', 'INSPECTOR', 'COMPENSATOR'] } },
      { path: 'routes', component: () => import('../views/Routes.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'rules', component: () => import('../views/Rules.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'users', component: () => import('../views/Users.vue'), meta: { roles: ['ADMIN'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.meta.public) return true
  if (!token || !user) return '/login'
  if (to.meta.roles && !to.meta.roles.includes(user.role)) return '/'
  return true
})

export default router
