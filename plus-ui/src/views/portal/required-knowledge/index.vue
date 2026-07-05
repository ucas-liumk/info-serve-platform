<template>
  <main class="rk-page">
    <StudySidebar active-code="overview" :subject-count="subjects.length" :question-count="totalQuestions" :groups="subjectGroups" />

    <section class="rk-main">
      <header class="rk-topbar">
        <div class="title-block">
          <h1>应知应会</h1>
          <p>{{ subjects.length }} 个科目 · {{ totalQuestions }} 道题 · {{ totalExams }} 套考试</p>
        </div>

        <div class="top-actions">
          <PortalNotificationBell />
        </div>
      </header>

      <section class="rk-board">
        <header class="rk-board-head">
          <div>
            <h2>选择学习科目</h2>
            <p>按考试方向分组展示学习栏目，进入科目后再选择知识点学习或模拟考试。</p>
          </div>
          <span>{{ totalKnowledge }} 个知识点</span>
        </header>

        <section v-for="group in subjectGroups" :id="group.code" :key="group.code" class="rk-section">
          <header class="rk-section-head">
            <div>
              <h3>{{ group.name }}</h3>
              <p>{{ group.description }}</p>
            </div>
            <em>{{ groupedSubjects[group.code].length }} 个栏目</em>
          </header>

          <div class="rk-subject-grid">
            <router-link
              v-for="subject in groupedSubjects[group.code]"
              :key="subject.code"
              class="rk-subject-card"
              :to="`/portal/required-knowledge/${subject.code}`"
            >
              <span class="rk-subject-mark">{{ subject.name.slice(0, 2) }}</span>
              <span class="rk-subject-copy">
                <strong>{{ subject.name }}</strong>
                <em>{{ subject.description }}</em>
              </span>
              <span class="rk-subject-stats">
                <span>{{ subject.knowledgeCount }} 知识点</span>
                <span>{{ subject.questionCount }} 题</span>
                <span>{{ subject.examCount }} 考试</span>
              </span>
              <IconArrow class="rk-subject-arrow" />
            </router-link>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import IconArrow from '~icons/material-symbols/arrow-forward-rounded';
import { RkSubjectGroup } from '@/api/required-knowledge/types';
import { listRequiredKnowledgeCatalog } from '@/api/portal/requiredKnowledge';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import StudySidebar from './components/StudySidebar.vue';
import { StudySubject, fallbackSubjectGroups, fallbackSubjects, findFallbackSubject } from './data';

const catalogGroups = ref<RkSubjectGroup[]>([]);

const subjectGroups = computed(() =>
  catalogGroups.value.length
    ? catalogGroups.value.map((group) => ({
        code: group.groupCode,
        name: group.groupName,
        description: group.description || ''
      }))
    : fallbackSubjectGroups
);

const subjects = computed<StudySubject[]>(() => {
  if (!catalogGroups.value.length) {
    return fallbackSubjects;
  }
  return catalogGroups.value.flatMap((group) =>
    (group.subjects || []).map((subject) => {
      const fallback = findFallbackSubject(subject.subjectCode);
      return {
        code: subject.subjectCode,
        name: subject.subjectName,
        groupCode: group.groupCode,
        groupName: group.groupName,
        description: subject.description || '',
        knowledgeCount: subject.knowledgeCount || 0,
        questionCount: subject.questionCount || 0,
        examCount: subject.examCount || 0,
        status: 'enabled',
        chapters: fallback?.chapters || [],
        exams: fallback?.exams || []
      };
    })
  );
});

const groupedSubjects = computed(() =>
  subjectGroups.value.reduce(
    (acc, group) => {
      acc[group.code] = subjects.value.filter((subject) => subject.groupCode === group.code);
      return acc;
    },
    {} as Record<string, StudySubject[]>
  )
);

const totalKnowledge = computed(() => subjects.value.reduce((sum, subject) => sum + subject.knowledgeCount, 0));
const totalQuestions = computed(() => subjects.value.reduce((sum, subject) => sum + subject.questionCount, 0));
const totalExams = computed(() => subjects.value.reduce((sum, subject) => sum + subject.examCount, 0));

const loadCatalog = async () => {
  try {
    const res = await listRequiredKnowledgeCatalog();
    catalogGroups.value = (res as any).data ?? [];
  } catch {
    catalogGroups.value = [];
  }
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
.rk-board {
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-topbar {
  position: relative;
  min-height: 86px;
  display: grid;
  grid-template-columns: minmax(220px, 480px) minmax(0, 1fr);
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
.rk-board-head h2,
.rk-section-head h3,
.rk-subject-copy strong {
  margin: 0;
  color: var(--rk-title);
  font-weight: 700;
  letter-spacing: 0;
}

.title-block h1 {
  font-size: var(--ip-font-headline);
  line-height: 1.25;
}

.title-block p,
.rk-board-head p,
.rk-section-head p,
.rk-subject-copy em {
  margin: 6px 0 0;
  color: var(--rk-muted);
  font-size: var(--ip-font-body);
  line-height: 1.6;
}

.top-actions {
  display: flex;
  justify-content: flex-end;
}

.rk-board {
  padding: 20px;
}

.rk-board-head,
.rk-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.rk-board-head {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--rk-border);
}

.rk-board-head h2 {
  font-size: var(--ip-font-title);
  line-height: 1.35;
}

.rk-board-head > span,
.rk-section-head em,
.rk-subject-stats span {
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
  color: var(--rk-muted);
  font-size: var(--ip-font-caption);
  font-style: normal;
  line-height: 1.5;
  white-space: nowrap;
}

.rk-section {
  padding-top: 20px;
}

.rk-section + .rk-section {
  margin-top: 20px;
  border-top: 1px solid var(--rk-border);
}

.rk-section-head {
  margin-bottom: 14px;
}

.rk-section-head h3 {
  font-size: var(--ip-font-title-sm);
  line-height: 1.4;
}

.rk-subject-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.rk-subject-card {
  min-height: 148px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 32px;
  grid-template-rows: auto 1fr;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--rk-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  color: inherit;
  text-decoration: none;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    box-shadow var(--ip-motion-fast) var(--ip-motion-ease),
    transform var(--ip-motion-fast) var(--ip-motion-ease);
}

.rk-subject-card:hover {
  border-color: var(--rk-accent-border);
  box-shadow: var(--ip-shadow-md);
  transform: translateY(-2px);
}

.rk-subject-mark {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ip-radius-md);
  background: var(--rk-accent-soft);
  color: var(--rk-accent);
  font-size: var(--ip-font-emphasis);
  font-weight: 700;
  line-height: 1.4;
}

.rk-subject-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.rk-subject-copy strong {
  font-size: var(--ip-font-title-sm);
  line-height: 1.4;
}

.rk-subject-copy em {
  font-style: normal;
}

.rk-subject-stats {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-self: end;
}

.rk-subject-arrow {
  align-self: start;
  justify-self: end;
  color: var(--rk-primary);
  font-size: 24px;
}

@media (max-width: 980px) {
  .rk-page {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .rk-topbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .rk-board-head,
  .rk-section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .top-actions {
    justify-content: flex-start;
  }
}
</style>
