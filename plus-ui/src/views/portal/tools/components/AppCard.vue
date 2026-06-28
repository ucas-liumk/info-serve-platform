<template>
  <article class="app-card">
    <button
      :class="['favorite-toggle', { on: app.favorited }]"
      type="button"
      :title="app.favorited ? '取消收藏' : '收藏工具'"
      :aria-label="app.favorited ? '取消收藏' : '收藏工具'"
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

    <footer class="card-foot">
      <span class="metric">
        <el-icon><Clock /></el-icon>
        使用 {{ app.useCount || 0 }} 次
      </span>
      <span :class="['metric favorite-count', { on: app.favorited }]">
        <el-icon>
          <StarFilled v-if="app.favorited" />
          <Star v-else />
        </el-icon>
        收藏 {{ app.favoriteCount || 0 }} 次
      </span>
    </footer>

    <button class="use-btn" type="button" @click="onUse">
      <span>立即使用</span>
      <IconArrowRight />
    </button>
  </article>
</template>

<script setup lang="ts">
import { computed, markRaw, ref } from 'vue';
import { Clock, Star, StarFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { PortalApp } from '@/api/appcenter/types';
import { useApp, favorite, unfavorite } from '@/api/appcenter/portal';
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';
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
  pdf: markRaw(IconPdf)
} as const;

const colorMap: Record<string, string> = {
  blue: '#1677ff',
  violet: '#5b21f6',
  slate: '#7e8aa3',
  orange: '#ff7a1a',
  red: '#ef1d1d',
  cyan: '#03a8c7',
  black: '#111827',
  green: '#16a34a',
  '#2563eb': '#2563eb',
  '#0f766e': '#0f766e',
  '#c2410c': '#c2410c'
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
const appColor = computed(() => colorMap[props.app.accent || ''] || props.app.accent || '#2563eb');

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
    ElMessage.warning('该工具暂未配置访问地址');
  }
  emit('changed');
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
  display: flex;
  flex-direction: column;
  padding: 20px 18px 16px;
  border: 1px solid #e4ebf7;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 28px rgba(27, 72, 145, 0.06);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.app-card:hover {
  transform: translateY(-2px);
  border-color: #bdd2f5;
  box-shadow: 0 18px 38px rgba(37, 99, 235, 0.12);
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
  border: 1px solid #d8e3f4;
  border-radius: 50%;
  background: #fff;
  color: #7890b8;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(31, 84, 156, 0.08);
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
  border-color: #9cbdf5;
  color: #1260e8;
  box-shadow: 0 12px 24px rgba(18, 96, 232, 0.14);
}

.favorite-toggle.on {
  border-color: #1260e8;
  background: #1260e8;
  color: #fff;
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
  border-radius: 8px;
  background: linear-gradient(145deg, var(--app-color), color-mix(in srgb, var(--app-color) 78%, #001f60));
  color: #fff;
  box-shadow:
    0 10px 22px rgba(42, 105, 210, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.app-logo svg {
  width: 28px;
  height: 28px;
}

.app-logo-text {
  font-size: 15px;
  line-height: 1;
  font-weight: 800;
}

.app-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.app-meta strong {
  overflow: hidden;
  color: #0f1f3d;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-meta em {
  color: #51658f;
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
  color: #25395f;
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
  background: #edf4ff;
  color: #1260e8;
  font-size: 12px;
  line-height: 1;
  font-weight: 700;
}

.card-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  color: #50648d;
  font-size: 12px;
  font-weight: 650;
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
  color: #1260e8;
}

.use-btn {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: auto;
  border: 1px solid #b7cef7;
  border-radius: 6px;
  background: #fff;
  color: #1260e8;
  font-size: 14px;
  line-height: 1;
  font-weight: 800;
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
  border-color: #1260e8;
  background: #1260e8;
  color: #fff;
  box-shadow: 0 10px 22px rgba(18, 96, 232, 0.2);
}

@media (max-width: 1500px) {
  .app-card {
    min-height: 270px;
    padding: 18px 16px 15px;
  }

  .app-meta strong {
    font-size: 17px;
  }
}
</style>
