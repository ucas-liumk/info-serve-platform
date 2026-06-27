<template>
  <main class="portal-home">
    <section class="home-shell">
      <header class="topbar">
        <div class="brand">
          <img class="brand-logo" :src="logoUrl" alt="信息中心数智服务平台" />
          <div class="brand-copy">
            <h1>信息中心数智服务平台</h1>
            <p>数智驱动 · 高效服务 · 共创共享</p>
          </div>
        </div>

        <div class="status-panel">
          <div class="status-row date-row">
            <span>{{ dateText }}</span>
            <span class="divider" aria-hidden="true"></span>
            <span>{{ timeText }}</span>
            <span class="divider" aria-hidden="true"></span>
            <span>{{ weekText }}</span>
          </div>
          <div class="status-row user-row">
            <span class="weather">
              <el-icon><Sunny /></el-icon>
              <b>26°C</b>
              <em>晴</em>
            </span>
            <span class="divider" aria-hidden="true"></span>
            <button class="admin-pill" type="button" @click="goAdmin">
              <span class="avatar">
                <el-icon><UserFilled /></el-icon>
              </span>
              <span>{{ userLabel }}</span>
              <el-icon class="down"><ArrowDown /></el-icon>
            </button>
          </div>
        </div>
      </header>

      <nav class="module-grid" aria-label="数智服务入口">
        <button v-for="item in modules" :key="item.title" class="module-card" type="button" @click="openModule(item)">
          <span class="module-visual">
            <img :src="item.image" :alt="item.title" />
          </span>
          <span class="module-title">{{ item.title }}</span>
          <span class="module-desc">{{ item.desc }}</span>
          <span class="module-action" aria-hidden="true">
            <IconArrowRight />
          </span>
        </button>
      </nav>

      <section class="stats-band" v-loading="loading">
        <div class="stats-heading">
          <strong>数智服务概览</strong>
          <span>SERVICE OVERVIEW</span>
        </div>
        <div class="stats-list">
          <div v-for="item in statsItems" :key="item.label" class="stat-item">
            <span class="stat-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="stat-copy">
              <em>{{ item.label }}</em>
              <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
            </span>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ArrowDown,
  Calendar,
  ChatLineRound,
  Collection,
  Connection,
  Service,
  Sunny,
  UserFilled
} from '@element-plus/icons-vue';
import { getPortalStats } from '@/api/infoservice/portal';
import { PortalStats } from '@/api/infoservice/types';
import logoUrl from '@/assets/portal/home-logo.png';
import moduleResource from '@/assets/portal/module-resource.png';
import moduleTools from '@/assets/portal/module-tools.png';
import moduleQa from '@/assets/portal/module-qa.png';
import moduleHot from '@/assets/portal/module-hot.png';
import moduleForum from '@/assets/portal/module-forum.png';
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';

interface HomeModule {
  title: string;
  desc: string;
  image: string;
  path?: string;
}

const router = useRouter();
const now = ref(new Date());
const loading = ref(false);
let timer: ReturnType<typeof setInterval> | undefined;

const stats = ref<PortalStats>({
  resourceCount: 0,
  toolCount: 0,
  topicCount: 0,
  activeUserCount: 0,
  todayVisitCount: 0
});

const modules: HomeModule[] = [
  { title: '资料共享', desc: '数据汇聚  共享共用', image: moduleResource, path: '/portal/resources' },
  { title: '工具即用', desc: '开箱即用  提升效率', image: moduleTools, path: '/portal/tools' },
  { title: '智能问答', desc: '智慧问答  快速响应', image: moduleQa },
  { title: '时事热点', desc: '热点速递  洞察先机', image: moduleHot },
  { title: '服务论坛', desc: '交流互动  共建共治', image: moduleForum, path: '/portal/forum' }
];

const userLabel = computed(() => '管理员');

