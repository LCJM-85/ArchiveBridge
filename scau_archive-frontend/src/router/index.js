import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/login/Login.vue'
import Dashboard from '@/views/dashboard/Dashboard.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const hasValidToken = token && !isTokenExpired(token)

  if (to.meta.requiresAuth) {
    if (hasValidToken) {
      next()
    } else {
      localStorage.removeItem('token')
      next({
        path: '/login',
        query: { redirect: to.fullPath },
      })
    }
  } else {
    next()
  }
})

function isTokenExpired(token) {
  const segments = token.split('.')
  if (segments.length !== 3) return true
  try {
    const payload = JSON.parse(decodeBase64Url(segments[1]))
    if (!payload.exp) return false
    return Date.now() >= payload.exp * 1000
  } catch (error) {
    return true
  }
}

function decodeBase64Url(base64Url) {
  const normalized = base64Url.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
  return decodeURIComponent(
    atob(padded)
      .split('')
      .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
      .join('')
  )
}

export default router
