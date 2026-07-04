<template>
  <el-drawer v-model="visible" size="min(760px, 94vw)" :with-header="false" class="forum-detail-drawer">
    <div v-if="detail?.topic" class="topic-detail">
      <header class="detail-toolbar">
        <div>
          <span class="board-name">{{ detail.topic.boardName }}</span>
          <strong>话题详情</strong>
        </div>
        <button class="drawer-close" type="button" aria-label="关闭" @click="visible = false">
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
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ChatDotRound, Close, Pointer, View } from '@element-plus/icons-vue';
import { getForumTopic, likeForumTopic, replyForumTopic, unlikeForumTopic } from '@/api/infoservice/portal';
import { ForumReply, ForumTopicDetail } from '@/api/infoservice/types';

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

const emit = defineEmits<{ (e: 'refresh-list'): void }>();

const visible = ref(false);
const detail = ref<ForumTopicDetail>();
const replyContent = ref('');
const inlineReplyContent = ref('');
const replying = ref(false);
const inlineReplying = ref(false);
const activeReplyTarget = ref<ReplyTarget>();

const parseReplyMeta = (value?: string): ReplyMeta => {
  if (!value) return {};
  try {
    const meta = JSON.parse(value) as ReplyMeta;
    return meta && typeof meta === 'object' ? meta : {};
  } catch {
    return {};
  }
};

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

const refreshDetail = async (id: number) => {
  const res: any = await getForumTopic(id);
  detail.value = res.data;
  emit('refresh-list');
};

const clearInlineReply = () => {
  activeReplyTarget.value = undefined;
  inlineReplyContent.value = '';
};

const open = async (id: number) => {
  await refreshDetail(id);
  replyContent.value = '';
  clearInlineReply();
  visible.value = true;
};

const submitReply = async () => {
  if (!detail.value?.topic?.topicId || !replyContent.value.trim()) return;
  const topicId = detail.value.topic.topicId;
  replying.value = true;
  try {
    await replyForumTopic(topicId, { content: replyContent.value.trim() });
    ElMessage.success('讨论已发布');
    replyContent.value = '';
    await refreshDetail(topicId);
  } finally {
    replying.value = false;
  }
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
    await refreshDetail(topicId);
  } finally {
    inlineReplying.value = false;
  }
};

const toggleLike = async () => {
  if (!detail.value?.topic?.topicId) return;
  const topic = detail.value.topic;
  topic.liked ? await unlikeForumTopic(topic.topicId) : await likeForumTopic(topic.topicId);
  await refreshDetail(topic.topicId);
};

defineExpose({ open });
</script>

<style scoped>
:global(.forum-detail-drawer .el-drawer__body) {
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

.board-name {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 900;
  background: var(--forum-accent-soft);
  color: var(--forum-accent);
}

.pin {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 900;
  background: var(--forum-accent);
  color: #fff;
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
.dialog-cancel {
  height: 38px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 850;
  cursor: pointer;
}

.reply-submit {
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

.reply-submit:hover {
  background: var(--forum-primary-deep);
}

.reply-submit:disabled {
  opacity: 0.66;
  cursor: default;
}

.dialog-cancel {
  border: 1px solid var(--forum-input-border);
  background: #fff;
  color: var(--forum-text);
}

@media (max-width: 760px) {
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
