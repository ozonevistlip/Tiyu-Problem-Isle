import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  {
    path: '/',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      { path: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { public: true } },
      { path: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { public: true } }
    ]
  },
  {
    path: '/teacher',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { role: 'teacher' },
    children: [
      { path: '', component: () => import('@/views/teacher/TeacherHomeView.vue') },
      { path: 'classes', component: () => import('@/views/teacher/ClassListView.vue') },
      { path: 'classes/:classId', component: () => import('@/views/teacher/ClassDetailView.vue') },
      { path: 'problems', component: () => import('@/views/teacher/ProblemListView.vue') },
      { path: 'problems/create', component: () => import('@/views/teacher/ProblemEditView.vue') },
      { path: 'problems/:problemId/edit', component: () => import('@/views/teacher/ProblemEditView.vue') },
      { path: 'problems/:problemId/testcases', component: () => import('@/views/teacher/TestcaseEditView.vue') },
      { path: 'problems/:problemId/hints', component: () => import('@/views/teacher/ProblemHintEditView.vue') },
      { path: 'contests', component: () => import('@/views/teacher/ContestListView.vue') },
      { path: 'code-visualizer', component: () => import('@/views/common/CodeVisualizerView.vue') },
      { path: 'contests/create', component: () => import('@/views/teacher/ContestEditView.vue') },
      { path: 'contests/:contestId/edit', component: () => import('@/views/teacher/ContestEditView.vue') },
      { path: 'contests/:contestId/problems', component: () => import('@/views/teacher/ContestProblemSelectView.vue') },
      { path: 'contests/:contestId/rank', component: () => import('@/views/teacher/ContestRankView.vue') },
      { path: 'contests/:contestId/submissions', component: () => import('@/views/teacher/ContestSubmissionView.vue') }
    ]
  },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { role: 'student' },
    children: [
      { path: '', component: () => import('@/views/student/StudentHomeView.vue') },
      { path: 'contests', component: () => import('@/views/student/StudentContestListView.vue') },
      { path: 'code-visualizer', component: () => import('@/views/common/CodeVisualizerView.vue') },
      { path: 'contests/:contestId', component: () => import('@/views/student/StudentContestDetailView.vue') },
      { path: 'contests/:contestId/problems/:problemId', component: () => import('@/views/student/StudentProblemView.vue') },
      { path: 'contests/:contestId/submissions', component: () => import('@/views/student/StudentSubmissionListView.vue') },
      { path: 'contests/:contestId/rank', component: () => import('@/views/student/StudentRankView.vue') }
    ]
  },
  { path: '/403', component: () => import('@/views/error/ForbiddenView.vue') },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/error/NotFoundView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const user = useUserStore()
  if (to.meta.public) {
    if (user.isLogin && user.role) return user.role === 'teacher' ? '/teacher' : '/student'
    return true
  }
  if (!user.isLogin) return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  if (!user.userInfo) {
    try {
      await user.fetchMe()
    } catch {
      user.logout()
      return '/login'
    }
  }
  const requiredRole = to.meta.role
  if (requiredRole && user.role !== requiredRole) return '/403'
  return true
})

export default router
