<template>
  <main class="rk-page">
    <StudySidebar :active-code="activeSideCode" :subject-count="subjects.length" :question-count="totalQuestions" :groups="subjectGroups" />

    <section v-if="subject" class="rk-main">
      <header class="rk-topbar">
        <div class="title-block">
          <h1>{{ mode === 'study' ? activeChapter?.title || '暂无知识点' : '选择考试' }}</h1>
          <p>
            {{ subject.name }} · {{ mode === 'study' ? activeChapter?.summary || '该科目暂未配置知识点内容。' : '选择已配置试卷，进入模拟考试。' }}
          </p>
        </div>

        <div class="top-actions">
          <div class="rk-mode-switch" aria-label="学习模式">
            <button type="button" :class="{ active: mode === 'study' }" @click="mode = 'study'">
              <IconBook />
              <span>学习</span>
            </button>
            <button type="button" :class="{ active: mode === 'exam' }" @click="mode = 'exam'">
              <IconExam />
              <span>考试</span>
            </button>
          </div>
          <PortalNotificationBell />
        </div>
      </header>

      <section class="rk-workspace">
        <aside class="rk-outline-panel">
          <router-link class="rk-back-link" to="/portal/required-knowledge">
            <IconBack />
            <span>返回科目列表</span>
          </router-link>

          <section class="rk-subject-card">
            <span>{{ subject.name.slice(0, 2) }}</span>
            <strong>{{ subject.name }}</strong>
            <em>{{ subject.description }}</em>
            <div>
              <span>{{ subject.knowledgeCount }} 知识点</span>
              <span>{{ subject.questionCount }} 题</span>
              <span>{{ subject.examCount }} 考试</span>
            </div>
          </section>

          <section class="rk-outline">
            <h2>知识点</h2>
            <button
              v-for="chapter in chapters"
              :key="chapter.id"
              class="rk-outline-item"
              :class="{ active: chapter.id === activeChapterId }"
              type="button"
              @click="
                mode = 'study';
                activeChapterId = chapter.id;
              "
            >
              <strong>{{ chapter.title }}</strong>
              <span>{{ chapter.summary }}</span>
            </button>
            <el-empty v-if="!chapters.length" description="暂无知识点" :image-size="80" />
          </section>
        </aside>

        <article v-if="mode === 'study'" class="rk-content-panel">
          <div v-if="activeChapter" class="rk-content">
            <p v-for="paragraph in activeChapter?.content" :key="paragraph">{{ paragraph }}</p>
          </div>
          <el-empty v-else description="暂无学习内容" />

          <section v-if="activeChapter && activeChapter.relatedQuestions.length" class="rk-related">
            <h2>关联题目</h2>
            <div class="rk-question-list">
              <div v-for="question in activeChapter?.relatedQuestions" :key="question.id" class="rk-question">
                <span>{{ questionTypeLabels[question.type] }}</span>
                <strong>{{ question.title }}</strong>
                <em>答案：{{ question.answer }}</em>
              </div>
            </div>
          </section>
        </article>

        <article v-else class="rk-content-panel rk-exam-list">
          <router-link v-for="exam in subject.exams" :key="exam.id" class="rk-exam-card" :to="`/portal/required-knowledge/${subject.code}/exam`">
            <span class="rk-exam-icon"><IconExam /></span>
            <span class="rk-exam-copy">
              <strong>{{ exam.name }}</strong>
              <em>{{ exam.duration }} 分钟 · {{ exam.questionCount }} 题 · {{ exam.totalScore }} 分</em>
            </span>
            <span class="rk-exam-action">开始考试</span>
          </router-link>
          <el-empty v-if="!subject.exams.length" description="暂无考试" />
        </article>
      </section>
    </section>

    <section v-else class="rk-main">
      <section class="rk-empty-state">
        <h1>科目不存在</h1>
        <p>请返回科目列表重新选择。</p>
        <router-link to="/portal/required-knowledge">返回科目列表</router-link>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import IconBack from '~icons/material-symbols/arrow-back-rounded';
import IconBook from '~icons/material-symbols/menu-book-outline-rounded';
import IconExam from '~icons/material-symbols/assignment-outline-rounded';
import { RkKnowledge, RkSubjectGroup } from '@/api/required-knowledge/types';
import { listRequiredKnowledgeBySubject, listRequiredKnowledgeCatalog } from '@/api/portal/requiredKnowledge';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import StudySidebar from './components/StudySidebar.vue';
import { StudySubject, fallbackSubjectGroups, fallbackSubjects, findFallbackSubject, questionTypeLabels } from './data';

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
const mode = ref<'study' | 'exam'>('study');
const activeChapterId = ref('');
const knowledgeLoaded = ref(false);
const knowledgeChapters = ref<StudySubject['chapters']>([]);
const chapters = computed(() => (knowledgeLoaded.value ? knowledgeChapters.value : subject.value?.chapters || []));
const activeChapter = computed(() => chapters.value.find((chapter) => chapter.id === activeChapterId.value) || chapters.value[0]);
const totalQuestions = computed(() => subjects.value.reduce((sum, item) => sum + item.questionCount, 0));

const splitKnowledgeContent = (content?: string) =>
  (content || '')
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean);

