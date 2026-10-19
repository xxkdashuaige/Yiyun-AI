import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/LoginView.vue'),
        meta: { public: true },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/HomeView.vue'),
      },
      {
        path: 'gallery',
        name: 'Gallery',
        component: () => import('@/views/GalleryView.vue'),
      },
      {
        path: 'gallery/:id',
        name: 'HeritageDetail',
        component: () => import('@/views/HeritageDetailView.vue'),
        props: true,
      },
      {
        path: 'ai',
        name: 'AiGuide',
        component: () => import('@/views/AiGuideView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'create',
        name: 'Create',
        component: () => import('@/views/CreateView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'works',
        name: 'Works',
        component: () => import('@/views/WorksView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'works/:id',
        name: 'WorkDetail',
        component: () => import('@/views/WorkDetailView.vue'),
        props: true,
        meta: { requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router