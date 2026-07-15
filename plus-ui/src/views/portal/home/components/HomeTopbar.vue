<template>
  <header class="topbar">
    <div class="brand">
      <img class="brand-logo" :src="logoUrl" alt="信息中心数智服务平台" />
      <div class="brand-copy">
        <h1>信息中心数智服务平台</h1>
        <p>数智驱动 · 高效服务 · 共创共享</p>
      </div>
    </div>

    <div class="utility-toolbar">
      <div class="date-group" :aria-label="`${dateText} ${weekText} ${timeText}`">
        <strong>{{ timeText }}</strong>
        <span>{{ dateText }} · {{ weekText }}</span>
      </div>

      <el-dropdown class="user-menu" trigger="click" @command="handleUserCommand">
        <button class="user-pill" type="button" :aria-label="`打开${userLabel}的用户菜单`">
          <span class="avatar">
            <img v-if="showAvatar" :src="userStore.avatar" alt="" @error="handleAvatarError" />
            <el-icon v-else><UserFilled /></el-icon>
          </span>
          <span class="user-label">{{ userLabel }}</span>
          <el-icon class="down"><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <div class="quick-actions">
        <button class="manual-entry" type="button" title="系统使用手册" aria-label="系统使用手册" @click="handleManualClick">
          <el-icon><Memo /></el-icon>
        </button>

        <span class="notification-entry">
          <PortalNotificationBell />
        </span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ArrowDown, Memo, UserFilled } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import logoUrl from '@/assets/portal/home-logo.png';

const emit = defineEmits<{
  (e: 'command', command: string | number | object): void;
  (e: 'open-manual'): void;
}>();

const userStore = useUserStore();
const now = ref(new Date());
const avatarFailed = ref(false);
let minuteTimeout: number | undefined;
let minuteTimer: number | undefined;

const userLabel = computed(() => userStore.nickname || userStore.name || '当前用户');
const showAvatar = computed(() => Boolean(userStore.avatar) && !avatarFailed.value);

const dateText = computed(() => {
  const year = now.value.getFullYear();
  const month = `${now.value.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.value.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
});

const timeText = computed(() => {
  const hour = `${now.value.getHours()}`.padStart(2, '0');
  const minute = `${now.value.getMinutes()}`.padStart(2, '0');
  return `${hour}:${minute}`;
});

const weekText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.getDay()]);

const handleUserCommand = (command: string | number | object) => emit('command', command);
const handleManualClick = () => emit('open-manual');
const handleAvatarError = () => {
  avatarFailed.value = true;
};

watch(
  () => userStore.avatar,
  () => {
    avatarFailed.value = false;
  }
);

onMounted(() => {
  const delayUntilNextMinute = 60000 - (Date.now() % 60000);
  minuteTimeout = window.setTimeout(() => {
    now.value = new Date();
    minuteTimer = window.setInterval(() => {
      now.value = new Date();
    }, 60000);
  }, delayUntilNextMinute);
});

onBeforeUnmount(() => {
  if (minuteTimeout) {
    window.clearTimeout(minuteTimeout);
  }
  if (minuteTimer) {
    window.clearInterval(minuteTimer);
  }
});
</script>

<style scoped>
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
  color: var(--ip-primary-900);
  font-size: 38px;
  line-height: 1.12;
  font-weight: 700;
  letter-spacing: 0;
  text-shadow: 0 2px 10px rgba(255, 255, 255, 0.62);
}

.brand-copy p {
  margin: 0;
  color: var(--ip-neutral-600);
  font-size: 20px;
  line-height: 1.25;
  font-weight: 600;
}

.utility-toolbar {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.date-group {
  min-width: 0;
  padding-right: 16px;
  border-right: 1px solid color-mix(in srgb, var(--ip-neutral-500) 28%, transparent);
  text-align: right;
  white-space: nowrap;
}

.date-group strong,
.date-group span {
  display: block;
}

.date-group strong {
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-title-sm);
  line-height: 1;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.date-group span {
  margin-top: 8px;
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-hint);
  line-height: 1;
  font-weight: 600;
}

.user-menu {
  min-width: 0;
}

.quick-actions,
.notification-entry {
  display: inline-flex;
}

.quick-actions {
  align-items: center;
  gap: 8px;
}

.manual-entry {
  position: relative;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--ip-neutral-300) 72%, transparent);
  border-radius: var(--ip-radius-full);
  background: color-mix(in srgb, var(--ip-neutral-0) 52%, transparent);
  color: var(--ip-neutral-600);
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease),
    box-shadow var(--ip-motion-fast) var(--ip-motion-ease);
}

.manual-entry:hover,
.manual-entry:focus-visible {
  border-color: var(--ip-primary-200);
  background: color-mix(in srgb, var(--ip-neutral-0) 78%, transparent);
  color: var(--ip-primary-700);
}

.manual-entry:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

.manual-entry .el-icon {
  font-size: 19px;
}

.user-pill {
  max-width: 220px;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 4px 10px 4px 4px;
  border: 1px solid transparent;
  border-radius: var(--ip-radius-full);
  background: color-mix(in srgb, var(--ip-neutral-0) 48%, transparent);
  color: var(--ip-primary-900);
  font-size: var(--ip-font-emphasis);
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease),
    box-shadow var(--ip-motion-fast) var(--ip-motion-ease);
}

.user-pill:hover,
.user-pill:focus-visible {
  border-color: color-mix(in srgb, var(--ip-primary-200) 72%, transparent);
  background: color-mix(in srgb, var(--ip-neutral-0) 76%, transparent);
  color: var(--ip-primary-600);
}

.user-pill:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

.user-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
  box-shadow: 0 8px 14px rgba(33, 103, 220, 0.24);
}

.avatar .el-icon {
  font-size: 22px;
}

.avatar img {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
  object-fit: cover;
}

.down {
  font-size: 16px;
}

@media (prefers-reduced-motion: reduce) {
  .user-pill,
  .manual-entry {
    transition: none;
  }
}

@media (max-width: 1460px) {
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

  .utility-toolbar {
    gap: 8px;
  }

  .date-group {
    padding-right: 12px;
  }
}

@media (max-width: 1023px) {
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

  .user-pill {
    max-width: 180px;
  }
}

@media (max-width: 840px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .utility-toolbar {
    width: 100%;
    justify-content: flex-start;
  }

  .user-pill,
  .manual-entry {
    min-height: 44px;
  }

  .manual-entry {
    width: 44px;
    height: 44px;
    flex-basis: 44px;
  }
}

@media (max-width: 640px) {
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

  .utility-toolbar {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    gap: 8px;
  }

  .date-group {
    min-width: 0;
    grid-column: 1;
    grid-row: 1;
    padding-right: 8px;
    text-align: left;
  }

  .date-group span {
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .user-menu {
    min-width: 0;
    grid-column: 2 / 4;
    grid-row: 1;
    justify-self: end;
  }

  .user-pill {
    max-width: 168px;
  }

  .quick-actions {
    grid-column: 2 / 4;
    grid-row: 2;
    justify-self: end;
  }
}
</style>
