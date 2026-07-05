<template>
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
        <PortalNotificationBell />
        <span class="divider" aria-hidden="true"></span>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <button class="user-pill" type="button">
            <span class="avatar">
              <img v-if="userStore.avatar" :src="userStore.avatar" alt="" />
              <el-icon v-else><UserFilled /></el-icon>
            </span>
            <span>{{ userLabel }}</span>
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
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { ArrowDown, Sunny, UserFilled } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import logoUrl from '@/assets/portal/home-logo.png';

const emit = defineEmits<{ (e: 'command', command: string | number | object): void }>();

const userStore = useUserStore();
const now = ref(new Date());
let timer: ReturnType<typeof setInterval> | undefined;

const userLabel = computed(() => userStore.nickname || userStore.name || '当前用户');

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

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date();
  }, 60000);
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
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

.status-panel {
  width: 390px;
  padding: 14px 18px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 16px;
  background: rgba(230, 244, 255, 0.74);
  box-shadow: var(--ip-shadow-md);
  backdrop-filter: blur(14px);
}

.status-row {
  display: flex;
  align-items: center;
  color: var(--ip-neutral-800);
  white-space: nowrap;
}

.date-row {
  height: 28px;
  justify-content: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
}

.divider {
  width: 1px;
  height: 22px;
  display: inline-block;
  background: var(--ip-neutral-300);
}

.user-row {
  justify-content: center;
  gap: 14px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--ip-neutral-200);
}

.weather {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
}

.weather .el-icon {
  color: var(--ip-warning);
  font-size: 25px;
  filter: drop-shadow(0 5px 9px rgba(255, 173, 35, 0.28));
}

.weather em {
  font-style: normal;
  font-weight: 600;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ip-primary-900);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.user-pill:hover,
.user-pill:focus-visible {
  color: var(--ip-primary-600);
  outline: none;
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

  .status-panel {
    width: 336px;
    padding: 12px 16px;
    border-radius: 16px;
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
}
</style>
