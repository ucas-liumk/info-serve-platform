<template>
  <div class="forum-app">
    <ForumSidebar :boards="boards" :board-code="boardCode" :total="total" @change-board="changeBoard" />

    <main class="forum-main">
      <header class="forum-topbar">
        <div class="title-block">
          <h1>{{ activeBoardName }}</h1>
          <p>当前筛选共 {{ total }} 条话题</p>
        </div>

        <div class="top-actions">
          <div class="search-box">
            <el-input
              v-model="keyword"
              class="forum-search"
              clearable
              placeholder="搜索话题、正文或发言人"
              size="large"
              @keyup.enter="reloadFirst"
              @clear="reloadFirst"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <button class="search-button" type="button" @click="reloadFirst">搜索</button>
          </div>

          <PortalNotificationBell />
          <button class="new-topic" type="button" @click="openTopicDialog">
            <el-icon><EditPen /></el-icon>
            <span>发起话题</span>
          </button>
        </div>
      </header>

      <TopicList
        :topics="topics"
        :loading="loading"
        :total="total"
        :page-size="pageSize"
        :page-num="pageNum"
        @open="openTopic"
        @page-change="onPage"
      />
    </main>

    <TopicDetailDrawer ref="detailDrawerRef" @refresh-list="reload" />

    <el-dialog v-model="topicDialogVisible" width="600px" title="发起话题" destroy-on-close>
      <el-form ref="topicFormRef" :model="topicForm" :rules="topicRules" label-width="82px">
        <el-form-item label="版块" prop="boardId">
          <el-select v-model="topicForm.boardId" class="full" placeholder="请选择版块">
            <el-option v-for="board in boards" :key="board.boardId" :label="board.boardName" :value="board.boardId" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="topicForm.title" maxlength="160" placeholder="请输入话题标题" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="topicForm.content" type="textarea" :rows="6" maxlength="5000" placeholder="描述问题、经验或需求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="dialog-cancel" type="button" @click="topicDialogVisible = false">取消</button>
        <button class="dialog-submit" type="button" :disabled="posting" @click="submitTopic">{{ posting ? '发布中' : '发布话题' }}</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import { EditPen, Search } from '@element-plus/icons-vue';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import ForumSidebar from './components/ForumSidebar.vue';
import TopicList from './components/TopicList.vue';
import TopicDetailDrawer from './components/TopicDetailDrawer.vue';
import { createForumTopic, listForumBoards, listForumTopics } from '@/api/portal/forum';
import { ForumBoard, ForumTopic } from '@/api/infoservice/types';

const boards = ref<ForumBoard[]>([]);
const topics = ref<ForumTopic[]>([]);
const keyword = ref('');
const boardCode = ref('all');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const topicDialogVisible = ref(false);
const posting = ref(false);
const topicFormRef = ref<FormInstance>();
const detailDrawerRef = ref<InstanceType<typeof TopicDetailDrawer>>();
const topicForm = reactive({
  boardId: undefined as number | undefined,
  title: '',
  content: ''
});

