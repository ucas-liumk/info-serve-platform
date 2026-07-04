<template>
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
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ChatDotRound, ChatLineRound, Collection, DocumentChecked, Flag, House } from '@element-plus/icons-vue';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { ForumBoard } from '@/api/infoservice/types';
import moduleForum from '@/assets/portal/module-forum.png';

defineProps<{ boards: ForumBoard[]; boardCode: string; total: number }>();
const emit = defineEmits<{ (e: 'change-board', code: string): void }>();

const router = useRouter();

const goPortalHome = () => {
  router.push(PORTAL_HOME_PATH);
};

const getBoardIcon = (board: ForumBoard) => {
  const text = `${board.boardCode || ''} ${board.boardName || ''}`;
  if (/consult|qa|咨询|答疑/.test(text)) return ChatLineRound;
  if (/share|experience|经验|交流/.test(text)) return Collection;
  if (/feedback|demand|需求|反馈/.test(text)) return Flag;
  return DocumentChecked;
};

const changeBoard = (code: string) => emit('change-board', code);
</script>

<style scoped>
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

.home-button {
  width: 100%;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--forum-border);
  border-radius: 8px;
  background: #fff;
  color: var(--forum-text);
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

@media (max-width: 980px) {
  .forum-sidebar {
    position: static;
    max-height: none;
  }

  .board-panel {
    max-height: none;
  }
}
</style>
