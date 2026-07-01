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
          <span class="board-icon">
            <el-icon><ChatDotRound /></el-icon>
          </span>
          <span class="board-copy">
            <strong>全部话题</strong>
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
          <span class="board-icon">
            <el-icon><component :is="getBoardIcon(board)" /></el-icon>
          </span>
          <span class="board-copy">
            <strong>{{ board.boardName }}</strong>
          </span>
          <span class="board-count">{{ board.topicCount || 0 }}</span>
        </button>
      </div>
    </aside>

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

      <section class="forum-content">
        <header class="content-head">
          <div>
            <strong>话题列表</strong>
          </div>
          <div class="result-count">
            <b>{{ total }}</b>
            <span>条话题</span>
          </div>
        </header>

        <div v-loading="loading" class="topic-list">
          <article
            v-for="(topic, index) in topics"
            :key="topic.topicId"
            :class="['topic-card', { top: topic.isTop === '1' }]"
            @click="openTopic(topic.topicId)"
          >
            <span class="topic-index">#{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="topic-main">
              <div class="topic-labels">
                <span v-if="topic.isTop === '1'" class="pin">置顶</span>
                <span class="board-name">{{ topic.boardName || '版块' }}</span>
                <span v-if="topic.isClosed === '1'" class="closed">已关闭</span>
              </div>
              <h3>{{ topic.title }}</h3>
              <p>{{ stripText(topic.content) }}</p>
              <div class="topic-meta">
                <span
                  ><el-icon><User /></el-icon>{{ topic.authorName || '用户' }}</span
                >
                <span>{{ topic.createTime || '-' }}</span>
              </div>
            </div>
            <div class="topic-stats">
              <span>
                <el-icon><View /></el-icon>
                <strong>{{ topic.viewCount || 0 }}</strong>
                <small>浏览</small>
              </span>
              <span>
                <el-icon><ChatDotRound /></el-icon>
                <strong>{{ topic.replyCount || 0 }}</strong>
                <small>回复</small>
              </span>
              <span>
                <el-icon><Pointer /></el-icon>
                <strong>{{ topic.likeCount || 0 }}</strong>
                <small>点赞</small>
              </span>
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

    <el-drawer v-model="detailVisible" size="min(760px, 94vw)" :with-header="false" class="forum-detail-drawer">
      <div v-if="detail?.topic" class="topic-detail">
        <header class="detail-toolbar">
          <div>
            <span class="board-name">{{ detail.topic.boardName }}</span>
            <strong>话题详情</strong>
          </div>
          <button class="drawer-close" type="button" aria-label="关闭" @click="detailVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <article class="detail-topic-card">
          <div class="detail-head">
            <div class="detail-author">
              <span class="reply-avatar">{{ getAvatarText(detail.topic.authorName) }}</span>
              <div>
                <strong>{{ detail.topic.authorName || '用户' }}</strong>
                <em>{{ detail.topic.createTime || '-' }}</em>
              </div>
            </div>
            <span v-if="detail.topic.isTop === '1'" class="pin">置顶</span>
          </div>
          <h2>{{ detail.topic.title }}</h2>
          <p class="detail-content">{{ detail.topic.content }}</p>
          <div class="detail-actions">
            <span
              ><el-icon><View /></el-icon>{{ detail.topic.viewCount || 0 }} 浏览</span
            >
            <span
              ><el-icon><ChatDotRound /></el-icon>{{ detail.replies?.length || 0 }} 讨论</span
            >
            <button :class="['like-btn', { liked: detail.topic.liked }]" type="button" @click="toggleLike">
              <el-icon><Pointer /></el-icon>
              {{ detail.topic.liked ? '已赞' : '点赞' }} {{ detail.topic.likeCount || 0 }}
            </button>
          </div>
        </article>

        <section class="discussion-panel">
          <div class="main-composer">
            <span class="reply-avatar mine">我</span>
            <div class="composer-body main-composer-body">
              <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="1000" placeholder="写下你的看法" />
              <button class="reply-submit floating-submit" type="button" :disabled="replying || !replyContent.trim()" @click="submitReply">
                {{ replying ? '提交中' : '发布讨论' }}
              </button>
            </div>
          </div>

          <el-empty v-if="replyThreads.length === 0" description="暂无讨论" />

          <div v-else class="thread-list">
            <article v-for="thread in replyThreads" :key="thread.replyId" class="reply-thread">
              <div class="reply-item">
                <span class="reply-avatar">{{ getAvatarText(thread.authorName) }}</span>
                <div class="reply-body">
                  <header class="reply-head">
                    <div>
                      <strong>{{ thread.authorName || '用户' }}</strong>
                      <em>{{ thread.createTime || '-' }}</em>
                    </div>
                    <span class="floor-tag">{{ thread.floor }}楼</span>
                  </header>
                  <p>{{ thread.content }}</p>
                  <button class="thread-reply" type="button" @click="beginInlineReply(thread)">
                    <el-icon><ChatDotRound /></el-icon>
                    回复
                  </button>
                </div>
              </div>

              <div v-if="thread.children.length > 0" class="nested-replies">
                <article v-for="child in thread.children" :key="child.replyId" class="nested-reply">
                  <span class="reply-avatar small">{{ getAvatarText(child.authorName) }}</span>
                  <div class="reply-body">
                    <header class="reply-head compact">
                      <div>
                        <strong>{{ child.authorName || '用户' }}</strong>
                        <em>
                          <template v-if="getReplyMeta(child).replyToUserName">回复 @{{ getReplyMeta(child).replyToUserName }} · </template>
                          {{ child.createTime || '-' }}
                        </em>
                      </div>
                    </header>
                    <p>{{ child.content }}</p>
                    <button class="thread-reply" type="button" @click="beginInlineReply(child)">
                      <el-icon><ChatDotRound /></el-icon>
                      回复
                    </button>
                  </div>
                </article>
              </div>

              <div v-if="activeReplyTarget?.rootReplyId === thread.replyId" class="inline-composer">
                <span class="reply-avatar mine">我</span>
                <div class="composer-body">
                  <el-input
                    v-model="inlineReplyContent"
                    type="textarea"
                    :rows="3"
                    maxlength="1000"
                    :placeholder="`回复 @${activeReplyTarget.authorName}`"
                  />
                  <div class="composer-actions">
                    <button class="dialog-cancel" type="button" @click="clearInlineReply">取消</button>
                    <button class="reply-submit" type="button" :disabled="inlineReplying || !inlineReplyContent.trim()" @click="submitInlineReply">
                      {{ inlineReplying ? '提交中' : '回复' }}
                    </button>
                  </div>
                </div>
              </div>
            </article>
          </div>
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
import {
  ChatDotRound,
  ChatLineRound,
  Close,
  Collection,
  DocumentChecked,
  EditPen,
  Flag,
  House,
  Pointer,
  Search,
  User,
  View
} from '@element-plus/icons-vue';
import { PORTAL_HOME_PATH } from '@/constants/router';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import {
  createForumTopic,
  getForumTopic,
  likeForumTopic,
  listForumBoards,
  listForumTopics,
  replyForumTopic,
  unlikeForumTopic
} from '@/api/infoservice/portal';
import { ForumBoard, ForumReply, ForumTopic, ForumTopicDetail } from '@/api/infoservice/types';
import moduleForum from '@/assets/portal/module-forum.png';

