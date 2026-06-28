import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import Layout from '@/layout/index.vue';
import { ADMIN_BASE_PATH, ADMIN_HOME_PATH, PORTAL_HOME_PATH } from '@/constants/router';

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: PORTAL_HOME_PATH,
    hidden: true
  },
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/social-callback',
    hidden: true,
    component: () => import('@/layout/components/SocialCallback/index.vue')
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue'),
    hidden: true
  },
  {
    path: '/register',
    redirect: '/login',
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    hidden: true
  },
  {
    path: PORTAL_HOME_PATH,
    component: () => import('@/layout/portal/index.vue'),
    hidden: true,
    children: [
      { path: '', name: 'InfoPortalHome', component: () => import('@/views/portal/home/index.vue'), meta: { title: '服务概览' } },
      { path: 'resources', name: 'InfoResources', component: () => import('@/views/portal/resources/index.vue'), meta: { title: '资料共享' } },
      { path: 'tools', name: 'InfoTools', component: () => import('@/views/portal/tools/index.vue'), meta: { title: '工具即用' } },
      { path: 'forum', name: 'InfoForum', component: () => import('@/views/portal/forum/index.vue'), meta: { title: '服务论坛' } }
    ]
  },
  {
    path: '/appcenter',
    redirect: `${PORTAL_HOME_PATH}/tools`,
    hidden: true
  },
  {
    path: '/index',
    redirect: ADMIN_HOME_PATH,
    hidden: true
  },
  {
    path: ADMIN_BASE_PATH,
    component: Layout,
    redirect: ADMIN_HOME_PATH,
    children: [
      {
        path: 'index',
        component: () => import('@/views/admin/home/index.vue'),
        name: 'Index',
        meta: { title: '后台首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: `${ADMIN_BASE_PATH}/user`,
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/admin/system/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  }
];

export const dynamicRoutes: RouteRecordRaw[] = [];

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_APP_CONTEXT_PATH),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0 };
  }
});

export default router;
