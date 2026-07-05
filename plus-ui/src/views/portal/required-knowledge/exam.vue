<template>
  <main class="rk-page">
    <StudySidebar :active-code="activeSideCode" :subject-count="subjects.length" :question-count="totalQuestions" :groups="subjectGroups" />

    <section v-if="subject" class="rk-main">
      <header class="rk-topbar">
        <div class="title-block">
          <h1>{{ exam?.name || '模拟考试' }}</h1>
          <p>{{ subject.name }} · 完成当前题目后可交卷查看解析。</p>
        </div>

        <div class="top-actions">
          <span class="rk-time-tag">{{ exam?.duration || 90 }} 分钟</span>
          <PortalNotificationBell />
        </div>
      </header>

      <section class="rk-exam-workspace">
        <aside class="rk-paper-panel">
          <router-link class="rk-back-link" :to="`/portal/required-knowledge/${subject.code}`">
            <IconBack />
            <span>返回{{ subject.name }}</span>
          </router-link>

          <section class="rk-paper-card">
            <span><IconExam /></span>
            <strong>{{ exam?.name || '模拟考试' }}</strong>
            <em>{{ exam?.duration || 90 }} 分钟 · {{ exam?.questionCount || 1 }} 题 · {{ exam?.totalScore || 100 }} 分</em>
          </section>

          <section class="rk-answer-sheet" aria-label="答题卡">
            <h2>答题卡</h2>
            <button type="button" :class="{ done: Boolean(answer) }">1</button>
          </section>
        </aside>

        <section class="rk-question-panel">
          <article class="rk-question-card">
            <div class="rk-question-meta">
              <span>第 1 题 / {{ exam?.questionCount || 1 }}</span>
              <span>单选题</span>
            </div>

            <h2>{{ currentQuestion.title }}</h2>

            <el-radio-group v-model="answer" class="rk-options">
              <el-radio v-for="option in currentQuestion.options" :key="option.value" class="rk-option" :value="option.value">
                <span>{{ option.value }}</span>
                <strong>{{ option.label }}</strong>
              </el-radio>
            </el-radio-group>

            <div class="rk-exam-actions">
              <el-button @click="answer = ''">清空</el-button>
              <el-button type="primary" @click="submitted = true">交卷</el-button>
            </div>
          </article>

          <section v-if="submitted" class="rk-result" :class="{ wrong: answer !== currentQuestion.answer }">
            <strong>{{ answer === currentQuestion.answer ? '本题正确' : '本题需要订正' }}</strong>
            <p>正确答案：{{ currentQuestion.answer }}。{{ currentQuestion.analysis }}</p>
          </section>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import IconBack from '~icons/material-symbols/arrow-back-rounded';
import IconExam from '~icons/material-symbols/assignment-outline-rounded';
import { RkSubjectGroup } from '@/api/required-knowledge/types';
import { listRequiredKnowledgeCatalog } from '@/api/portal/requiredKnowledge';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import StudySidebar from './components/StudySidebar.vue';
import { StudySubject, fallbackSubjectGroups, fallbackSubjects, findFallbackSubject } from './data';

const route = useRoute();
const catalogGroups = ref<RkSubjectGroup[]>([]);
const subjectGroups = computed(() =>
  catalogGroups.value.length
    ? catalogGroups.value.map((group) => ({ code: group.groupCode, name: group.groupName, description: group.description || '' }))
    : fallbackSubjectGroups
);
const subjects = computed<StudySubject[]>(() => {
  if (!catalogGroups.value.length) {
    return fallbackSubjects;
  }
  return catalogGroups.value.flatMap((group) =>
    (group.subjects || []).map((item) => {
      const fallback = findFallbackSubject(item.subjectCode);
      return {
        code: item.subjectCode,
        name: item.subjectName,
        groupCode: group.groupCode,
        groupName: group.groupName,
        description: item.description || '',
        knowledgeCount: item.knowledgeCount || 0,
        questionCount: item.questionCount || 0,
        examCount: item.examCount || 0,
        status: 'enabled',
        chapters: fallback?.chapters || [],
        exams: fallback?.exams || []
      };
    })
  );
});
const subject = computed(() => subjects.value.find((item) => item.code === String(route.params.subjectCode || '')));
const activeSideCode = computed(() => subject.value?.groupCode || 'overview');
const exam = computed(() => subject.value?.exams[0]);
const answer = ref('');
const submitted = ref(false);
const totalQuestions = computed(() => subjects.value.reduce((sum, item) => sum + item.questionCount, 0));
const loadCatalog = async () => {
  try {
    const res = await listRequiredKnowledgeCatalog();
    catalogGroups.value = (res as any).data ?? [];
  } catch {
    catalogGroups.value = [];
  }
};
const currentQuestion = {
  title: '客户在项目执行中提出新增报表需求，项目经理首先应如何处理？',
  answer: 'B',
  analysis: '新增需求应先进入变更控制流程，评估影响后再决定是否纳入范围。',
  options: [
    { value: 'A', label: '立即安排开发人员实现' },
    { value: 'B', label: '提交变更请求并评估影响' },
    { value: 'C', label: '拒绝客户需求' },
    { value: 'D', label: '等项目结束后再讨论' }
  ]
};

