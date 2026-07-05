<template>
  <main class="rk-admin-page">
    <header class="rk-admin-head">
      <div>
        <p class="rk-admin-kicker">应知应会管理</p>
        <h1>题目录入</h1>
        <p>维护首期题库。题目保存后可被考试配置引用。</p>
      </div>
      <AdminNav />
    </header>

    <section class="rk-summary" aria-label="题库概览">
      <div>
        <strong>{{ questionList.length }}</strong>
        <span>题目总数</span>
      </div>
      <div>
        <strong>{{ publishedCount }}</strong>
        <span>已发布</span>
      </div>
      <div>
        <strong>{{ draftCount }}</strong>
        <span>草稿</span>
      </div>
    </section>

    <section class="rk-panel">
      <div class="rk-toolbar">
        <el-form class="rk-filter" :model="query" inline>
          <el-form-item label="科目">
            <el-select v-model="query.subject" clearable placeholder="全部科目" style="width: 168px">
              <el-option v-for="subject in subjects" :key="subject.code" :label="subject.name" :value="subject.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="query.type" clearable placeholder="全部题型" style="width: 140px">
              <el-option label="单选" value="单选" />
              <el-option label="多选" value="多选" />
              <el-option label="判断" value="判断" />
              <el-option label="主观" value="主观" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="drawerVisible = true">新增题目</el-button>
      </div>

      <el-table v-if="filteredQuestions.length" :data="filteredQuestions" border>
        <el-table-column label="题目摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="rk-question-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.subject }} · {{ row.type }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="150" />
        <el-table-column prop="type" label="题型" width="100" />
        <el-table-column prop="answer" label="答案" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="rk-tag" :class="{ draft: row.status === '草稿' }">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default>
            <el-button link type="primary">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无匹配题目" />
    </section>

    <el-drawer v-model="drawerVisible" title="新增题目" size="520px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="科目">
          <el-select v-model="form.subject" placeholder="请选择科目" style="width: 100%">
            <el-option v-for="subject in subjects" :key="subject.code" :label="subject.name" :value="subject.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-radio-group v-model="form.type">
            <el-radio-button label="单选" />
            <el-radio-button label="多选" />
            <el-radio-button label="判断" />
            <el-radio-button label="主观" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.title" type="textarea" :rows="4" placeholder="请输入题干" />
        </el-form-item>
        <el-form-item label="选项">
          <el-input v-model="form.options" type="textarea" :rows="4" placeholder="每行一个选项，主观题可留空" />
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="form.answer" placeholder="如 A 或 正确" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="请输入解析" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">保存</el-button>
      </template>
    </el-drawer>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import AdminNav from '../components/AdminNav.vue';
import { subjects } from '../data';

interface QuestionRow {
  title: string;
  subject: string;
  type: string;
  answer: string;
  status: string;
  updatedAt: string;
}

const drawerVisible = ref(false);
const query = reactive({ subject: '', type: '' });
const questionList = ref<QuestionRow[]>([
  { title: '范围基准通常由哪些内容组成？', subject: '软考高项', type: '多选', answer: 'A/B/C', status: '已发布', updatedAt: '2026-07-05 09:30' },
  { title: '互斥信号量 mutex 的初值通常是多少？', subject: '考研 408', type: '单选', answer: '1', status: '已发布', updatedAt: '2026-07-05 09:20' },
  { title: '当 x -> 0 时，sin x 与 x 是否等价？', subject: '考研数学', type: '判断', answer: '正确', status: '草稿', updatedAt: '2026-07-05 09:10' }
]);

const form = reactive({
  subject: '',
  type: '单选',
  title: '',
  options: '',
  answer: '',
  analysis: ''
});

const filteredQuestions = computed(() =>
  questionList.value.filter((item) => (!query.subject || item.subject === query.subject) && (!query.type || item.type === query.type))
);
const publishedCount = computed(() => questionList.value.filter((item) => item.status === '已发布').length);
const draftCount = computed(() => questionList.value.filter((item) => item.status === '草稿').length);

const saveQuestion = () => {
  questionList.value.unshift({
    title: form.title || '未命名题目',
    subject: form.subject || '软考高项',
    type: form.type,
    answer: form.answer || '待补充',
    status: '草稿',
    updatedAt: '2026-07-05 10:00'
  });
  drawerVisible.value = false;
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

.rk-admin-head h1 {
  margin: 0;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-headline);
  line-height: 1.25;
  font-weight: 700;
}

.rk-admin-head p {
  margin: 8px 0 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-body);
  line-height: 1.6;
}

.rk-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.rk-summary div {
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-summary strong {
  display: block;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-title);
  line-height: 1.35;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rk-summary span {
  display: block;
  margin-top: 4px;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-panel {
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.rk-filter :deep(.el-form-item) {
  margin-bottom: 0;
}

.rk-question-cell {
  display: grid;
  gap: 4px;
}

.rk-question-cell strong {
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 600;
}

.rk-question-cell span {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-tag {
  height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--ip-success-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-success-soft);
  color: var(--ip-success);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-tag.draft {
  border-color: var(--ip-warning-border);
  background: var(--ip-warning-soft);
  color: var(--ip-warning);
}

@media (max-width: 900px) {
  .rk-admin-head,
  .rk-toolbar {
    display: grid;
  }

  .rk-summary {
    grid-template-columns: 1fr;
  }
}
</style>
