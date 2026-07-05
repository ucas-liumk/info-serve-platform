<template>
  <div class="rk-module-page">
    <section class="rk-module-header">
      <div>
        <p>应知应会管理</p>
        <h1>{{ title }}</h1>
        <span>{{ description }}</span>
      </div>
      <el-tag type="info" effect="light">{{ status }}</el-tag>
    </section>

    <section class="rk-module-panel">
      <div class="panel-title">
        <strong>后续接入项</strong>
        <span>当前已完成后台菜单和路由占位，业务表单/接口由应知应会模块继续实现。</span>
      </div>
      <el-timeline>
        <el-timeline-item v-for="item in items" :key="item" type="primary">
          {{ item }}
        </el-timeline-item>
      </el-timeline>
    </section>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  status: string;
  items: string[];
}>();
</script>

<style scoped lang="scss">
.rk-module-page {
  padding: 16px;
}

.rk-module-header,
.rk-module-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.rk-module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 136px;
  padding: 24px;

  p,
  h1 {
    margin: 0;
  }

  p {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  h1 {
    margin-top: 8px;
    color: var(--el-text-color-primary);
    font-size: 28px;
    font-weight: 700;
    line-height: 1.25;
  }

  span {
    display: block;
    margin-top: 10px;
    color: var(--el-text-color-regular);
    font-size: 14px;
  }
}

.rk-module-panel {
  margin-top: 16px;
  padding: 22px 24px;
}

.panel-title {
  margin-bottom: 18px;

  strong {
    display: block;
    color: var(--el-text-color-primary);
    font-size: 16px;
  }

  span {
    display: block;
    margin-top: 6px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.5;
  }
}

@media (max-width: 900px) {
  .rk-module-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