interface ReplyMeta {
  parentReplyId?: number;
  replyToReplyId?: number;
  replyToUserId?: number;
  replyToUserName?: string;
}

interface ReplyTarget {
  rootReplyId: number;
  replyId: number;
  authorName: string;
  authorId?: number;
}

type ReplyThread = ForumReply & {
  floor: number;
  children: ForumReply[];
};

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
const inlineReplying = ref(false);
const replyContent = ref('');
const inlineReplyContent = ref('');
const activeReplyTarget = ref<ReplyTarget>();
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
const replyMetaMap = computed(() => {
  const map = new Map<number, ReplyMeta>();
  (detail.value?.replies || []).forEach((reply) => {
    map.set(reply.replyId, parseReplyMeta(reply.remark));
  });
  return map;
});
const replyThreads = computed<ReplyThread[]>(() => {
  const replies = detail.value?.replies || [];
  const byId = new Map(replies.map((reply) => [reply.replyId, reply]));
  const childrenByParent = new Map<number, ForumReply[]>();
  const roots: ForumReply[] = [];

  replies.forEach((reply) => {
    const parentReplyId = replyMetaMap.value.get(reply.replyId)?.parentReplyId;
    if (parentReplyId && byId.has(parentReplyId)) {
      const children = childrenByParent.get(parentReplyId) || [];
      children.push(reply);
      childrenByParent.set(parentReplyId, children);
      return;
    }
    roots.push(reply);
  });

  return roots.map((reply, index) => ({
    ...reply,
    floor: index + 1,
    children: childrenByParent.get(reply.replyId) || []
  }));
});

