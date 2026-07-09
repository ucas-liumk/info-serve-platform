<template>
  <main class="portal-home">
    <section class="home-shell">
      <HomeTopbar @open-manual="openManualDialog" />

      <ModuleGrid :modules="featuredModules" :total="modules.length" @open="openModule" @more="openModuleDialog" />

      <StatsBand :stats="stats" :loading="loading" />
    </section>

    <AllModuleDialog v-model="moduleDialog.visible" :modules="modules" :saving="moduleDialog.saving" @open="openModule" @reorder="saveModuleOrder" />
    <SystemManualDialog v-model="manualDialogVisible" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getPortalStats } from '@/api/portal/stats';
import { PortalStats } from '@/api/infoservice/types';
import { listPortalModules, updatePortalModuleOrder } from '@/api/portal/module';
import AllModuleDialog from './components/AllModuleDialog.vue';
import HomeTopbar from './components/HomeTopbar.vue';
import ModuleGrid from './components/ModuleGrid.vue';
import StatsBand from './components/StatsBand.vue';
import SystemManualDialog from './components/SystemManualDialog.vue';
import moduleResource from '@/assets/portal/module-resource.png';
import moduleTools from '@/assets/portal/module-tools.png';
import moduleQa from '@/assets/portal/module-qa.png';
import moduleHot from '@/assets/portal/module-hot.png';
import moduleForum from '@/assets/portal/module-forum.png';
import moduleLowcode from '@/assets/portal/module-lowcode.png';
import moduleAnalysis from '@/assets/portal/module-analysis.png';
import moduleUsageDashboard from '@/assets/portal/module-usage-dashboard.png';
import moduleDashboard from '@/assets/portal/module-dashboard.png';

interface HomeModule {
  code?: string;
  title: string;
  desc: string;
  image: string;
  path?: string;
  sortOrder?: number;
}

const router = useRouter();
const loading = ref(false);
const manualDialogVisible = ref(false);

const moduleDialog = reactive({
  visible: false,
  saving: false
});

const stats = ref<PortalStats>({
  resourceCount: 0,
  toolCount: 0,
  topicCount: 0,
  activeUserCount: 0,
  todayVisitCount: 0
});

/** 各模块默认配图（注册表 image 为空时按 moduleCode 兜底） */
const MODULE_ART: Record<string, string> = {
  resources: moduleResource,
  appcenter: moduleTools,
  lowcode: moduleLowcode,
  analysis: moduleAnalysis,
  'usage-dashboard': moduleUsageDashboard,
  dashboard: moduleDashboard,
  qa: moduleQa,
  news: moduleHot,
  forum: moduleForum
};

const MODULE_NAME_ART: Record<string, string> = {
  低代码: moduleLowcode,
  运行分析: moduleAnalysis,
  应用态势: moduleUsageDashboard,
  态势: moduleDashboard
};

const HOME_MODULE_LIMIT = 6;

/** 注册表不可用时的兜底卡片（与种子数据一致） */
const DEFAULT_MODULES: HomeModule[] = [
  { code: 'resources', title: '资料共享', desc: '数据汇聚  共享共用', image: moduleResource, path: '/portal/resources', sortOrder: 10 },
  { code: 'appcenter', title: '应用中心', desc: '应用聚合  即取即用', image: moduleTools, path: '/portal/tools', sortOrder: 20 },
  { code: 'usage-dashboard', title: '应用态势', desc: '运行洞察  转型透明', image: moduleUsageDashboard, path: '/portal/usage-dashboard', sortOrder: 30 },
  { code: 'forum', title: '服务论坛', desc: '交流互动  共建共治', image: moduleForum, path: '/portal/forum', sortOrder: 40 },
  { code: 'qa', title: '智能问答', desc: '智慧问答  快速响应', image: moduleQa, sortOrder: 80 },
  { code: 'news', title: '时事热点', desc: '热点速递  洞察先机', image: moduleHot, sortOrder: 90 }
];

