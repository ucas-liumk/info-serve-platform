<template>
  <div class="forum-app">
    <aside class="forum-sidebar">
      <div class="side-brand">
        <img :src="moduleForum" alt="服务论坛" />
        <div>
          <strong>服务论坛</strong>
          <span>互助交流 · 共建共治</span>
        </div>
      </div>

      <button class="home-button" type="button" @click="goPortalHome">
        <el-icon><House /></el-icon>
        <span>返回首页</span>
      </button>

      <div class="board-panel">
        <div class="panel-title">
          <span>论坛版块</span>
          <em>{{ boards.length }} 个</em>
        </div>
        <button :class="['board-filter', { active: boardCode === 'all' }]" type="button" @click="changeBoard('all')">
          <span class="board-copy">
            <strong>全部话题</strong>
            <em>查看所有咨询、经验与反馈</em>
          </span>
          <span class="board-count">{{ total }}</span>
        </button>
        <button
          v-for="board in boards"
          :key="board.boardId"
          :class="['board-filter', { active: boardCode === board.boardCode }]"
          type="button"
          @click="changeBoard(board.boardCode)"
        >
          <span class="board-copy">
            <strong>{{ board.boardName }}</strong>
            <em>{{ board.description || '版块讨论与信息沉淀' }}</em>
          </span>
          <span class="board-count">{{ board.topicCount || 0 }}</span>
        </button>
      </div>

      <div class="side-summary">
        <span>当前话题总数</span>
        <strong>{{ total }}</strong>
        <p>围绕服务问题、工具使用、资料共建进行集中交流。</p>
      </div>
    </aside>

    <main class="forum-main">
      <header class="forum-topbar">
        <div class="title-block">
          <h1>服务论坛</h1>
          <p>沉淀咨询答复、经验分享与服务需求反馈</p>
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

          <button class="new-topic" type="button" @click="openTopicDialog">
            <el-icon><EditPen /></el-icon>
            <span>发起话题</span>
          </button>
        </div>
      </header>

      <section class="forum-content">
        <header class="content-head">
          <div>
            <strong>{{ activeBoardName }}</strong>
            <span>{{ activeBoardDescription }}</span>
          </div>
          <div class="result-count">
            <b>{{ total }}</b>
            <span>条话题</span>
          </div>
        </header>

        <div v-loading="loading" class="topic-list">
          <article
            v-for="topic in topics"
            :key="topic.topicId"
            :class="['topic-card', { top: topic.isTop === '1' }]"
            @click="openTopic(topic.topicId)"
          >
            <div class="topic-main">
              <div class="topic-labels">
                <span v-if="topic.isTop === '1'" class="pin">置顶</span>
                <span class="board-name">{{ topic.boardName || '版块' }}</span>
                <span v-if="topic.isClosed === '1'" class="closed">已关闭</span>
              </div>
              <h3>{{ topic.title }}</h3>
              <p>{{ stripText(topic.content) }}</p>
              <div class="topic-meta">
                <span><el-icon><User /></el-icon>{{ topic.authorName || '用户' }}</span>
                <span>{{ topic.createTime || '-' }}</span>
              </div>
            </div>
            <div class="topic-stats">
              <span><el-icon><View /></el-icon>{{ topic.viewCount || 0 }}</span>
              <span><el-icon><ChatDotRound /></el-icon>{{ topic.replyCount || 0 }}</span>
              <span><el-icon><Pointer /></el-icon>{{ topic.likeCount || 0 }}</span>
            </div>
          </article>
        </div>
        <el-empty v-if="!loading && topics.length === 0" description="暂无话题" />
        <el-pagination
          v-if="total > pageSize"
          class="pager"
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="onPage"
        />
      </section>
    </main>

    <el-drawer v-model="detailVisible" size="620px" title="话题详情">
      <div v-if="detail?.topic" class="topic-detail">
        <div class="detail-head">
          <span class="board-name">{{ detail.topic.boardName }}</span>
          <button :class="['like-btn', { liked: detail.topic.liked }]" type="button" @click="toggleLike">
            <el-icon><Pointer /></el-icon>
            {{ detail.topic.liked ? '已赞' : '点赞' }} {{ detail.topic.likeCount || 0 }}
          </button>
        </div>
        <h2>{{ detail.topic.title }}</h2>
        <div class="author-line">{{ detail.topic.authorName || '用户' }} · {{ detail.topic.createTime || '-' }}</div>
        <p class="content">{{ detail.topic.content }}</p>
        <section class="replies">
          <h3>回复 {{ detail.replies?.length || 0 }}</h3>
          <article v-for="reply in detail.replies" :key="reply.replyId" class="reply">
            <strong>{{ reply.authorName || '用户' }}</strong>
            <span>{{ reply.createTime || '-' }}</span>
            <p>{{ reply.content }}</p>
          </article>
          <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="1000" placeholder="写下你的回复" />
          <button class="reply-submit" type="button" :disabled="replying || !replyContent.trim()" @click="submitReply">
            {{ replying ? '提交中' : '回复' }}
          </button>
        </section>
      </div>
    </el-drawer>

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
import { useRouter } from 'vue-router';
import type { FormInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import { ChatDotRound, EditPen, House, Pointer, Search, User, View } from '@element-plus/icons-vue';
import { PORTAL_HOME_PATH } from '@/constants/router';
import {
  createForumTopic,
  getForumTopic,
  likeForumTopic,
  listForumBoards,
  listForumTopics,
  replyForumTopic,
  unlikeForumTopic
} from '@/api/infoservice/portal';
import { ForumBoard, ForumTopic, ForumTopicDetail } from '@/api/infoservice/types';
import moduleForum from '@/assets/portal/module-forum.png';

const router = useRouter();
const boards = ref<ForumBoard[]>([]);
const topics = ref<ForumTopic[]>([]);
const detail = ref<ForumTopicDetail>();
const keyword = ref('');
const boardCode = ref('all');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const topicDialogVisible = ref(false);
const posting = ref(false);
const replying = ref(false);
const replyContent = ref('');
const topicFormRef = ref<FormInstance>();
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
const activeBoardDescription = computed(() =>
  boardCode.value === 'all' ? '按最新动态查看论坛全部交流内容' : activeBoard.value?.description || '查看该版块下的问题、经验和反馈'
);

const goPortalHome = () => {
  router.push(PORTAL_HOME_PATH);
};

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

const openTopic = async (id: number) => {
  const res: any = await getForumTopic(id);
  detail.value = res.data;
  replyContent.value = '';
  detailVisible.value = true;
  reload();
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

const submitReply = async () => {
  if (!detail.value?.topic?.topicId || !replyContent.value.trim()) return;
  replying.value = true;
  try {
    await replyForumTopic(detail.value.topic.topicId, { content: replyContent.value });
    ElMessage.success('回复已发布');
    await openTopic(detail.value.topic.topicId);
  } finally {
    replying.value = false;
  }
};

const toggleLike = async () => {
  if (!detail.value?.topic?.topicId) return;
  const topic = detail.value.topic;
  topic.liked ? await unlikeForumTopic(topic.topicId) : await likeForumTopic(topic.topicId);
  await openTopic(topic.topicId);
};

const stripText = (value?: string) => (value || '').replace(/\s+/g, ' ').slice(0, 120);

onMounted(async () => {
  await loadBoards();
  await reload();
});
</script>

<style scoped>
.forum-app {
  min-height: 100vh;
  --forum-primary: #1260e8;
  --forum-primary-deep: #0f55cf;
  --forum-primary-soft: #edf4ff;
  --forum-title: #0b1833;
  --forum-text: #25395f;
  --forum-muted: #53668f;
  --forum-weak: #8a97af;
  --forum-border: #e1e9f6;
  --forum-input-border: #dbe5f4;
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 28px 44px;
  background: linear-gradient(180deg, rgba(237, 244, 255, 0.9) 0%, rgba(247, 250, 255, 0.72) 320px), #f7faff;
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

.forum-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  display: grid;
  grid-template-rows: auto auto auto auto;
  gap: 14px;
}

.side-brand {
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 13px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.side-brand img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.side-brand div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.side-brand strong {
  color: var(--forum-title);
  font-size: 22px;
  line-height: 1.15;
  font-weight: 900;
}

.side-brand span {
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.home-button,
.search-button,
.new-topic {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  font-weight: 850;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.home-button {
  width: 100%;
  border: 1px solid var(--forum-border);
  background: #fff;
  color: var(--forum-text);
}

.home-button:hover {
  border-color: var(--forum-primary);
  background: var(--forum-primary-soft);
  color: var(--forum-primary);
}

.board-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 2px 2px 6px;
}

.panel-title span {
  color: var(--forum-title);
  font-size: 15px;
  font-weight: 900;
}

.panel-title em {
  color: var(--forum-muted);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.board-filter {
  min-height: 74px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 13px 12px;
  background: #f8fbff;
  color: var(--forum-text);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.board-filter:hover {
  border-color: #b8cff4;
  background: #f5f9ff;
}

.board-filter.active {
  border-color: #b8cff4;
  background: var(--forum-primary-soft);
  box-shadow: inset 3px 0 0 var(--forum-primary);
}

.board-copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.board-copy strong {
  overflow: hidden;
  color: var(--forum-title);
  font-size: 15px;
  line-height: 1.2;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.board-copy em {
  display: -webkit-box;
  overflow: hidden;
  color: var(--forum-muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 650;
  line-height: 1.35;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.board-count {
  min-width: 38px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #fff;
  color: var(--forum-primary);
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 0 0 1px #dbe5f4 inset;
}

.side-summary {
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 16px;
  background: linear-gradient(180deg, #fff 0%, #f5f9ff 100%);
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.side-summary span {
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 750;
}

.side-summary strong {
  display: block;
  margin-top: 6px;
  color: var(--forum-title);
  font-size: 32px;
  line-height: 1;
  font-weight: 900;
}

.side-summary p {
  margin: 10px 0 0;
  color: var(--forum-muted);
  font-size: 13px;
  line-height: 1.5;
  font-weight: 650;
}

.forum-main {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 16px;
}

.forum-topbar {
  min-height: 74px;
  display: grid;
  grid-template-columns: minmax(220px, 390px) minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 14px 16px 14px 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.title-block {
  min-width: 0;
}

.title-block h1 {
  margin: 0;
  color: var(--forum-title);
  font-size: 27px;
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
  box-shadow: 0 8px 20px rgba(18, 96, 232, 0.18);
}

.new-topic {
  flex: 0 0 auto;
  border: 1px solid var(--forum-primary);
  padding: 0 15px;
  background: var(--forum-primary);
  color: #fff;
}

.forum-content {
  min-height: calc(100vh - 150px);
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 14px 34px rgba(35, 83, 151, 0.07);
}

.content-head {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--forum-border);
  padding: 14px 18px;
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
}

.content-head div:first-child {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.content-head strong {
  color: var(--forum-title);
  font-size: 18px;
  line-height: 1.2;
  font-weight: 900;
}

.content-head span {
  overflow: hidden;
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-count {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  border-radius: 8px;
  padding: 8px 12px;
  background: var(--forum-primary-soft);
  color: var(--forum-primary);
}

.result-count b {
  font-size: 22px;
  line-height: 1;
  font-weight: 900;
}

.result-count span {
  color: var(--forum-primary);
  font-size: 13px;
  font-weight: 800;
}

.topic-list {
  min-height: 520px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
}

.topic-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 18px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(35, 83, 151, 0.05);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.topic-card:hover {
  border-color: #b8cff4;
  box-shadow: 0 16px 34px rgba(18, 96, 232, 0.1);
  transform: translateY(-1px);
}

.topic-card.top {
  border-left: 4px solid var(--forum-primary);
}

.topic-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pin,
.board-name,
.closed {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 900;
}

.pin {
  background: var(--forum-primary);
  color: #fff;
}

.board-name {
  background: var(--forum-primary-soft);
  color: var(--forum-primary);
}

.closed {
  background: #f2f5f9;
  color: var(--forum-muted);
}

.topic-card h3 {
  margin: 12px 0 8px;
  color: var(--forum-title);
  font-size: 20px;
  line-height: 1.25;
  font-weight: 900;
}

.topic-card p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--forum-muted);
  font-size: 14px;
  line-height: 1.6;
  font-weight: 650;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.topic-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--forum-weak);
  font-size: 12px;
  font-weight: 750;
}

.topic-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.topic-meta .el-icon {
  font-size: 14px;
}

.topic-stats {
  display: flex;
  align-items: stretch;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
}

.topic-stats span {
  min-width: 94px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
  background: #f7faff;
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 850;
}

.topic-stats .el-icon {
  color: var(--forum-primary);
  font-size: 16px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 0 0 22px;
}

.forum-app :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--forum-primary);
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.like-btn {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--forum-input-border);
  border-radius: 8px;
  padding: 0 10px;
  background: #fff;
  color: var(--forum-text);
  font-weight: 850;
  cursor: pointer;
}

.like-btn.liked {
  border-color: var(--forum-primary);
  background: var(--forum-primary-soft);
  color: var(--forum-primary);
}

.topic-detail h2 {
  margin: 18px 0 8px;
  color: var(--forum-title);
  font-size: 25px;
  line-height: 1.3;
  font-weight: 900;
}

.author-line {
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 750;
}

.content {
  margin: 22px 0;
  white-space: pre-wrap;
  color: var(--forum-text);
  font-size: 15px;
  line-height: 1.85;
  font-weight: 650;
}

.replies {
  border-top: 1px solid var(--forum-border);
  padding-top: 18px;
}

.replies h3 {
  margin: 0 0 14px;
  color: var(--forum-title);
  font-size: 18px;
  font-weight: 900;
}

.reply {
  padding: 14px 0;
  border-bottom: 1px solid #eef3fa;
}

.reply strong {
  color: var(--forum-title);
  font-weight: 900;
}

.reply span {
  margin-left: 10px;
  color: var(--forum-weak);
  font-size: 12px;
  font-weight: 750;
}

.reply p {
  margin: 8px 0 0;
  color: var(--forum-text);
  line-height: 1.7;
  white-space: pre-wrap;
}

.reply-submit,
.dialog-submit,
.dialog-cancel {
  height: 38px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 850;
  cursor: pointer;
}

.reply-submit,
.dialog-submit {
  margin-top: 12px;
  border: 0;
  background: var(--forum-primary);
  color: #fff;
}

.reply-submit:hover,
.dialog-submit:hover {
  background: var(--forum-primary-deep);
}

.reply-submit:disabled,
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

  .forum-sidebar {
    position: static;
    max-height: none;
  }

  .board-panel {
    max-height: none;
  }

  .side-summary {
    display: none;
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

  .content-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .topic-card {
    grid-template-columns: 1fr;
  }

  .topic-stats {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .topic-stats span {
    min-width: 82px;
  }
}

@media (max-width: 520px) {
  .forum-app {
    padding-inline: 12px;
  }

  .topic-list {
    padding: 12px;
  }
}
</style>