const topicRules = {
  boardId: [{ required: true, message: '请选择版块', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
};

const activeBoard = computed(() => boards.value.find((board) => board.boardCode === boardCode.value));
const activeBoardName = computed(() => (boardCode.value === 'all' ? '全部话题' : activeBoard.value?.boardName || '当前版块'));

const loadBoards = async () => {
  const res: any = await listForumBoards();
  boards.value = res.data || [];
};

const reload = async () => {
  loading.value = true;
  try {
    const res: any = await listForumTopics({
      boardCode: boardCode.value,
      keyword: keyword.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    topics.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const reloadFirst = () => {
  pageNum.value = 1;
  reload();
};

const changeBoard = (code: string) => {
  boardCode.value = code;
  reloadFirst();
};

const onPage = (page: number) => {
  pageNum.value = page;
  reload();
};

const openTopic = (id: number) => {
  detailDrawerRef.value?.open(id);
};

const openTopicDialog = () => {
  topicForm.boardId = boards.value[0]?.boardId;
  topicForm.title = '';
  topicForm.content = '';
  topicDialogVisible.value = true;
};

const submitTopic = async () => {
  await topicFormRef.value?.validate();
  posting.value = true;
  try {
    const res: any = await createForumTopic(topicForm);
    ElMessage.success('话题已发布');
    topicDialogVisible.value = false;
    await loadBoards();
    reloadFirst();
    if (res.data?.topicId) {
      openTopic(res.data.topicId);
    }
  } finally {
    posting.value = false;
  }
};

onMounted(async () => {
  await loadBoards();
  await reload();
});
</script>

<style scoped>
.forum-app {
  min-height: 100vh;
  --forum-primary: #245f8f;
  --forum-primary-deep: #183f63;
  --forum-primary-soft: #eaf2f8;
  --forum-accent: #c46654;
  --forum-accent-soft: #fff0ec;
  --forum-amber: #b7791f;
  --forum-amber-soft: #fff3dc;
  --forum-title: #14243a;
  --forum-text: #32445c;
  --forum-muted: #68788c;
  --forum-weak: #96a1af;
  --forum-border: #dce5ed;
  --forum-input-border: #d3dee8;
  --forum-surface: rgba(255, 255, 255, 0.94);
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 28px 44px;
  background: linear-gradient(180deg, rgba(241, 244, 248, 0.95) 0%, rgba(247, 249, 252, 0.82) 320px), #f5f7fa;
  color: var(--forum-text);
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.forum-app :deep(.el-input__wrapper),
.forum-app :deep(.el-select__wrapper),
.forum-app :deep(.el-textarea__inner) {
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--forum-input-border) inset;
}

.forum-app :deep(.el-input__wrapper:hover),
.forum-app :deep(.el-select__wrapper:hover),
.forum-app :deep(.el-input__wrapper.is-focus),
.forum-app :deep(.el-select__wrapper.is-focused),
.forum-app :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--forum-primary) inset;
}

.forum-app :deep(.el-input__inner),
.forum-app :deep(.el-select__placeholder),
.forum-app :deep(.el-textarea__inner) {
  color: var(--forum-text);
  font-weight: 650;
}

.forum-app :deep(.el-input__inner::placeholder),
.forum-app :deep(.el-textarea__inner::placeholder) {
  color: var(--forum-muted);
}

.forum-main {
  min-width: 0;
  max-width: 1480px;
  display: grid;
  align-content: start;
  gap: 16px;
}

.forum-topbar {
  position: relative;
  min-height: 86px;
  display: grid;
  grid-template-columns: minmax(220px, 390px) minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  box-sizing: border-box;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 14px 16px 14px 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.forum-topbar::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--forum-accent);
  pointer-events: none;
}

.title-block {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.title-block h1 {
  margin: 0;
  color: var(--forum-title);
  font-size: 28px;
  line-height: 1.15;
  font-weight: 900;
}

.title-block p {
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-actions {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.search-box {
  min-width: 300px;
  max-width: 620px;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.forum-search {
  min-width: 0;
  --el-input-height: 40px;
}

:deep(.forum-search .el-input__wrapper) {
  padding: 0 14px;
  border-radius: 8px;
}

:deep(.forum-search .el-input__inner) {
  font-size: 14px;
}

.search-button,
.new-topic {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1;
  font-weight: 850;
  white-space: nowrap;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.search-button {
  border: 1px solid var(--forum-primary);
  padding: 0 16px;
  background: var(--forum-primary);
  color: #fff;
}

.search-button:hover,
.new-topic:hover {
  background: var(--forum-primary-deep);
  box-shadow: 0 8px 20px rgba(36, 95, 143, 0.18);
  transform: translateY(-1px);
}

.new-topic {
  flex: 0 0 auto;
  border: 1px solid var(--forum-primary);
  padding: 0 15px;
  background: var(--forum-primary);
  color: #fff;
}

.forum-app :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--forum-primary);
}

.dialog-submit,
.dialog-cancel {
  height: 38px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 850;
  cursor: pointer;
}

.dialog-submit {
  border: 0;
  background: var(--forum-primary);
  color: #fff;
}

.dialog-submit:hover {
  background: var(--forum-primary-deep);
}

.dialog-submit:disabled {
  opacity: 0.66;
  cursor: default;
}

.dialog-cancel {
  border: 1px solid var(--forum-input-border);
  background: #fff;
  color: var(--forum-text);
}

.full {
  width: 100%;
}

@media (max-width: 1360px) {
  .forum-app {
    grid-template-columns: 256px minmax(0, 1fr);
    padding-inline: 22px;
  }

  .forum-topbar {
    grid-template-columns: 1fr;
  }

  .top-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .forum-app {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}

@media (max-width: 760px) {
  .top-actions,
  .search-box {
    width: 100%;
  }

  .top-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    min-width: 0;
    grid-template-columns: 1fr;
  }

  .search-button,
  .new-topic {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .forum-app {
    padding-inline: 12px;
  }
}
</style>
