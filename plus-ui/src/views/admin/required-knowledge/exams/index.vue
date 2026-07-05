<template>
  <main class="rk-admin-page">
    <header class="rk-admin-head">
      <div>
        <p class="rk-admin-kicker">应知应会管理</p>
        <h1>考试配置</h1>
        <p>配置每个科目的考试名称、题量、时长和分值。</p>
      </div>
      <AdminNav />
    </header>

    <section class="rk-panel">
      <div class="rk-toolbar">
        <div>
          <strong>已配置 {{ exams.length }} 套考试</strong>
          <span>首期只维护基础模拟卷，题目来源于已发布题库。</span>
        </div>
        <el-button type="primary" @click="dialogVisible = true">新增考试</el-button>
      </div>
      <el-table :data="exams" border>
        <el-table-column label="考试名称" min-width="240">
          <template #default="{ row }">
            <div class="rk-exam-cell">
              <strong>{{ row.name }}</strong>
              <span>{{ row.questionCount }} 题 · {{ row.duration }} 分钟 · {{ row.score }} 分</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="160" />
        <el-table-column prop="questionCount" label="题量" width="100" />
        <el-table-column prop="duration" label="时长" width="110">
          <template #default="{ row }">{{ row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column prop="score" label="总分" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="rk-tag">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default>
            <el-button link type="primary">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="新增考试" width="520px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="考试名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="科目">
          <el-select v-model="form.subject" style="width: 100%">
            <el-option v-for="subject in subjectOptions" :key="subject.subjectId" :label="subject.subjectName" :value="subject.subjectName" />
          </el-select>
        </el-form-item>
        <el-form-item label="题量"><el-input-number v-model="form.questionCount" :min="1" /></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="form.duration" :min="15" /> <span class="rk-unit">分钟</span></el-form-item>
        <el-form-item label="总分"><el-input-number v-model="form.score" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveExam">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import AdminNav from '../components/AdminNav.vue';
import { listSubjectOptions } from '@/api/required-knowledge';
import { RkSubject } from '@/api/required-knowledge/types';

const dialogVisible = ref(false);
const subjectOptions = ref<RkSubject[]>([]);
const exams = ref([
  { name: '高项基础模拟卷 A', subject: '软考高项', questionCount: 45, duration: 90, score: 75, status: '启用' },
  { name: '408 基础模拟卷 A', subject: '考研 408', questionCount: 40, duration: 120, score: 150, status: '启用' }
]);
const form = reactive({ name: '', subject: '软考高项', questionCount: 30, duration: 90, score: 100 });

const saveExam = () => {
  exams.value.unshift({ ...form, status: '启用' });
  dialogVisible.value = false;
};

const loadSubjects = async () => {
  const res = await listSubjectOptions({ status: '0' });
  subjectOptions.value = (res as any).data ?? [];
  form.subject = subjectOptions.value[0]?.subjectName || '软考高项';
};

onMounted(() => {
  loadSubjects();
});
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

.rk-admin-head p,
.rk-toolbar,
.rk-unit {
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

.rk-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.rk-toolbar strong {
  display: block;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-emphasis);
  line-height: 1.6;
  font-weight: 700;
}

.rk-toolbar span {
  display: block;
  margin-top: 2px;
}

.rk-exam-cell {
  display: grid;
  gap: 4px;
}

.rk-exam-cell strong {
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 600;
}

.rk-exam-cell span {
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

.rk-unit {
  margin-left: 8px;
}

@media (max-width: 900px) {
  .rk-admin-head,
  .rk-toolbar {
    display: grid;
  }
}
</style>
