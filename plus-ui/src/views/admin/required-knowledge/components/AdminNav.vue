<template>
  <nav class="rk-admin-nav" aria-label="应知应会管理导航">
    <router-link to="/admin/required-knowledge/questions">
      <IconQuestion />
      <span>题目录入</span>
    </router-link>
    <router-link to="/admin/required-knowledge/exams">
      <IconExam />
      <span>考试配置</span>
    </router-link>
    <router-link to="/admin/required-knowledge/ocr">
      <IconUpload />
      <span>OCR 导入</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import IconExam from '~icons/material-symbols/assignment-outline-rounded';
import IconQuestion from '~icons/material-symbols/help-outline-rounded';
import IconUpload from '~icons/material-symbols/upload-file-outline-rounded';
</script>

<style scoped>
.rk-admin-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rk-admin-nav a {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  text-decoration: none;
}

.rk-admin-nav a.router-link-active {
  border-color: var(--ip-primary-200);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
  font-weight: 600;
}

.rk-admin-nav svg {
  width: 16px;
  height: 16px;
}
</style>
