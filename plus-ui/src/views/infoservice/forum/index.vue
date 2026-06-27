<template>
  <div class="forum">
    <PortalHeader
      v-model="keyword"
      title="服务论坛"
      subtitle="先开放最基础的发言互动，集中承接咨询、经验和需求反馈。"
      search-placeholder="搜索话题、正文或发言人"
      @search="reloadFirst"
    />

    <section class="forum-shell">
      <aside class="boards">
        <button :class="['board', { active: boardCode === 'all' }]" type="button" @click="changeBoard('all')">
          <strong>全部话题</strong>
          <span>{{ total }} 条</span>
        </button>
        <button v-for="board in boards" :key="board.boardId" :class="['board', { active: boardCode === board.boardCode }]" type="button" @click="changeBoard(board.boardCode)">
          <strong>{{ board.boardName }}</strong>
          <span>{{ board.topicCount || 0 }} 条</span>
          <em>{{ board.description }}</em>
        </button>
        <button class="new-topic" type="button" @click="openTopicDialog">
          <el-icon><EditPen /></el-icon>
          发起话题
        </button>
      </aside>

      <main class="topics">
        <div v-loading="loading" class="topic-list">
          <article v-for="topic in topics" :key="topic.topicId" :class="['topic-card', { top: topic.isTop === '1' }]" @click="openTopic(topic.topicId)">
            <div class="topic-main">
              <div class="topic-labels">
                <span v-if="topic.isTop === '1'" class="pin">置顶</span>
                <span class="board-name">{{ topic.boardName || '版块' }}</span>
              </div>
              <h3>{{ topic.title }}</h3>
              <p>{{ stripText(topic.content) }}</p>
              <div class="topic-meta">
                <span>{{ topic.authorName || '用户' }}</span>
                <span>{{ topic.createTime || '-' }}</span>
              </div>
            </div>
            <div class="topic-stats">
              <span>{{ topic.viewCount || 0 }} 看</span>
              <span>{{ topic.replyCount || 0 }} 复</span>
              <span>{{ topic.likeCount || 0 }} 赞</span>
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
      </main>
    </section>

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
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import { EditPen, Pointer } from '@element-plus/icons-vue';
import PortalHeader from '@/layout/portal/components/PortalHeader.vue';
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
.forum {
  min-height: 100vh;
}

.forum-shell {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 18px;
  padding: 0 40px 34px;
}

.boards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.board,
.new-topic {
  border-radius: 8px;
  cursor: pointer;
}

.board {
  min-height: 82px;
  border: 1px solid #dbe2ea;
  padding: 14px;
  background: rgba(255, 255, 255, 0.9);
  text-align: left;
}

.board strong,
.board span,
.board em {
  display: block;
}

.board strong {
  color: #172033;
  font-size: 16px;
  font-weight: 950;
}

.board span {
  margin-top: 5px;
  color: #8a5d1d;
  font-size: 13px;
  font-weight: 850;
}

.board em {
  margin-top: 8px;
  color: #667386;
  font-size: 12px;
  font-style: normal;
  font-weight: 650;
  line-height: 1.45;
}

.board.active {
  border-color: #c98b2e;
  background: #fff4de;
}

.new-topic {
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  background: #17304e;
  color: #fff;
  font-weight: 900;
}

.topic-list {
  min-height: 560px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-card {
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 18px;
  padding: 20px;
  border: 1px solid #dce3eb;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 28px rgba(32, 46, 69, 0.05);
  cursor: pointer;
}

.topic-card:hover {
  border-color: #c98b2e;
}

.topic-card.top {
  border-left: 4px solid #c98b2e;
}

.topic-labels {
  display: flex;
  gap: 8px;
}

.pin,
.board-name {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 900;
}

.pin {
  background: #17304e;
  color: #fff;
}

.board-name {
  background: #fff4de;
  color: #8a5d1d;
}

.topic-card h3 {
  margin: 12px 0 8px;
  color: #172033;
  font-size: 20px;
  line-height: 1.25;
  font-weight: 950;
}

.topic-card p {
  margin: 0;
  color: #5d6878;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 650;
}

.topic-meta {
  margin-top: 14px;
  display: flex;
  gap: 14px;
  color: #7b8798;
  font-size: 12px;
  font-weight: 800;
}

.topic-stats {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: #526076;
  font-weight: 850;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 22px;
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
  border: 1px solid #d9e0e8;
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: #223047;
  font-weight: 900;
  cursor: pointer;
}

.like-btn.liked {
  border-color: #c98b2e;
  background: #fff4de;
  color: #8a5d1d;
}

.topic-detail h2 {
  margin: 18px 0 8px;
  color: #172033;
  font-size: 25px;
  line-height: 1.3;
  font-weight: 950;
}

.author-line {
  color: #7b8798;
  font-size: 13px;
  font-weight: 800;
}

.content {
  margin: 22px 0;
  white-space: pre-wrap;
  color: #2a364a;
  font-size: 15px;
  line-height: 1.85;
  font-weight: 650;
}

.replies {
  border-top: 1px solid #e2e8ef;
  padding-top: 18px;
}

.replies h3 {
  margin: 0 0 14px;
  color: #172033;
  font-size: 18px;
  font-weight: 950;
}

.reply {
  padding: 14px 0;
  border-bottom: 1px solid #eef2f6;
}

.reply strong {
  color: #172033;
  font-weight: 950;
}

.reply span {
  margin-left: 10px;
  color: #8793a3;
  font-size: 12px;
  font-weight: 800;
}

.reply p {
  margin: 8px 0 0;
  color: #354258;
  line-height: 1.7;
  white-space: pre-wrap;
}

.reply-submit,
.dialog-submit,
.dialog-cancel {
  height: 38px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 900;
  cursor: pointer;
}

.reply-submit,
.dialog-submit {
  margin-top: 12px;
  border: 0;
  background: #17304e;
  color: #fff;
}

.reply-submit:disabled,
.dialog-submit:disabled {
  opacity: 0.66;
  cursor: default;
}

.dialog-cancel {
  border: 1px solid #d9e0e8;
  background: #fff;
  color: #223047;
}

.full {
  width: 100%;
}

@media (max-width: 1120px) {
  .forum-shell {
    grid-template-columns: 1fr;
  }

  .boards {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .forum-shell {
    padding-inline: 18px;
  }

  .boards {
    grid-template-columns: 1fr;
  }

  .topic-card {
    grid-template-columns: 1fr;
  }

  .topic-stats {
    align-items: flex-start;
    flex-direction: row;
  }
}
</style>