const modules = ref<HomeModule[]>(DEFAULT_MODULES);
const featuredModules = computed(() => modules.value.slice(0, HOME_MODULE_LIMIT));

/** 首页卡片改从 portal_module 注册表渲染（后台可配启停/排序/权限） */
const loadModules = async () => {
  try {
    const res = await listPortalModules();
    const rows = res.data ?? [];
    if (rows.length > 0) {
      modules.value = rows
        .map((row) => ({
          code: row.moduleCode,
          title: row.moduleName,
          desc: row.description || '',
          image: row.image || resolveModuleImage(row.moduleCode, row.moduleName),
          path: row.status === '0' && row.entryPath ? row.entryPath : undefined,
          sortOrder: Number(row.sortOrder || 0)
        }))
        .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
    }
  } catch {
    // 注册表不可用时保留兜底卡片，不阻塞首页
  }
};

const resolveModuleImage = (moduleCode?: string, moduleName?: string) => {
  if (moduleCode && MODULE_ART[moduleCode]) {
    return MODULE_ART[moduleCode];
  }
  const matchedName = Object.keys(MODULE_NAME_ART).find((keyword) => moduleName?.includes(keyword));
  return matchedName ? MODULE_NAME_ART[matchedName] : moduleTools;
};

const openModule = (item: HomeModule) => {
  if (item.path) {
    if (/^https?:\/\//.test(item.path)) {
      window.open(normalizeServiceUrl(item.path), '_blank');
      return;
    }
    router.push(item.path);
    return;
  }
  ElMessage.info(`${item.title}将在后续版本开放`);
};

const openModuleDialog = () => {
  moduleDialog.visible = true;
};

const openManualDialog = () => {
  manualDialogVisible.value = true;
};

const saveModuleOrder = async (orderedModules: HomeModule[]) => {
  modules.value = orderedModules;
  const moduleCodes = orderedModules.map((item) => item.code).filter((code): code is string => Boolean(code));
  if (moduleCodes.length === 0) {
    return;
  }

  moduleDialog.saving = true;
  try {
    await updatePortalModuleOrder(moduleCodes);
    ElMessage.success('首页服务顺序已保存');
  } catch {
    ElMessage.error('顺序已调整，但保存失败，请稍后重试');
  } finally {
    moduleDialog.saving = false;
  }
};

const normalizeServiceUrl = (raw: string) => {
  try {
    const url = new URL(raw);
    if (['127.0.0.1', 'localhost'].includes(url.hostname)) {
      url.hostname = window.location.hostname;
    }
    return url.toString();
  } catch {
    return raw;
  }
};

onMounted(async () => {
  void loadModules();

  loading.value = true;
  try {
    const res: any = await getPortalStats();
    stats.value = { ...stats.value, ...(res.data || {}) };
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.portal-home {
  --portal-max: var(--ip-layout-max);
  --portal-blue: var(--ip-primary-900);
  --portal-text: var(--ip-neutral-900);
  --portal-muted: var(--ip-neutral-600);

  min-height: 100vh;
  overflow: hidden;
  color: var(--portal-text);
  background-image:
    linear-gradient(180deg, rgba(247, 252, 255, 0.04) 0%, rgba(247, 252, 255, 0.1) 44%, rgba(247, 252, 255, 0.72) 78%, var(--ip-neutral-50) 100%),
    url('@/assets/portal/portal-home-bg.png');
  background-position:
    center top,
    center -104px;
  background-repeat: no-repeat;
  background-size:
    100% 100%,
    cover;
}

.home-shell {
  min-height: 100vh;
  padding: 32px 48px;
}

@media (max-width: 1460px) {
  .home-shell {
    padding: 28px 32px;
  }
}

@media (max-width: 1180px) {
  .portal-home {
    background-position:
      center top,
      center -48px;
  }
}

@media (max-width: 1023px) {
  .portal-home {
    overflow: auto;
  }
}

@media (max-width: 640px) {
  .home-shell {
    padding: 18px;
  }
}
</style>