const goPortalHome = () => {
  router.push(PORTAL_HOME_PATH);
};

const parseReplyMeta = (value?: string): ReplyMeta => {
  if (!value) return {};
  try {
    const meta = JSON.parse(value) as ReplyMeta;
    return meta && typeof meta === 'object' ? meta : {};
  } catch {
    return {};
  }
};

const getReplyMeta = (reply: ForumReply) => replyMetaMap.value.get(reply.replyId) || {};

const getAvatarText = (name?: string) => {
  const text = (name || '用户').trim();
  return text.slice(0, 1).toUpperCase();
};

const findReplyById = (replyId?: number) => (detail.value?.replies || []).find((reply) => reply.replyId === replyId);

const getThreadRootReplyId = (reply: ForumReply) => {
  const parentReplyId = getReplyMeta(reply).parentReplyId;
  return parentReplyId && findReplyById(parentReplyId) ? parentReplyId : reply.replyId;
};

const getBoardIcon = (board: ForumBoard) => {
  const text = `${board.boardCode || ''} ${board.boardName || ''}`;
  if (/consult|qa|咨询|答疑/.test(text)) return ChatLineRound;
  if (/share|experience|经验|交流/.test(text)) return Collection;
  if (/feedback|demand|需求|反馈/.test(text)) return Flag;
  return DocumentChecked;
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

const refreshTopicDetail = async (id: number) => {
  const res: any = await getForumTopic(id);
  detail.value = res.data;
  await reload();
};

const openTopic = async (id: number) => {
  await refreshTopicDetail(id);
  replyContent.value = '';
  clearInlineReply();
  detailVisible.value = true;
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
  const topicId = detail.value.topic.topicId;
  replying.value = true;
  try {
    await replyForumTopic(topicId, { content: replyContent.value.trim() });
    ElMessage.success('讨论已发布');
    replyContent.value = '';
    await refreshTopicDetail(topicId);
  } finally {
    replying.value = false;
  }
};

const clearInlineReply = () => {
  activeReplyTarget.value = undefined;
  inlineReplyContent.value = '';
};

const beginInlineReply = (reply: ForumReply) => {
  activeReplyTarget.value = {
    rootReplyId: getThreadRootReplyId(reply),
    replyId: reply.replyId,
    authorId: reply.authorId,
    authorName: reply.authorName || '用户'
  };
  inlineReplyContent.value = '';
};

const submitInlineReply = async () => {
  if (!detail.value?.topic?.topicId || !activeReplyTarget.value || !inlineReplyContent.value.trim()) return;
  const topicId = detail.value.topic.topicId;
  const target = activeReplyTarget.value;
  inlineReplying.value = true;
  try {
    await replyForumTopic(topicId, {
      content: inlineReplyContent.value.trim(),
      remark: JSON.stringify({
        parentReplyId: target.rootReplyId,
        replyToReplyId: target.replyId,
        replyToUserId: target.authorId,
        replyToUserName: target.authorName
      })
    });
    ElMessage.success('回复已发布');
    clearInlineReply();
    await refreshTopicDetail(topicId);
  } finally {
    inlineReplying.value = false;
  }
};

const toggleLike = async () => {
  if (!detail.value?.topic?.topicId) return;
  const topic = detail.value.topic;
  topic.liked ? await unlikeForumTopic(topic.topicId) : await likeForumTopic(topic.topicId);
  await refreshTopicDetail(topic.topicId);
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

.forum-sidebar {
  position: sticky;
  top: 18px;
  align-self: start;
  display: grid;
  grid-template-rows: auto auto auto;
  gap: 14px;
}

.side-brand {
  position: relative;
  height: 86px;
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 13px;
  box-sizing: border-box;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(196, 102, 84, 0.12), transparent 42%), linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.side-brand::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -34px;
  width: 110px;
  height: 72px;
  border: 1px solid rgba(196, 102, 84, 0.22);
  border-radius: 8px;
  transform: rotate(-14deg);
  pointer-events: none;
}

.side-brand img {
  position: relative;
  z-index: 1;
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.side-brand div {
  position: relative;
  z-index: 1;
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
  transform: translateY(-1px);
}

.board-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 14px;
  background: var(--forum-surface);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
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
  min-height: 58px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.98)), #f8fafc;
  color: var(--forum-text);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.board-filter:hover {
  border-color: #b8c9d9;
  background: #f8fafc;
  transform: translateX(2px);
}