onMounted(() => {
  loadCatalog();
});
</script>

<style scoped>
.rk-page {
  min-height: 100vh;
  --rk-primary: var(--ip-primary-600);
  --rk-primary-soft: var(--ip-primary-50);
  --rk-accent: var(--ip-primary-700);
  --rk-accent-soft: var(--ip-primary-50);
  --rk-accent-border: var(--ip-primary-200);
  --rk-title: var(--ip-neutral-900);
  --rk-text: var(--ip-neutral-700);
  --rk-muted: var(--ip-neutral-500);
  --rk-border: var(--ip-neutral-200);
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 24px;
  padding: 24px 32px 48px;
  background: var(--ip-neutral-50);
  color: var(--rk-text);
  font-family:
    MiSans,
    HarmonyOS Sans SC,
    PingFang SC,
    Microsoft YaHei,
    sans-serif;
}

.rk-main {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 16px;
}

.rk-topbar,
.rk-paper-panel,
.rk-question-card,
.rk-result {
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-topbar {
  position: relative;
  min-height: 86px;
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  align-items: center;
  gap: 24px;
  overflow: hidden;
  padding: 16px 20px 16px 24px;
}

.rk-topbar::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--rk-accent);
}

.title-block,
.top-actions {
  position: relative;
  z-index: 1;
}

.title-block h1,
.rk-paper-card strong,
.rk-answer-sheet h2,
.rk-question-card h2,
.rk-result strong {
  margin: 0;
  color: var(--rk-title);
  font-weight: 700;
  letter-spacing: 0;
}

.title-block h1 {
  font-size: var(--ip-font-headline);
  line-height: 1.25;
  text-wrap: balance;
}

.title-block p,
.rk-paper-card em,
.rk-result p {
  color: var(--rk-muted);
  font-size: var(--ip-font-body);
  line-height: 1.6;
}

.title-block p {
  margin: 6px 0 0;
}

.top-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.rk-time-tag,
.rk-question-meta span {
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--ip-warning-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-warning-soft);
  color: var(--ip-warning);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
  font-weight: 700;
  white-space: nowrap;
}

.rk-exam-workspace {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
}

.rk-paper-panel,
.rk-question-card,
.rk-result {
  padding: 20px;
}

.rk-back-link {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--rk-primary);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 700;
  text-decoration: none;
}

.rk-paper-card {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-50);
}

.rk-paper-card > span {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ip-radius-md);
  background: var(--rk-accent-soft);
  color: var(--rk-accent);
  font-size: 24px;
}

.rk-paper-card strong,
.rk-answer-sheet h2 {
  font-size: var(--ip-font-title-sm);
  line-height: 1.4;
}

.rk-paper-card em {
  font-style: normal;
}

.rk-answer-sheet {
  margin-top: 20px;
}

.rk-answer-sheet h2 {
  margin-bottom: 12px;
}

.rk-answer-sheet button {
  width: 36px;
  height: 36px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--rk-text);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 700;
}

.rk-answer-sheet button.done {
  border-color: var(--rk-accent-border);
  background: var(--rk-accent-soft);
  color: var(--rk-accent);
}

.rk-question-panel {
  min-width: 0;
}

.rk-question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.rk-question-meta span {
  border-color: var(--rk-border);
  background: var(--ip-neutral-50);
  color: var(--rk-muted);
}

.rk-question-card h2 {
  margin-bottom: 24px;
  font-size: var(--ip-font-title);
  line-height: 1.35;
}

.rk-options {
  display: grid;
  gap: 12px;
}

.rk-option {
  min-height: 52px;
  width: 100%;
  display: flex;
  align-items: center;
  margin-right: 0;
  padding: 12px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
}

.rk-option span {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--rk-primary);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 700;
}

.rk-option strong {
  color: var(--rk-title);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 600;
}

.rk-exam-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.rk-result {
  margin-top: 16px;
}

.rk-result strong {
  font-size: var(--ip-font-emphasis);
  line-height: 1.6;
}

.rk-result p {
  margin: 8px 0 0;
  color: var(--rk-text);
}

.rk-result.wrong {
  border-color: var(--ip-warning-border);
  background: var(--ip-warning-soft);
}

@media (max-width: 1180px) {
  .rk-exam-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .rk-page {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .rk-topbar {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .top-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
