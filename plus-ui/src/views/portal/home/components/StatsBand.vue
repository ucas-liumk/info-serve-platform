<template>
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
          <strong
            >{{ item.value }}<small>{{ item.unit }}</small></strong
          >
        </span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Calendar, ChatLineRound, Collection, Connection, Service, UserFilled } from '@element-plus/icons-vue';
import { PortalStats } from '@/api/infoservice/types';
import { formatStat } from '@/utils/format';

const props = defineProps<{ stats: PortalStats; loading: boolean }>();

const statsItems = computed(() => {
  const serviceTotal =
    Number(props.stats.resourceCount || 0) +
    Number(props.stats.toolCount || 0) +
    Number(props.stats.topicCount || 0) +
    Number(props.stats.todayVisitCount || 0);

  return [
    { label: '数据资源总量', value: formatStat(props.stats.resourceCount), unit: '个', icon: Collection },
    { label: '服务调用总量', value: formatStat(serviceTotal), unit: '次', icon: Service },
    { label: '活跃用户数', value: formatStat(props.stats.activeUserCount), unit: '人', icon: UserFilled },
    { label: '论坛话题数', value: formatStat(props.stats.topicCount), unit: '个', icon: ChatLineRound },
    { label: '今日访问量', value: formatStat(props.stats.todayVisitCount), unit: '次', icon: Calendar },
    { label: '在线服务数', value: formatStat(props.stats.toolCount), unit: '个', icon: Connection }
  ];
});
</script>

<style scoped>
.stats-band {
  max-width: var(--portal-max);
  min-height: 108px;
  display: grid;
  grid-template-columns: 196px 1fr;
  align-items: center;
  gap: 24px;
  margin: 24px auto 0;
  padding: 18px 28px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: color-mix(in srgb, var(--ip-neutral-0) 88%, transparent);
  backdrop-filter: blur(10px);
}

.stats-heading {
  min-height: 70px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid color-mix(in srgb, var(--ip-neutral-200) 72%, transparent);
}

.stats-heading strong {
  color: var(--ip-primary-700);
  font-size: 20px;
  line-height: 1.1;
  font-weight: 700;
}

.stats-heading span {
  margin-top: 10px;
  color: var(--ip-neutral-500);
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
  border-right: 1px solid color-mix(in srgb, var(--ip-neutral-200) 72%, transparent);
}

.stat-item:last-child {
  border-right: 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
  box-shadow: var(--ip-shadow-sm);
}

.stat-icon .el-icon {
  font-size: 24px;
}

.stat-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-copy em {
  color: var(--ip-neutral-600);
  font-size: 14px;
  line-height: 1;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.stat-copy strong {
  color: var(--ip-neutral-900);
  font-size: 24px;
  line-height: 1;
  font-weight: 700;
  white-space: nowrap;
}

.stat-copy small {
  margin-left: 6px;
  color: var(--ip-neutral-600);
  font-size: 14px;
  font-weight: 650;
}

@media (max-width: 1460px) {
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
    border-bottom: 1px solid var(--ip-neutral-200);
  }

  .stat-item {
    min-height: 76px;
    border-bottom: 1px solid var(--ip-neutral-200);
  }

  .stat-item:nth-child(3n) {
    border-right: 0;
  }

  .stat-item:last-child {
    border-bottom: 0;
  }
}

@media (max-width: 767px) {
  .stats-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-item:nth-child(3n) {
    border-right: 1px solid var(--ip-neutral-200);
  }

  .stat-item:nth-child(2n) {
    border-right: 0;
  }
}

@media (max-width: 640px) {
  .stats-band {
    padding: 20px;
    border-radius: 16px;
  }
}

@media (max-width: 520px) {
  .stats-list {
    grid-template-columns: 1fr;
  }

  .stat-item,
  .stat-item:nth-child(2n),
  .stat-item:nth-child(3n) {
    border-right: 0;
  }
}
</style>