.board-filter.active {
  border-color: #b8c9d9;
  background: var(--forum-accent-soft);
  box-shadow: inset 3px 0 0 var(--forum-accent);
}

.board-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fff;
  color: var(--forum-accent);
  box-shadow: 0 0 0 1px var(--forum-input-border) inset;
}

.board-filter.active .board-icon {
  background: var(--forum-accent);
  color: #fff;
  box-shadow: 0 10px 20px rgba(196, 102, 84, 0.2);
}

.board-copy {
  min-width: 0;
  display: grid;
}

.board-copy strong {
  color: var(--forum-title);
  font-size: 15px;
  line-height: 1.2;
  font-weight: 850;
}

.board-count {
  min-width: 38px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #fff;
  color: var(--forum-accent);
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 0 0 1px var(--forum-input-border) inset;
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

.forum-content {
  min-height: calc(100vh - 150px);
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.08);
}

.content-head {
  min-height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--forum-border);
  padding: 14px 18px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
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

.result-count {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  border-radius: 8px;
  padding: 8px 12px;
  background: var(--forum-accent-soft);
  color: var(--forum-accent);
}

.result-count b {
  font-size: 22px;
  line-height: 1;
  font-weight: 900;
}

.result-count span {
  color: var(--forum-accent);
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
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 18px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  padding: 18px;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(196, 102, 84, 0.035), transparent 28%), #fff;
  box-shadow: 0 10px 26px rgba(31, 54, 76, 0.06);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.topic-card:hover {
  border-color: #b8c9d9;
  box-shadow: 0 16px 34px rgba(31, 54, 76, 0.11);
  transform: translateY(-1px);
}

.topic-card.top {
  box-shadow:
    inset 4px 0 0 var(--forum-accent),
    0 10px 26px rgba(31, 54, 76, 0.06);
}

.topic-index {
  position: absolute;
  left: 18px;
  top: 20px;
  width: 38px;
  color: #9aa8be;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.topic-main {
  min-width: 0;
  padding-left: 52px;
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
  background: var(--forum-accent);
  color: #fff;
}

.board-name {
  background: var(--forum-accent-soft);
  color: var(--forum-accent);
}

.closed {
  background: var(--forum-amber-soft);
  color: #8a5d1d;
}

.topic-card h3 {
  margin: 12px 0 8px;
  color: var(--forum-title);
  font-size: 20px;
  line-height: 1.25;
  font-weight: 900;
  letter-spacing: 0;
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
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
  background: #f5f7fa;
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 850;
}

.topic-stats strong {
  color: var(--forum-title);
  font-size: 14px;
  line-height: 1;
  font-weight: 900;
}

.topic-stats small {
  color: var(--forum-weak);
  font-size: 12px;
  font-weight: 750;
}

.topic-stats .el-icon {
  color: var(--forum-accent);
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

:deep(.forum-detail-drawer .el-drawer__body) {
  padding: 0;
  background: #f5f8fc;
}

.topic-detail {
  min-height: 100%;
  color: var(--forum-text);
}

.detail-toolbar {
  position: sticky;
  top: 0;
  z-index: 3;
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--forum-border);
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
}

.detail-toolbar div {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-toolbar strong {
  color: var(--forum-title);
  font-size: 17px;
  font-weight: 900;
}

.drawer-close {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--forum-input-border);
  border-radius: 8px;
  background: #fff;
  color: var(--forum-muted);
  cursor: pointer;
}

.drawer-close:hover {
  border-color: var(--forum-primary);
  color: var(--forum-primary);
}

.detail-topic-card,
.discussion-panel {
  border-bottom: 1px solid var(--forum-border);
  background: #fff;
}

.detail-topic-card {
  padding: 20px 22px 18px;
}

.detail-head,
.detail-actions,
.reply-head,
.composer-actions {
  display: flex;
  align-items: center;
}

.detail-head,
.reply-head {
  justify-content: space-between;
  gap: 14px;
}

