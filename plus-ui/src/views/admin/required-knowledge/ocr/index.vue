<template>
  <main class="rk-admin-page">
    <header class="rk-admin-head">
      <div>
        <p class="rk-admin-kicker">应知应会管理</p>
        <h1>OCR 导入</h1>
        <p>使用 PaddleOCR 识别 PDF 或截图，生成题目草稿，复核后再入库。</p>
      </div>
      <AdminNav />
    </header>

    <section class="rk-panel">
      <div class="rk-upload">
        <div>
          <h2>上传材料</h2>
          <p>支持 PDF、PNG、JPG。首期只生成可编辑草稿，不自动发布题目。</p>
          <div class="rk-flow">
            <span>上传材料</span>
            <span>PaddleOCR</span>
            <span>草稿复核</span>
          </div>
        </div>
        <el-upload action="#" :auto-upload="false" :show-file-list="false" @change="handleFileChange">
          <el-button type="primary">选择文件</el-button>
        </el-upload>
      </div>

      <el-table :data="drafts" border>
        <el-table-column label="材料名称" min-width="260">
          <template #default="{ row }">
            <div class="rk-file-cell">
              <strong>{{ row.fileName }}</strong>
              <span>{{ row.engine }} · {{ row.status }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="140" />
        <el-table-column prop="draftCount" label="草稿数" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <span class="rk-warning-tag">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="engine" label="识别引擎" width="140" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default>
            <el-button link type="primary">复核草稿</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import AdminNav from '../components/AdminNav.vue';

const drafts = ref([
  { fileName: '408-操作系统-PV操作截图.png', subject: '考研 408', draftCount: 3, status: '待复核', engine: 'PaddleOCR' },
  { fileName: '高项-范围管理样题.pdf', subject: '软考高项', draftCount: 5, status: '待复核', engine: 'PaddleOCR' }
]);

const handleFileChange = (file: any) => {
  drafts.value.unshift({
    fileName: file.name || '新上传材料.pdf',
    subject: '待选择',
    draftCount: 0,
    status: '待处理',
    engine: 'PaddleOCR'
  });
};
</script>

<style scoped>
.rk-admin-page {
  padding: 24px;
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-900);
}

.rk-admin-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.rk-admin-kicker {
  margin: 0 0 4px;
  color: var(--ip-primary-600);
  font-size: var(--ip-font-hint);
  line-height: 1.5;
  font-weight: 700;
}

.rk-admin-head h1,
.rk-upload h2 {
  margin: 0;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-headline);
  line-height: 1.25;
  font-weight: 700;
}

.rk-upload h2 {
  font-size: var(--ip-font-title-sm);
  line-height: 1.4;
}

.rk-admin-head p,
.rk-upload p {
  margin: 8px 0 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-body);
  line-height: 1.6;
}

.rk-panel {
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-upload {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--ip-neutral-200);
}

.rk-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.rk-flow span {
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--ip-primary-200);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
  font-weight: 600;
}

.rk-file-cell {
  display: grid;
  gap: 4px;
}

.rk-file-cell strong {
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 600;
}

.rk-file-cell span {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-warning-tag {
  height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--ip-warning-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-warning-soft);
  color: var(--ip-warning);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

@media (max-width: 900px) {
  .rk-admin-head,
  .rk-upload {
    display: grid;
  }
}
</style>
