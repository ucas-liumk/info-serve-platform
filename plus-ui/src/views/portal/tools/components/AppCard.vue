<template>
  <article class="app-card">
    <button
      :class="['favorite-toggle', { on: app.favorited }]"
      type="button"
      :title="app.favorited ? '取消收藏' : '收藏应用'"
      :aria-label="app.favorited ? '取消收藏' : '收藏应用'"
      :disabled="favoritePending"
      @click.stop="onFav"
    >
      <el-icon>
        <StarFilled v-if="app.favorited" />
        <Star v-else />
      </el-icon>
    </button>

    <header class="card-head">
      <span class="app-logo" :style="{ '--app-color': appColor }">
        <component v-if="iconComponent" :is="iconComponent" />
        <span v-else class="app-logo-text">{{ fallbackIcon }}</span>
      </span>
      <span class="app-meta">
        <strong>{{ app.appName }}</strong>
        <em>{{ app.version || 'latest' }}</em>
      </span>
    </header>

    <p class="app-desc">{{ app.description }}</p>

    <div class="tags">
      <span v-for="tag in tagList" :key="tag">{{ tag }}</span>
    </div>

    <div v-if="isOfflineApp" class="package-meta">
      <el-icon><Download /></el-icon>
      <span>{{ app.packageName || '离线安装包' }}</span>
      <em v-if="app.packageSize">{{ formatFileSize(app.packageSize) }}</em>
    </div>

    <footer class="card-foot">
      <span class="metric">
        <el-icon><Clock /></el-icon>
        使用 {{ useCountText }}<span v-if="useCountText !== '—'"> 次</span>
      </span>
      <span :class="['metric favorite-count', { on: app.favorited }]">
        <el-icon>
          <StarFilled v-if="app.favorited" />
          <Star v-else />
        </el-icon>
        收藏 {{ favoriteCountText }}<span v-if="favoriteCountText !== '—'"> 次</span>
      </span>
    </footer>

    <button :class="['use-btn', { download: isOfflineApp }]" type="button" @click="onPrimaryAction">
      <span>{{ primaryActionText }}</span>
      <IconArrowRight />
    </button>
  </article>
</template>

