<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index === levelList.length - 1" class="no-redirect">{{ item.meta?.title }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta?.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router';
import { usePermissionStore } from '@/store/modules/permission';
import { ADMIN_BASE_PATH, ADMIN_HOME_PATH } from '@/constants/router';

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();
const levelList = ref<any[]>([]);

const findPathNum = (str: string, char = '/') => {
  if (typeof str !== 'string' || str.length === 0) return 0;
  return str.split(char).length - 1;
};

const getMatched = (pathList: string[], routeList: RouteRecordRaw[]) => {
  const matched: RouteRecordRaw[] = [];

  const walk = (list: RouteRecordRaw[], paths: string[]) => {
    const currentPath = paths[0];
    const data = list.find((item) => item.path === currentPath || item.name?.toString().toLowerCase() === currentPath);
    if (!data) return;

    matched.push(data);
    if (data.children && paths.length > 1) {
      walk(data.children, paths.slice(1));
    }
  };

  walk(routeList, pathList);
  return matched;
};

const isDashboard = (item?: any) => {
  const name = item?.name as string | undefined;
  return name?.trim() === 'Index';
};

const getBreadcrumb = () => {
  let matched: any[] = [];

  if (route.path.startsWith(`${ADMIN_BASE_PATH}/`)) {
    matched = route.matched.filter((item) => item.meta && item.meta.title);
  } else if (findPathNum(route.path) > 2) {
    const pathList = route.path.match(/\/\w+/gi)?.map((item, index) => (index === 0 ? item : item.slice(1))) ?? [];
    matched = getMatched(pathList, permissionStore.defaultRoutes);
  } else {
    matched = route.matched.filter((item) => item.meta && item.meta.title);
  }

  if (!isDashboard(matched[0])) {
    matched = [{ path: ADMIN_HOME_PATH, meta: { title: '后台首页' } }, ...matched];
  }

  levelList.value = matched.filter((item) => item.meta && item.meta.title && item.meta.breadcrumb !== false);
};

const handleLink = (item: any) => {
  if (item.redirect && item.redirect !== 'noRedirect') {
    router.push(item.redirect);
    return;
  }
  router.push(item.path);
};

watchEffect(() => {
  if (route.path.startsWith('/redirect/')) return;
  getBreadcrumb();
});

onMounted(() => {
  getBreadcrumb();
});
</script>

<style lang="scss" scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;
  margin-left: 8px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>