const dateText = computed(() => {
  const year = now.value.getFullYear();
  const month = `${now.value.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.value.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
});

const timeText = computed(() => {
  const hour = `${now.value.getHours()}`.padStart(2, '0');
  const minute = `${now.value.getMinutes()}`.padStart(2, '0');
  const second = `${now.value.getSeconds()}`.padStart(2, '0');
  return `${hour}:${minute}:${second}`;
});

const weekText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.getDay()]);

const formatNumber = (value: number) => Number(value || 0).toLocaleString('en-US');

const statsItems = computed(() => {
  const serviceTotal =
    Number(stats.value.resourceCount || 0) +
    Number(stats.value.toolCount || 0) +
    Number(stats.value.topicCount || 0) +
    Number(stats.value.todayVisitCount || 0);

  return [
    { label: '数据资源总量', value: formatNumber(stats.value.resourceCount), unit: '个', icon: Collection },
    { label: '服务调用总量', value: formatNumber(serviceTotal), unit: '次', icon: Service },
    { label: '活跃用户数', value: formatNumber(stats.value.activeUserCount), unit: '人', icon: UserFilled },
    { label: '智能问答次数', value: formatNumber(stats.value.topicCount), unit: '次', icon: ChatLineRound },
    { label: '今日访问量', value: formatNumber(stats.value.todayVisitCount), unit: '次', icon: Calendar },
    { label: '在线服务数', value: formatNumber(stats.value.toolCount), unit: '个', icon: Connection }
  ];
});

const openModule = (item: HomeModule) => {
  if (item.path) {
    router.push(item.path);
    return;
  }
  ElMessage.info(`${item.title}将在后续版本开放`);
};

const goAdmin = () => {
  router.push('/index');
};

onMounted(async () => {
  timer = setInterval(() => {
    now.value = new Date();
  }, 1000);

  loading.value = true;
  try {
    const res: any = await getPortalStats();
    stats.value = { ...stats.value, ...(res.data || {}) };
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped>
.portal-home {
  --portal-max: 1544px;
  --portal-blue: #082b68;
  --portal-text: #071f4b;
  --portal-muted: rgba(7, 31, 75, 0.72);

  min-height: 100vh;
  overflow: hidden;
  color: var(--portal-text);
  background-image:
    linear-gradient(180deg, rgba(247, 252, 255, 0.04) 0%, rgba(247, 252, 255, 0.1) 44%, rgba(247, 252, 255, 0.68) 78%, #f7fbff 100%),
    url("@/assets/portal/home-great-wall.png");
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

.topbar {
  max-width: var(--portal-max);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand-logo {
  width: 64px;
  height: 64px;
  object-fit: contain;
  filter: drop-shadow(0 12px 18px rgba(37, 116, 214, 0.2));
}

.brand-copy h1 {
  margin: 0 0 8px;
  color: var(--portal-blue);
  font-size: 38px;
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: 0;
  text-shadow: 0 2px 10px rgba(255, 255, 255, 0.62);
}

.brand-copy p {
  margin: 0;
  color: rgba(8, 43, 104, 0.78);
  font-size: 21px;
  line-height: 1.25;
  font-weight: 600;
}

.status-panel {
  width: 352px;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 20px;
  background: rgba(230, 244, 255, 0.74);
  box-shadow: 0 16px 42px rgba(44, 112, 184, 0.14), inset 0 1px 0 rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px);
}

.status-row {
  display: flex;
  align-items: center;
  color: #09285d;
  white-space: nowrap;
}

.date-row {
  height: 28px;
  justify-content: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
}

.divider {
  width: 1px;
  height: 22px;
  display: inline-block;
  background: rgba(48, 96, 151, 0.36);
}

.user-row {
  justify-content: center;
  gap: 20px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(52, 100, 151, 0.16);
}

.weather {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
}

.weather .el-icon {
  color: #ffb31c;
  font-size: 25px;
  filter: drop-shadow(0 5px 9px rgba(255, 173, 35, 0.28));
}

.weather em {
  font-style: normal;
  font-weight: 600;
}

.admin-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--portal-blue);
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
}

.avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #6fb1ff 0%, #1f68e3 100%);
  color: #fff;
  box-shadow: 0 8px 14px rgba(33, 103, 220, 0.24);
}

.avatar .el-icon {
  font-size: 22px;
}

.down {
  font-size: 16px;
}

.module-grid {
  max-width: 1440px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
  margin: 148px auto 0;
}

.module-card {
  position: relative;
  height: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 20px 20px;
  border: 1px solid rgba(207, 224, 245, 0.92);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 252, 255, 0.94) 100%);
  box-shadow: 0 18px 42px rgba(35, 90, 151, 0.14), inset 0 1px 0 rgba(255, 255, 255, 0.96);
  color: var(--portal-blue);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.module-card::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(circle at 50% 18%, rgba(54, 142, 255, 0.16), transparent 46%);
  opacity: 0;
  transition: opacity 0.18s ease;
  pointer-events: none;
}

.module-card:hover {
  transform: translateY(-6px);
  border-color: rgba(122, 179, 244, 0.92);
  box-shadow: 0 28px 58px rgba(32, 98, 180, 0.2), inset 0 1px 0 rgba(255, 255, 255, 1);
}

.module-card:hover::before {
  opacity: 1;
}

.module-visual {
  width: 166px;
  height: 166px;
  display: block;
  margin-bottom: 14px;
}

.module-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.module-title {
  position: relative;
  z-index: 1;
  color: var(--portal-blue);
  font-size: 28px;
  line-height: 1.15;
  font-weight: 800;
}

.module-desc {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--portal-muted);
  font-size: 16px;
  line-height: 1.35;
  font-weight: 600;
}

.module-action {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  flex: 0 0 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 14px;
  box-sizing: border-box;
  border-radius: 50%;
  background: linear-gradient(180deg, #4fb0ff 0%, #2e86f5 52%, #1f6fe5 100%);
  color: #fff;
  line-height: 0;
  box-shadow:
    inset 0 1px 2px rgba(255, 255, 255, 0.72),
    inset 0 -4px 7px rgba(20, 91, 204, 0.36),
    0 8px 13px rgba(36, 116, 236, 0.28);
}

.module-action svg {
  width: 21px;
  height: 21px;
  color: #fff;
  transform: translateX(1px);
  filter: drop-shadow(0 1px 1px rgba(7, 61, 148, 0.2));
}

.stats-band {
  max-width: var(--portal-max);
  min-height: 108px;
  display: grid;
  grid-template-columns: 196px 1fr;
  align-items: center;
  gap: 24px;
  margin: 24px auto 0;
  padding: 18px 28px;
  border: 1px solid rgba(211, 226, 244, 0.96);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 20px 46px rgba(35, 85, 146, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
}

.stats-heading {
  min-height: 70px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid rgba(70, 113, 160, 0.3);
}

.stats-heading strong {
  color: #0a54aa;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 800;
}

.stats-heading span {
  margin-top: 10px;
  color: rgba(7, 31, 75, 0.56);
  font-size: 14px;
  line-height: 1;
  font-weight: 500;
}

.stats-list {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.stat-item {
  min-height: 68px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  border-right: 1px solid rgba(70, 113, 160, 0.24);
}

.stat-item:last-child {
  border-right: 0;
}

.stat-icon {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #eff8ff 0%, #cfe9ff 100%);
  color: #1f74e8;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88), 0 10px 20px rgba(42, 114, 206, 0.12);
}

.stat-icon .el-icon {
  font-size: 28px;
}

.stat-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-copy em {
  color: rgba(7, 31, 75, 0.72);
  font-size: 14px;
  line-height: 1;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.stat-copy strong {
  color: var(--portal-blue);
  font-size: 24px;
  line-height: 1;
  font-weight: 800;
  white-space: nowrap;
}

.stat-copy small {
  margin-left: 6px;
  color: var(--portal-blue);
  font-size: 15px;
  font-weight: 650;
}

@media (max-width: 1460px) {
  .home-shell {
    padding: 28px 32px;
  }

  .brand-logo {
    width: 58px;
    height: 58px;
  }

  .brand-copy h1 {
    font-size: 33px;
  }

  .brand-copy p {
    font-size: 18px;
  }

  .status-panel {
    width: 336px;
    padding: 12px 16px;
    border-radius: 18px;
  }

  .module-grid {
    gap: 16px;
    margin-top: 128px;
  }

  .module-card {
    height: 318px;
    padding-top: 16px;
  }

  .module-visual {
    width: 148px;
    height: 148px;
  }

  .module-title {
    font-size: 25px;
  }

  .module-desc {
    font-size: 15px;
  }

  .stats-band {
    grid-template-columns: 184px 1fr;
    padding: 16px 24px;
  }

  .stat-item {
    gap: 10px;
    padding-inline: 10px;
  }

  .stat-copy strong {
    font-size: 22px;
  }
}

@media (max-width: 1180px) {
  .portal-home {
    background-position:
      center top,
      center -48px;
  }

  .module-grid {
    margin-top: 96px;
  }

  .module-card {
    height: 288px;
    padding: 14px 14px 18px;
  }

  .module-visual {
    width: 124px;
    height: 124px;
  }

  .module-title {
    font-size: 22px;
  }

  .module-desc {
    font-size: 14px;
  }

  .module-action {
    width: 34px;
    height: 34px;
    min-width: 34px;
    min-height: 34px;
    flex-basis: 34px;
    margin-top: 12px;
  }

  .stats-band,
  .stats-list {
    grid-template-columns: 1fr;
  }

  .stats-band {
    align-items: stretch;
    gap: 16px;
    margin-top: 24px;
  }

  .stats-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    row-gap: 0;
  }

  .stats-heading {
    min-height: auto;
    padding-bottom: 14px;
    border-right: 0;
    border-bottom: 1px solid rgba(70, 113, 160, 0.22);
  }

  .stat-item {
    min-height: 76px;
    border-bottom: 1px solid rgba(70, 113, 160, 0.18);
  }

  .stat-item:nth-child(3n) {
    border-right: 0;
  }

  .stat-item:last-child {
    border-bottom: 0;
  }
}

@media (max-width: 1023px) {
  .portal-home {
    overflow: auto;
  }

  .topbar {
    gap: 20px;
  }

  .brand-logo {
    width: 54px;
    height: 54px;
  }

  .brand-copy h1 {
    font-size: 30px;
  }

  .brand-copy p {
    font-size: 16px;
  }

  .status-panel {
    width: 328px;
  }

  .date-row {
    font-size: 16px;
  }

  .weather,
  .admin-pill {
    font-size: 15px;
  }

  .module-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-top: 64px;
  }

  .module-card {
    height: 252px;
  }

  .module-visual {
    width: 112px;
    height: 112px;
    margin-bottom: 10px;
  }

  .module-title {
    font-size: 21px;
  }
}

@media (max-width: 840px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-panel {
    width: 100%;
    max-width: 360px;
  }
}

@media (max-width: 767px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-top: 48px;
  }

  .stats-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-item:nth-child(3n) {
    border-right: 1px solid rgba(70, 113, 160, 0.24);
  }

  .stat-item:nth-child(2n) {
    border-right: 0;
  }
}

@media (max-width: 640px) {
  .home-shell {
    padding: 18px;
  }

  .brand {
    gap: 12px;
  }

  .brand-logo {
    width: 48px;
    height: 48px;
  }

  .brand-copy h1 {
    font-size: 25px;
  }

  .brand-copy p {
    font-size: 15px;
  }

  .status-panel {
    padding: 14px;
    border-radius: 18px;
  }

  .date-row {
    gap: 9px;
    font-size: 15px;
  }

  .user-row {
    gap: 12px;
  }

  .weather,
  .admin-pill {
    font-size: 15px;
  }

  .module-grid {
    margin-top: 40px;
  }

  .module-card {
    height: 236px;
  }

  .module-visual {
    width: 104px;
    height: 104px;
  }

  .module-title {
    font-size: 20px;
  }

  .stats-band {
    padding: 20px;
    border-radius: 18px;
  }
}

@media (max-width: 520px) {
  .module-grid,
  .stats-list {
    grid-template-columns: 1fr;
  }

  .module-card {
    height: 232px;
  }

  .stat-item,
  .stat-item:nth-child(2n),
  .stat-item:nth-child(3n) {
    border-right: 0;
  }
}
</style>