<script setup lang="ts">
import { computed, markRaw, ref } from 'vue';
import { Clock, Download, Star, StarFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import type { PortalApp } from '@/api/appcenter/types';
import { useApp, favorite, unfavorite } from '@/api/portal/appcenter';
import { formatStat } from '@/utils/format';
import { downloadPortalAppPackage } from '../download';
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';
import IconEducation from '~icons/material-symbols/school-outline-rounded';
import IconDify from '~icons/simple-icons/dify';
import IconAirflow from '~icons/simple-icons/apacheairflow';
import IconMinio from '~icons/simple-icons/minio';
import IconGrafana from '~icons/simple-icons/grafana';
import IconRedis from '~icons/simple-icons/redis';
import IconNginx from '~icons/simple-icons/nginx';
import IconN8n from '~icons/simple-icons/n8n';
import IconPortainer from '~icons/simple-icons/portainer';
import IconTensorflow from '~icons/simple-icons/tensorflow';
import IconClickhouse from '~icons/simple-icons/clickhouse';
import IconSuperset from '~icons/simple-icons/apachesuperset';
import IconVault from '~icons/simple-icons/vault';
import IconPrometheus from '~icons/simple-icons/prometheus';
import IconK3s from '~icons/simple-icons/k3s';
import IconGitea from '~icons/simple-icons/gitea';
import IconDrawio from '~icons/simple-icons/diagramsdotnet';
import IconExcalidraw from '~icons/simple-icons/excalidraw';
import IconPdf from '~icons/material-symbols/picture-as-pdf-outline-rounded';

const props = defineProps<{ app: PortalApp }>();
const emit = defineEmits<{ (e: 'changed'): void }>();
const favoritePending = ref(false);

const iconMap = {
  dify: markRaw(IconDify),
  airflow: markRaw(IconAirflow),
  apacheairflow: markRaw(IconAirflow),
  minio: markRaw(IconMinio),
  grafana: markRaw(IconGrafana),
  redis: markRaw(IconRedis),
  openresty: markRaw(IconNginx),
  nginx: markRaw(IconNginx),
  n8n: markRaw(IconN8n),
  portainer: markRaw(IconPortainer),
  tensorflow: markRaw(IconTensorflow),
  clickhouse: markRaw(IconClickhouse),
  superset: markRaw(IconSuperset),
  apachesuperset: markRaw(IconSuperset),
  vault: markRaw(IconVault),
  prometheus: markRaw(IconPrometheus),
  k3s: markRaw(IconK3s),
  gitea: markRaw(IconGitea),
  drawio: markRaw(IconDrawio),
  diagramsdotnet: markRaw(IconDrawio),
  excalidraw: markRaw(IconExcalidraw),
  stirlingpdf: markRaw(IconPdf),
  pdf: markRaw(IconPdf),
  requiredknowledge: markRaw(IconEducation)
} as const;

const colorMap: Record<string, string> = {
  blue: 'var(--ip-primary-600)',
  violet: 'var(--ip-mod-qa)',
  slate: 'var(--ip-neutral-500)',
  orange: 'var(--ip-mod-appcenter)',
  red: 'var(--ip-danger)',
  cyan: 'var(--ip-mod-resources)',
  black: 'var(--ip-neutral-900)',
  green: 'var(--ip-success)',
  '#2563eb': 'var(--ip-primary-600)',
  '#0f766e': 'var(--ip-mod-resources)',
  '#c2410c': 'var(--ip-mod-appcenter)'
};

const normalizeKey = (value?: string | number) =>
  String(value || '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
const appKey = computed(() => normalizeKey((props.app as any).appCode || props.app.appName));
const iconComponent = computed(() => iconMap[appKey.value as keyof typeof iconMap]);
const tagList = computed(() =>
  (props.app.tags || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3)
);
const fallbackIcon = computed(() => (props.app.icon || props.app.appName.slice(0, 2)).slice(0, 3));
const appColor = computed(() => colorMap[props.app.accent || ''] || props.app.accent || 'var(--ip-mod-appcenter)');
const useCountText = computed(() => formatStat(props.app.useCount));
const favoriteCountText = computed(() => formatStat(props.app.favoriteCount));
const isOfflineApp = computed(() => props.app.appType === 'offline');
const primaryActionText = computed(() => (isOfflineApp.value ? '下载离线包' : '立即使用'));

const formatFileSize = (size?: number) => {
  if (!size) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

const normalizeToolUrl = (raw: string) => {
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

const onUse = async () => {
  const res: any = await useApp(props.app.appId);
  const url = res.data;
  if (url) {
    window.open(normalizeToolUrl(url), '_blank');
  } else {
    ElMessage.warning('该应用暂未配置访问地址');
  }
  emit('changed');
};

const onPrimaryAction = async () => {
  if (isOfflineApp.value) {
    await downloadPortalAppPackage(props.app);
    return;
  }
  await onUse();
};

const onFav = async () => {
  if (favoritePending.value) return;
  favoritePending.value = true;
  try {
    props.app.favorited ? await unfavorite(props.app.appId) : await favorite(props.app.appId);
    emit('changed');
  } finally {
    favoritePending.value = false;
  }
};
</script>

<style scoped>
.app-card {
  position: relative;
  min-height: 282px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 20px 18px 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--ip-shadow-sm);
  transition:
    transform var(--ip-motion-base) var(--ip-motion-ease),
    box-shadow var(--ip-motion-base) var(--ip-motion-ease),
    border-color var(--ip-motion-base) var(--ip-motion-ease);
}

.app-card:hover {
  transform: translateY(-2px);
  border-color: var(--ip-primary-200);
  box-shadow: var(--ip-shadow-md);
}

.card-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-right: 40px;
}

.favorite-toggle {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 999px;
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-500);
  cursor: pointer;
  box-shadow: var(--ip-shadow-sm);
  transition:
    transform 0.16s ease,
    border-color 0.16s ease,
    background 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease;
}

.favorite-toggle .el-icon {
  font-size: 18px;
}

.favorite-toggle:hover {
  transform: translateY(-1px);
  border-color: var(--ip-primary-200);
  color: var(--ip-primary-600);
  box-shadow: var(--ip-shadow-md);
}

.favorite-toggle.on {
  border-color: var(--ip-primary-600);
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
}

.favorite-toggle:disabled {
  cursor: progress;
  opacity: 0.72;
}

.app-logo {
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 10px;
  background: linear-gradient(145deg, var(--app-color), color-mix(in srgb, var(--app-color) 78%, #001f60));
  color: var(--ip-neutral-0);
  box-shadow:
    var(--ip-shadow-md),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.app-logo svg {
  width: 28px;
  height: 28px;
}

.app-logo-text {
  font-size: 14px;
  line-height: 1;
  font-weight: 700;
}

.app-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.app-meta strong {
  overflow: hidden;
  color: var(--ip-neutral-900);
  font-size: 16px;
  line-height: 1.2;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-meta em {
  color: var(--ip-neutral-500);
  font-size: 13px;
  line-height: 1;
  font-style: normal;
  font-weight: 600;
}

.app-desc {
  min-height: 58px;
  display: -webkit-box;
  overflow: hidden;
  margin: 16px 0 0;
  color: var(--ip-neutral-700);
  font-size: 14px;
  line-height: 1.58;
  font-weight: 600;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.tags {
  min-height: 28px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.tags span {
  height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 9px;
  border-radius: 6px;
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
  font-size: 12px;
  line-height: 1;
  font-weight: 700;
}

.package-meta {
  min-height: 28px;
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 10px;
  overflow: hidden;
  color: var(--tool-muted);
  font-size: 12px;
  font-weight: 800;
}

.package-meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.package-meta em {
  flex: 0 0 auto;
  color: var(--tool-weak);
  font-style: normal;
}

.card-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  color: var(--ip-neutral-500);
  font-size: 12px;
  font-weight: 600;
}

.metric {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.metric .el-icon {
  font-size: 15px;
}

.favorite-count.on {
  color: var(--ip-primary-600);
}

.use-btn {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: auto;
  border: 1px solid var(--ip-primary-200);
  border-radius: 6px;
  background: var(--ip-neutral-0);
  color: var(--ip-primary-600);
  font-size: 14px;
  line-height: 1;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease;
}

.use-btn svg {
  width: 18px;
  height: 18px;
}

.use-btn:hover {
  border-color: var(--ip-primary-600);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
  box-shadow: var(--ip-shadow-md);
}

.use-btn.download {
  border-color: var(--tool-accent);
  color: var(--tool-accent);
}

.use-btn.download:hover {
  border-color: var(--tool-accent);
  background: var(--tool-accent);
  color: #fff;
  box-shadow: 0 10px 22px rgba(183, 121, 31, 0.2);
}

@media (max-width: 1500px) {
  .app-card {
    min-height: 270px;
    padding: 18px 16px 15px;
  }

  .app-meta strong {
    font-size: 16px;
  }
}
</style>
