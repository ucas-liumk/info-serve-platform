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
        <PortalUserMenu />
        <span class="divider" aria-hidden="true"></span>
        <button class="manual-entry" type="button" title="系统使用手册" aria-label="系统使用手册" @click="handleManualClick">
          <el-icon><Memo /></el-icon>
        </button>
        <span class="divider" aria-hidden="true"></span>
        <PortalNotificationBell />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { Memo } from '@element-plus/icons-vue';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import PortalUserMenu from '@/layout/portal/components/PortalUserMenu.vue';
import logoUrl from '@/assets/portal/home-logo.png';

const emit = defineEmits<{
  (e: 'open-manual'): void;
}>();

const now = ref(new Date());
let timer: ReturnType<typeof setInterval> | undefined;

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

const handleManualClick = () => emit('open-manual');

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

.manual-entry {
  position: relative;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d3dee8;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  color: #58708c;
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(31, 54, 76, 0.07);
  transition:
    border-color 0.16s ease,
    background 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.manual-entry:hover {
  border-color: #b8c9d9;
  background: #eaf2f8;
  color: #245f8f;
  box-shadow: 0 14px 28px rgba(36, 95, 143, 0.12);
  transform: translateY(-1px);
}

.manual-entry .el-icon {
  font-size: 19px;
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

  .admin-pill {
    font-size: 15px;
  }
}
</style>