const mapKnowledgeToChapter = (item: RkKnowledge) => ({
  id: String(item.knowledgeId),
  title: item.title,
  summary: item.summary || '',
  content: splitKnowledgeContent(item.content),
  relatedQuestions: []
});

const loadCatalog = async () => {
  try {
    const res = await listRequiredKnowledgeCatalog();
    catalogGroups.value = (res as any).data ?? [];
  } catch {
    catalogGroups.value = [];
  }
};

let knowledgeRequestId = 0;
const loadKnowledge = async (subjectCode: string) => {
  const requestId = ++knowledgeRequestId;
  knowledgeLoaded.value = false;
  knowledgeChapters.value = [];
  try {
    const res = await listRequiredKnowledgeBySubject(subjectCode);
    if (requestId !== knowledgeRequestId) return;
    const rows = (res as any).data ?? [];
    knowledgeChapters.value = rows.map(mapKnowledgeToChapter);
    knowledgeLoaded.value = true;
  } catch {
    if (requestId === knowledgeRequestId) {
      knowledgeLoaded.value = false;
      knowledgeChapters.value = [];
    }
  }
};

watch(
  subject,
  async (value) => {
    if (value?.code) {
      await loadKnowledge(value.code);
    } else {
      knowledgeLoaded.value = false;
      knowledgeChapters.value = [];
    }
    activeChapterId.value = chapters.value[0]?.id || '';
    mode.value = 'study';
  },
  { immediate: true }
);

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
.rk-outline-panel,
.rk-content-panel,
.rk-empty-state {
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
.rk-outline h2,
.rk-related h2,
.rk-subject-card strong,
.rk-question strong,
.rk-exam-copy strong,
.rk-empty-state h1 {
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
.rk-subject-card em,
.rk-outline-item span,
.rk-content,
.rk-question em,
.rk-exam-copy em,
.rk-empty-state p {
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

.rk-mode-switch {
  height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
}

.rk-mode-switch button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  border-radius: var(--ip-radius-sm);
  padding: 0 12px;
  background: transparent;
  color: var(--rk-muted);
  font-size: var(--ip-font-body);
  line-height: 1;
  font-weight: 700;
  cursor: pointer;
}

.rk-mode-switch button.active {
  background: var(--ip-neutral-0);
  color: var(--rk-primary);
  box-shadow: var(--ip-shadow-sm);
}

.rk-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
}

.rk-outline-panel,
.rk-content-panel {
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

.rk-subject-card {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-50);
}

.rk-subject-card > span,
.rk-exam-icon {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ip-radius-md);
  background: var(--rk-accent-soft);
  color: var(--rk-accent);
  font-size: var(--ip-font-emphasis);
  line-height: 1.6;
  font-weight: 700;
}

.rk-subject-card strong {
  font-size: var(--ip-font-title-sm);
  line-height: 1.4;
}

.rk-subject-card em,
.rk-exam-copy em,
.rk-question em {
  font-style: normal;
}

.rk-subject-card div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rk-subject-card div span,
.rk-question span {
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--rk-muted);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-outline {
  margin-top: 20px;
}

.rk-outline h2,
.rk-related h2 {
  margin-bottom: 12px;
  font-size: var(--ip-font-title-sm);
  line-height: 1.4;
}

.rk-outline-item {
  width: 100%;
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: var(--ip-radius-sm);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.rk-outline-item + .rk-outline-item {
  margin-top: 8px;
}

.rk-outline-item:hover,
.rk-outline-item.active {
  border-color: var(--rk-accent-border);
  background: var(--rk-accent-soft);
}

.rk-outline-item strong,
.rk-exam-copy strong {
  color: var(--rk-title);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 700;
}

.rk-content {
  max-width: 780px;
  display: grid;
  gap: 16px;
  color: var(--rk-text);
  font-size: var(--ip-font-emphasis);
}

.rk-content p {
  margin: 0;
}

.rk-related {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--rk-border);
}

.rk-question-list,
.rk-exam-list {
  display: grid;
  gap: 12px;
}

.rk-question {
  min-height: 58px;
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 6px 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
}

.rk-question span {
  justify-content: center;
  border-color: var(--rk-accent-border);
  background: var(--rk-accent-soft);
  color: var(--rk-accent);
}

.rk-question em {
  grid-column: 2;
  font-size: var(--ip-font-hint);
}

.rk-exam-card {
  min-height: 76px;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  color: inherit;
  text-decoration: none;
}

.rk-exam-card:hover {
  border-color: var(--rk-accent-border);
  background: var(--rk-accent-soft);
}

.rk-exam-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.rk-exam-action,
.rk-empty-state a {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: var(--ip-radius-sm);
  background: var(--rk-primary);
  color: var(--ip-neutral-0);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 700;
  text-decoration: none;
}

.rk-empty-state {
  max-width: 640px;
  margin: 64px auto;
  padding: 32px;
  text-align: center;
}

.rk-empty-state h1 {
  font-size: var(--ip-font-headline);
  line-height: 1.25;
}

.rk-empty-state p {
  margin: 12px 0 24px;
}

@media (max-width: 1180px) {
  .rk-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .rk-page {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .rk-topbar,
  .rk-exam-card {
    grid-template-columns: 1fr;
  }

  .rk-topbar {
    align-items: stretch;
  }

  .top-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
