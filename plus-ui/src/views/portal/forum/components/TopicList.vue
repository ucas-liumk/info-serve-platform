<template>
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
</template>

<script setup lang="ts">
import { ChatDotRound, Pointer, User, View } from '@element-plus/icons-vue';
import { ForumTopic } from '@/api/infoservice/types';

defineProps<{ topics: ForumTopic[]; loading: boolean; total: number; pageSize: number; pageNum: number }>();
const emit = defineEmits<{ (e: 'open', id: number): void; (e: 'page-change', page: number): void }>();

const stripText = (value?: string) => (value || '').replace(/\s+/g, ' ').slice(0, 120);

const openTopic = (id: number) => emit('open', id);
const onPage = (page: number) => emit('page-change', page);
</script>

<style scoped>
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

@media (max-width: 760px) {
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
}

@media (max-width: 520px) {
  .topic-list {
    padding: 12px;
  }
}
</style>
