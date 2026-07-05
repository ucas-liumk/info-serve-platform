<template>
  <div class="required-knowledge-admin">
    <section class="rk-admin-hero">
      <div>
        <p>后台管理</p>
        <h1>应知应会</h1>
        <span>题库、考试和材料识别管理入口</span>
      </div>
      <el-tag type="info" effect="light">待接入</el-tag>
    </section>

    <section class="rk-admin-grid">
      <article v-for="item in moduleCards" :key="item.title" class="rk-admin-card">
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <div>
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts" name="RequiredKnowledgeAdmin">
import { Document, Files, Finished } from '@element-plus/icons-vue';

const moduleCards = [
  { title: '题库管理', desc: '题目录入、编辑与发布', icon: Document },
  { title: '考试配置', desc: '试卷、题量、时长与分值', icon: Finished },
  { title: 'OCR 导入', desc: 'PDF 和截图材料识别', icon: Files }
];
</script>

<style scoped lang="scss">
.required-knowledge-admin {
  padding: 16px;
}

.rk-admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 144px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);

  p {
    margin: 0 0 8px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  h1 {
    margin: 0;
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

.rk-admin-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.rk-admin-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  min-height: 112px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);

  .el-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
    font-size: 20px;
  }

  strong {
    display: block;
    color: var(--el-text-color-primary);
    font-size: 16px;
    line-height: 1.4;
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
  .rk-admin-grid {
    grid-template-columns: 1fr;
  }

  .rk-admin-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
