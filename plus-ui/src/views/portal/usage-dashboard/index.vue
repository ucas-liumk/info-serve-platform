<template>
  <main class="usage-dashboard-entry" v-loading="redirecting">
    <el-card class="usage-dashboard-card" shadow="never">
      <el-result
        :icon="configured ? 'success' : 'warning'"
        :title="configured ? '正在打开应用态势大屏' : 'DataEase 态势大屏未配置'"
        :sub-title="configured ? '如果没有自动跳转，请点击下方按钮重新打开。' : '请先在 DataEase 发布大屏链接，并配置门户运行时地址。'"
      >
        <template #extra>
          <el-space wrap>
            <el-button v-if="configured" type="primary" @click="openDashboard">打开态势大屏</el-button>
            <el-button @click="goHome">返回门户首页</el-button>
          </el-space>
          <el-alert
            v-if="!configured"
            class="usage-dashboard-alert"
            title="配置方式：修改 plus-ui/public/app-config.js 的 dataeaseDashboardUrl，或在构建前设置 VITE_APP_DATAEASE_DASHBOARD_URL。"
            type="info"
            show-icon
            :closable="false"
          />
        </template>
      </el-result>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { PORTAL_HOME_PATH } from '@/constants/router';

const router = useRouter();
const redirecting = ref(false);

const configuredUrl = computed(() => {
  const runtimeUrl = window.__INFO_SERVE_CONFIG__?.dataeaseDashboardUrl;
  return normalizeServiceUrl(runtimeUrl || import.meta.env.VITE_APP_DATAEASE_DASHBOARD_URL || '');
});
const configured = computed(() => Boolean(configuredUrl.value));

const normalizeServiceUrl = (raw: string) => {
  const value = raw.trim();
  if (!value) {
    return '';
  }

  try {
    const url = new URL(value, window.location.origin);
    if (['127.0.0.1', 'localhost'].includes(url.hostname)) {
      url.hostname = window.location.hostname;
    }
    return url.toString();
  } catch {
    return value;
  }
};

const openDashboard = () => {
  if (!configured.value) {
    return;
  }
  redirecting.value = true;
  window.location.replace(configuredUrl.value);
};

const goHome = () => {
  void router.push(PORTAL_HOME_PATH);
};

onMounted(() => {
  if (configured.value) {
    window.setTimeout(openDashboard, 160);
  }
});
</script>

<style scoped>
.usage-dashboard-entry {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ip-font-title);
}

.usage-dashboard-card {
  width: min(720px, 100%);
}

.usage-dashboard-alert {
  margin-top: var(--ip-font-title);
  text-align: left;
}
</style>