.detail-author,
.reply-item,
.nested-reply,
.main-composer,
.inline-composer {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.detail-author div,
.reply-head div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.detail-author strong,
.reply-head strong {
  color: var(--forum-title);
  font-size: 14px;
  font-weight: 900;
}

.detail-author em,
.reply-head em {
  color: var(--forum-weak);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.reply-avatar {
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(196, 102, 84, 0.14), rgba(255, 240, 236, 0.9)), #fff;
  color: var(--forum-accent);
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 0 0 1px var(--forum-input-border) inset;
}

.reply-avatar.mine {
  background: var(--forum-primary);
  color: #fff;
}

.reply-avatar.small {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.topic-detail h2 {
  margin: 16px 0 10px;
  color: var(--forum-title);
  font-size: 25px;
  line-height: 1.32;
  font-weight: 900;
}

.detail-content,
.reply-body p {
  margin: 0;
  white-space: pre-wrap;
  color: var(--forum-text);
  font-size: 15px;
  line-height: 1.8;
  font-weight: 650;
}

.detail-actions {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 750;
}

.detail-actions span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.like-btn,
.thread-reply {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
  font-weight: 850;
  cursor: pointer;
}

.like-btn {
  height: 34px;
  border: 1px solid var(--forum-input-border);
  padding: 0 10px;
  background: #fff;
  color: var(--forum-text);
}

.like-btn.liked {
  border-color: var(--forum-primary);
  background: var(--forum-primary-soft);
  color: var(--forum-primary);
}

.discussion-panel {
  padding: 0 22px 24px;
}

.discussion-head {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--forum-border);
}

.discussion-head div {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.discussion-head strong {
  color: var(--forum-title);
  font-size: 18px;
  font-weight: 900;
}

.main-composer {
  padding: 18px 0;
}

.composer-body,
.reply-body {
  min-width: 0;
  flex: 1;
}

.main-composer-body {
  position: relative;
}

.main-composer-body :deep(.el-textarea__inner) {
  min-height: 112px;
  padding-right: 104px;
  padding-bottom: 44px;
}

.composer-actions {
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.thread-list {
  display: grid;
  gap: 0;
}

.reply-thread {
  border-top: 1px solid #edf2f9;
  padding: 18px 0;
}

.reply-thread:first-child {
  border-top: 0;
}

.reply-head {
  margin-bottom: 8px;
}

.reply-head.compact {
  margin-bottom: 6px;
}

.floor-tag {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 4px 8px;
  background: #f4f7fb;
  color: var(--forum-weak);
  font-size: 12px;
  font-weight: 900;
}

.thread-reply {
  height: 30px;
  margin-top: 10px;
  border: 0;
  padding: 0 2px;
  background: transparent;
  color: var(--forum-muted);
  font-size: 13px;
}

.thread-reply:hover {
  color: var(--forum-accent);
}

.nested-replies {
  display: grid;
  gap: 14px;
  margin: 14px 0 0 48px;
  border: 1px solid #e5edf8;
  border-radius: 8px;
  padding: 14px;
  background: #fbfcfe;
}

.nested-reply + .nested-reply {
  border-top: 1px solid #e7eef8;
  padding-top: 14px;
}

.inline-composer {
  margin: 14px 0 0 48px;
  border: 1px solid #d9e6f7;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
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
  border: 0;
  background: var(--forum-primary);
  color: #fff;
}

.floating-submit {
  position: absolute;
  right: 10px;
  bottom: 10px;
  height: 32px;
  padding: 0 12px;
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

  .topic-main {
    padding-left: 0;
    padding-top: 26px;
  }

  .topic-index {
    left: 18px;
    top: 16px;
  }

  .topic-stats {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .topic-stats span {
    min-width: 82px;
    flex: 1;
  }

  .detail-toolbar {
    padding-inline: 14px;
  }

  .detail-topic-card,
  .discussion-panel {
    padding-inline: 16px;
  }

  .discussion-head {
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    padding: 14px 0;
  }

  .detail-actions {
    gap: 8px;
  }

  .nested-replies,
  .inline-composer {
    margin-left: 0;
  }
}

@media (max-width: 520px) {
  .forum-app {
    padding-inline: 12px;
  }

  .topic-list {
    padding: 12px;
  }

  .detail-topic-card,
  .discussion-panel {
    padding-inline: 12px;
  }

  .reply-item,
  .nested-reply,
  .main-composer,
  .inline-composer {
    gap: 9px;
  }

  .reply-avatar {
    width: 32px;
    height: 32px;
  }
}
</style>
