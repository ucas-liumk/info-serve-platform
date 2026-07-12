<template>
  <aside
    class="context-panel"
    :class="{ 'is-collapsed': panel.collapsed, 'is-resizing': resizing }"
    :style="panel.collapsed ? undefined : { width: `${panelWidth}px` }"
  >
    <template v-if="!panel.collapsed">
      <div
        class="resize-handle"
        role="separator"
        aria-orientation="vertical"
        aria-label="拖拽调节功能区宽度"
        title="拖拽调宽 · 双击复位"
        @pointerdown="startResize"
        @dblclick="resetPanelWidth"
      ></div>
      <header class="doc-head">
        <span class="doc-mark">{{ docMark }}</span>
        <strong class="doc-title" :title="docTitle">{{ docTitle }}</strong>
        <div class="doc-actions">
          <button class="doc-button" type="button" title="下载原文件" @click="emit('download')">
            <el-icon><Download /></el-icon>
          </button>
          <button class="doc-button" type="button" title="关闭预览" @click="emit('close')">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </header>

      <div class="panel-head">
        <template v-if="activeTile">
          <button class="crumb-back" type="button" title="关闭，返回功能区" @click="closeWorkspace">
            <el-icon><ArrowLeft /></el-icon>
            功能区
          </button>
          <span class="crumb-current">
            <el-icon class="crumb-glyph"><component :is="tileGlyph(activeTile)" /></el-icon>
            {{ activeTile.name }}
          </span>
        </template>
        <span v-else class="panel-title">功能区</span>
        <span class="head-spacer"></span>
        <button class="collapse-button" type="button" title="收起功能区" @click="toggleCollapse">
          <el-icon><DArrowRight /></el-icon>
        </button>
      </div>

      <template v-if="!activeTile">
        <div class="tile-grid">
          <button
            v-for="tile in tiles"
            :key="tile.key"
            type="button"
            class="tile"
            :class="`tile--${tile.tone}`"
            :disabled="tile.status !== 'active'"
            @click="handleTileClick(tile)"
          >
            <span class="tile-chip">
              <el-icon><component :is="tileGlyph(tile)" /></el-icon>
            </span>
            <span v-if="tile.status === 'soon'" class="tile-soon">即将上线</span>
            <span class="tile-name">{{ tile.name }}</span>
          </button>
        </div>
        <p class="overview-hint">点击磁贴进入对应功能 · 更多能力即将上线</p>
      </template>

      <section v-if="panel.view === 'chat'" class="workspace">
        <p class="workspace-title">交流互动 · 大家的公开讨论</p>
        <div class="editor-box">
          <el-input v-model="chatForm.content" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="发表你的看法，所有人可见…" />
          <div class="editor-row">
            <span class="editor-hint">{{ chatEditingNoteId ? '正在编辑我的发言' : '将以公开身份发布' }}</span>
            <div class="editor-actions">
              <button v-if="chatEditingNoteId" class="ghost-button" type="button" @click="resetChatForm">取消</button>
              <button class="primary-button" type="button" :disabled="chatSubmitting || !chatForm.content.trim()" @click="submitChat">
                {{ chatEditingNoteId ? '保存' : '发布' }}
              </button>
            </div>
          </div>
        </div>
        <div v-loading="listLoading.public" class="note-list">
          <article v-for="note in publicNotes" :key="note.noteId" class="note-item">
            <header>
              <strong>{{ note.authorName || '平台用户' }}</strong>
              <el-tag v-if="note.mine" size="small" type="primary">我的</el-tag>
            </header>
            <p>{{ note.content }}</p>
            <footer>
              <span class="note-time">{{ formatDateTime(note.updateTime || note.createTime) }}</span>
              <div v-if="note.mine" class="note-ops">
                <button class="link-button" type="button" @click="editChatNote(note)">编辑</button>
                <button class="link-button danger" type="button" @click="removeNote(note)">删除</button>
              </div>
            </footer>
          </article>
          <el-empty v-if="!listLoading.public && publicNotes.length === 0" :image-size="90" description="还没有公开讨论，来发第一条吧" />
        </div>
      </section>

      <section v-else-if="panel.view === 'note'" class="workspace">
        <p class="workspace-title">我的笔记 · 私有可切公开</p>
        <div class="editor-box">
          <el-input v-model="noteForm.content" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="记录你的想法、要点或摘录…" />
          <div class="editor-row">
            <el-switch v-model="notePublic" active-text="公开" inactive-text="私有" />
            <div class="editor-actions">
              <button v-if="editingNoteId" class="ghost-button" type="button" @click="resetNoteForm">取消</button>
              <button class="primary-button" type="button" :disabled="noteSubmitting || !noteForm.content.trim()" @click="submitNote">
                {{ editingNoteId ? '保存' : '新增' }}
              </button>
            </div>
          </div>
        </div>
        <div v-loading="listLoading.my" class="note-list">
          <article v-for="note in myNotes" :key="note.noteId" class="note-item">
            <header>
              <strong>{{ note.authorName || '我' }}</strong>
              <el-tag size="small" :type="note.visibility === 'public' ? 'success' : 'info'">
                {{ note.visibility === 'public' ? '公开' : '私有' }}
              </el-tag>
            </header>
            <p>{{ note.content }}</p>
            <footer>
              <span class="note-time">{{ formatDateTime(note.updateTime || note.createTime) }}</span>
              <div class="note-ops">
                <button class="link-button" type="button" @click="editNote(note)">编辑</button>
                <button
                  v-if="note.visibility === 'private'"
                  class="link-button"
                  type="button"
                  :disabled="visibilityUpdatingId === note.noteId"
                  @click="changeNoteVisibility(note, 'public')"
                >
                  分享到交流互动
                </button>
                <button
                  v-else
                  class="link-button"
                  type="button"
                  :disabled="visibilityUpdatingId === note.noteId"
                  @click="changeNoteVisibility(note, 'private')"
                >
                  取消分享
                </button>
                <button class="link-button danger" type="button" @click="removeNote(note)">删除</button>
              </div>
            </footer>
          </article>
          <el-empty v-if="!listLoading.my && myNotes.length === 0" :image-size="90" description="还没有笔记" />
        </div>
      </section>

      <section v-else-if="panel.view === 'info'" class="workspace">
        <p class="workspace-title">文件信息 · 资料元数据与阅看记录</p>
        <PanelFileInfo :items="infoItems" :records="viewRecords" :loading="listLoading.records" />
      </section>
    </template>

    <div v-else class="rail">
      <button class="rail-toggle" type="button" title="展开功能区" @click="toggleCollapse">
        <el-icon><DArrowLeft /></el-icon>
      </button>
      <div class="rail-tiles">
        <button
          v-for="tile in tiles"
          :key="tile.key"
          type="button"
          class="rail-tile"
          :class="[`tile--${tile.tone}`, { 'is-on': panel.view === tile.key }]"
          :disabled="tile.status !== 'active'"
          :title="tile.status === 'soon' ? `${tile.name}（即将上线）` : tile.name"
          @click="handleTileClick(tile)"
        >
          <span class="tile-chip">
            <el-icon><component :is="tileGlyph(tile)" /></el-icon>
          </span>
          <span class="rail-name">{{ tile.name }}</span>
        </button>
      </div>
      <div class="rail-foot">
        <button class="doc-button" type="button" title="下载原文件" @click="emit('download')">
          <el-icon><Download /></el-icon>
        </button>
        <button class="doc-button" type="button" title="关闭预览" @click="emit('close')">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { Component } from 'vue';
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Aim,
  ArrowLeft,
  ChatDotRound,
  Close,
  DArrowLeft,
  DArrowRight,
  Document,
  Download,
  Headset,
  MagicStick,
  Notebook,
  Share
} from '@element-plus/icons-vue';
import {
  createResourceNote,
  deleteResourceNote,
  listMyResourceNotes,
  listPublicResourceNotes,
  listResourceViewRecords,
  updateResourceNote
} from '@/api/portal/resources';
import type { InfoResource, ResourceNote, ResourceNotePayload, ResourceViewRecord } from '@/api/infoservice/types';
import type { PanelState, StudioTile } from './panelStudio';
import {
  buildResourceInfoItems,
  clampPanelWidth,
  DEFAULT_PANEL_STATE,
  DEFAULT_PANEL_WIDTH,
  formatDateTime,
  reducePanelState,
  STUDIO_TILES
} from './panelStudio';
import PanelFileInfo from './PanelFileInfo.vue';

const props = defineProps<{
  resource?: InfoResource;
  resourceId: string;
  /** 文件类型徽标（如 PDF/DOCX），由预览页按后缀推导 */
  typeLabel?: string;
}>();

const emit = defineEmits<{
  download: [];
  close: [];
}>();

/** 磁贴图标名 → Element 线性图标组件（政务风；新增磁贴时在此补映射） */
const TILE_GLYPHS: Record<string, Component> = {
  Notebook,
  ChatDotRound,
  Document,
  Aim,
  Headset,
  MagicStick,
  Share
};

const tileGlyph = (tile: StudioTile): Component => TILE_GLYPHS[tile.icon] ?? Document;

const tiles = STUDIO_TILES;
const panel = ref<PanelState>(DEFAULT_PANEL_STATE);
const infoItems = computed(() => buildResourceInfoItems(props.resource));

/** 拖拽调宽：宽度记忆到 localStorage，双击手柄复位；私密模式等存取异常静默回退默认宽 */
const PANEL_WIDTH_STORAGE_KEY = 'ip-preview-panel-width';

const readStoredPanelWidth = (): number => {
  try {
    const raw = window.localStorage.getItem(PANEL_WIDTH_STORAGE_KEY);
    return clampPanelWidth(raw === null ? DEFAULT_PANEL_WIDTH : Number(raw), window.innerWidth);
  } catch {
    return DEFAULT_PANEL_WIDTH;
  }
};

const persistPanelWidth = (width: number) => {
  try {
    window.localStorage.setItem(PANEL_WIDTH_STORAGE_KEY, String(width));
  } catch {
    /* 私密模式下不持久化 */
  }
};

const panelWidth = ref(readStoredPanelWidth());
const resizing = ref(false);

const startResize = (event: PointerEvent) => {
  const handle = event.currentTarget as HTMLElement;
  const startX = event.clientX;
  const startWidth = panelWidth.value;
  resizing.value = true;
  handle.setPointerCapture(event.pointerId);

  const onMove = (moveEvent: PointerEvent) => {
    panelWidth.value = clampPanelWidth(startWidth + (startX - moveEvent.clientX), window.innerWidth);
  };
  const stop = () => {
    resizing.value = false;
    persistPanelWidth(panelWidth.value);
    handle.removeEventListener('pointermove', onMove);
    handle.removeEventListener('pointerup', stop);
    handle.removeEventListener('pointercancel', stop);
  };
  handle.addEventListener('pointermove', onMove);
  handle.addEventListener('pointerup', stop);
  handle.addEventListener('pointercancel', stop);
  event.preventDefault();
};

const resetPanelWidth = () => {
  panelWidth.value = DEFAULT_PANEL_WIDTH;
  persistPanelWidth(DEFAULT_PANEL_WIDTH);
};
const activeTile = computed(() => tiles.find((tile) => tile.key === panel.value.view));
const docTitle = computed(() => props.resource?.title || props.resource?.originalName || '资料预览');
const docMark = computed(() => `.${(props.typeLabel || 'FILE').replace(/^\./, '')}`);

const myNotes = ref<ResourceNote[]>([]);
const publicNotes = ref<ResourceNote[]>([]);
const viewRecords = ref<ResourceViewRecord[]>([]);
const listLoading = reactive({ my: false, public: false, records: false });
const listLoaded = reactive({ my: false, public: false, records: false });

const noteForm = reactive({ content: '' });
const notePublic = ref(false);
const editingNoteId = ref<number | string>();
const noteSubmitting = ref(false);

const chatForm = reactive({ content: '' });
const chatEditingNoteId = ref<number | string>();
const chatSubmitting = ref(false);
const visibilityUpdatingId = ref<number | string>();

const unwrapRows = <T,>(payload: any): T[] => {
  const body = payload?.data !== undefined && payload?.rows === undefined ? payload.data : payload;
  const table = body?.rows !== undefined ? body : body?.data;
  return Array.isArray(table?.rows) ? table.rows : [];
};

/** 取数失败时保留旧数据；错误提示由 request 拦截器统一弹出 */
const loadMyNotes = async () => {
  if (!props.resourceId) return;
  listLoading.my = true;
  try {
    const res: any = await listMyResourceNotes(props.resourceId, { pageNum: 1, pageSize: 50 });
    myNotes.value = unwrapRows<ResourceNote>(res);
    listLoaded.my = true;
  } catch {
    /* 拦截器已提示 */
  } finally {
    listLoading.my = false;
  }
};

const loadPublicNotes = async () => {
  if (!props.resourceId) return;
  listLoading.public = true;
  try {
    const res: any = await listPublicResourceNotes(props.resourceId, { pageNum: 1, pageSize: 50 });
    publicNotes.value = unwrapRows<ResourceNote>(res);
    listLoaded.public = true;
  } catch {
    /* 拦截器已提示 */
  } finally {
    listLoading.public = false;
  }
};

const loadViewRecords = async () => {
  if (!props.resourceId) return;
  listLoading.records = true;
  try {
    const res: any = await listResourceViewRecords(props.resourceId, { pageNum: 1, pageSize: 50 });
    viewRecords.value = unwrapRows<ResourceViewRecord>(res);
    listLoaded.records = true;
  } catch {
    /* 拦截器已提示 */
  } finally {
    listLoading.records = false;
  }
};

/** 笔记与公开流互为镜像：任何写操作后双向刷新两个列表 */
const refreshNotes = async () => {
  await Promise.all([loadMyNotes(), loadPublicNotes()]);
};

const handleTileClick = (tile: StudioTile) => {
  const next = reducePanelState(panel.value, { type: 'clickTile', tile });
  if (next === panel.value) return;
  panel.value = next;
  if (next.view === 'note' && !listLoaded.my) loadMyNotes();
  if (next.view === 'chat' && !listLoaded.public) loadPublicNotes();
  if (next.view === 'info' && !listLoaded.records) loadViewRecords();
};

const closeWorkspace = () => {
  panel.value = reducePanelState(panel.value, { type: 'closeWorkspace' });
};

const toggleCollapse = () => {
  panel.value = reducePanelState(panel.value, { type: 'toggleCollapse' });
};

const saveNote = async (payload: ResourceNotePayload, noteId?: number | string) => {
  if (noteId !== undefined) {
    await updateResourceNote(props.resourceId, noteId, payload);
  } else {
    await createResourceNote(props.resourceId, payload);
  }
};

const submitNote = async () => {
  const content = noteForm.content.trim();
  if (!content || !props.resourceId || noteSubmitting.value) return;
  noteSubmitting.value = true;
  try {
    await saveNote({ content, visibility: notePublic.value ? 'public' : 'private' }, editingNoteId.value);
    ElMessage.success(editingNoteId.value ? '笔记已保存' : '笔记已新增');
    resetNoteForm();
    await refreshNotes();
  } catch {
    /* 拦截器已提示 */
  } finally {
    noteSubmitting.value = false;
  }
};

const submitChat = async () => {
  const content = chatForm.content.trim();
  if (!content || !props.resourceId || chatSubmitting.value) return;
  chatSubmitting.value = true;
  try {
    await saveNote({ content, visibility: 'public' }, chatEditingNoteId.value);
    ElMessage.success(chatEditingNoteId.value ? '发言已更新' : '发言已发布');
    resetChatForm();
    await refreshNotes();
  } catch {
    /* 拦截器已提示 */
  } finally {
    chatSubmitting.value = false;
  }
};

const editNote = (note: ResourceNote) => {
  editingNoteId.value = note.noteId;
  noteForm.content = note.content;
  notePublic.value = note.visibility === 'public';
};

const editChatNote = (note: ResourceNote) => {
  chatEditingNoteId.value = note.noteId;
  chatForm.content = note.content;
};

const resetNoteForm = () => {
  editingNoteId.value = undefined;
  noteForm.content = '';
  notePublic.value = false;
};

const resetChatForm = () => {
  chatEditingNoteId.value = undefined;
  chatForm.content = '';
};

const removeNote = async (note: ResourceNote) => {
  try {
    await ElMessageBox.confirm('确认删除这条笔记吗？', '删除笔记', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
  } catch {
    return; // 用户取消
  }
  try {
    await deleteResourceNote(props.resourceId, note.noteId);
    ElMessage.success('笔记已删除');
    if (editingNoteId.value === note.noteId) resetNoteForm();
    if (chatEditingNoteId.value === note.noteId) resetChatForm();
    await refreshNotes();
  } catch {
    /* 拦截器已提示 */
  }
};

/** 阅读器划词引用：展开「我的笔记」工作区并把引用文本追加进编辑器（保存动作留给用户补充后自行触发） */
const quoteSelection = (text: string) => {
  const noteTile = tiles.find((tile) => tile.key === 'note');
  if (noteTile) handleTileClick(noteTile);
  noteForm.content = noteForm.content.trim() ? `${noteForm.content.trimEnd()}\n\n${text}` : text;
  ElMessage.success('已引用到我的笔记，可补充批注后保存');
};

defineExpose({ quoteSelection });

/** 分享到交流互动 / 取消分享：只改可见性，内容原样保留 */
const changeNoteVisibility = async (note: ResourceNote, visibility: 'public' | 'private') => {
  if (visibilityUpdatingId.value !== undefined) return;
  visibilityUpdatingId.value = note.noteId;
  try {
    await updateResourceNote(props.resourceId, note.noteId, { content: note.content, visibility });
    ElMessage.success(visibility === 'public' ? '已分享到交流互动' : '已取消分享');
    await refreshNotes();
  } catch {
    /* 拦截器已提示 */
  } finally {
    visibilityUpdatingId.value = undefined;
  }
};

watch(
  () => props.resourceId,
  () => {
    panel.value = DEFAULT_PANEL_STATE;
    myNotes.value = [];
    publicNotes.value = [];
    viewRecords.value = [];
    listLoaded.my = false;
    listLoaded.public = false;
    listLoaded.records = false;
    resetNoteForm();
    resetChatForm();
  },
  { immediate: true }
);
</script>

<style scoped>
.context-panel {
  position: relative;
  width: 392px;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 全局 aside 样式会注入 padding，收起态 96px 容不下，间距改由内部元素自控 */
  padding: 0;
  border: 1px solid var(--resource-border);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-md);
  transition: width var(--ip-motion-fast) var(--ip-motion-ease);
}

/* 收起态行内宽度不生效（:style 仅在展开时绑定），固定 96px */
.context-panel.is-collapsed {
  width: 96px;
}

/* 拖拽期间关闭宽度过渡并禁选文字，跟手不拖影 */
.context-panel.is-resizing {
  transition: none;
  user-select: none;
}

.resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 6;
  width: 6px;
  cursor: col-resize;
  transition: background var(--ip-motion-fast) var(--ip-motion-ease);
}

.resize-handle:hover {
  background: var(--ip-primary-50);
}

.context-panel.is-resizing .resize-handle {
  background: var(--ip-primary-200);
}

.doc-head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 9px;
  border-bottom: 1px solid var(--ip-neutral-100);
  padding: 10px 12px;
}

.doc-mark {
  flex: 0 0 auto;
  border-radius: var(--ip-radius-sm);
  padding: 4px 7px;
  background: var(--ip-primary-50);
  color: var(--resource-primary);
  font-size: var(--ip-font-caption);
  font-weight: 900;
}

.doc-title {
  /* 两行钳制：完整名进 title tooltip，超两行才省略 */
  display: -webkit-box;
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: var(--resource-title);
  font-size: var(--ip-font-body);
  line-height: 1.4;
  font-weight: 850;
  word-break: break-all;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.doc-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 5px;
}

.doc-button {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--resource-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--resource-muted);
  cursor: pointer;
  transition:
    color var(--ip-motion-fast) var(--ip-motion-ease),
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease);
}

.doc-button:hover {
  border-color: var(--ip-primary-200);
  background: var(--ip-primary-50);
  color: var(--resource-primary);
}

.panel-head {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--ip-neutral-100);
  padding: 10px 12px;
}

.panel-title {
  color: var(--resource-title);
  font-size: var(--ip-font-body);
  font-weight: 900;
}

.head-spacer {
  flex: 1 1 auto;
}

.crumb-back {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--resource-primary);
  font-size: var(--ip-font-hint);
  font-weight: 850;
  cursor: pointer;
}

.crumb-back:hover {
  text-decoration: underline;
}

.crumb-current {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  color: var(--resource-title);
  font-size: var(--ip-font-hint);
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.crumb-current::before {
  content: '/';
  margin-right: 3px;
  color: var(--ip-neutral-200);
  font-weight: 750;
}

.crumb-glyph {
  color: var(--resource-primary);
}

.collapse-button {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 1px solid var(--resource-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--resource-muted);
  cursor: pointer;
  transition:
    color var(--ip-motion-fast) var(--ip-motion-ease),
    border-color var(--ip-motion-fast) var(--ip-motion-ease);
}

.collapse-button:hover {
  border-color: var(--ip-primary-200);
  color: var(--resource-primary);
}

.tile-grid {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 12px 14px;
}

/* 政务风磁贴：白底描边卡片 + 粉彩衬底线性图标（tone 只作用于图标衬底，不再整卡糖果色） */
.tile {
  position: relative;
  border: 1px solid var(--ip-neutral-100);
  border-radius: var(--ip-radius-md);
  padding: 11px 12px 10px;
  background: var(--ip-neutral-0);
  text-align: left;
  cursor: pointer;
  transition:
    transform var(--ip-motion-fast) var(--ip-motion-ease),
    box-shadow var(--ip-motion-fast) var(--ip-motion-ease),
    border-color var(--ip-motion-fast) var(--ip-motion-ease);
}

.tile:disabled {
  cursor: default;
}

.tile:not(:disabled):hover {
  border-color: var(--ip-primary-200);
  transform: translateY(-1px);
  box-shadow: var(--ip-shadow-md);
}

.tile-chip {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ip-radius-sm);
  font-size: var(--ip-font-emphasis);
}

.tile--blue .tile-chip {
  background: var(--ip-tile-blue);
  color: var(--ip-tile-blue-ink);
}

.tile--green .tile-chip {
  background: var(--ip-tile-green);
  color: var(--ip-tile-green-ink);
}

.tile--amber .tile-chip {
  background: var(--ip-tile-amber);
  color: var(--ip-tile-amber-ink);
}

.tile--purple .tile-chip {
  background: var(--ip-tile-purple);
  color: var(--ip-tile-purple-ink);
}

.tile--cyan .tile-chip {
  background: var(--ip-tile-cyan);
  color: var(--ip-tile-cyan-ink);
}

.tile--pink .tile-chip {
  background: var(--ip-tile-pink);
  color: var(--ip-tile-pink-ink);
}

.tile:disabled .tile-chip {
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-400);
}

.tile-name {
  display: block;
  margin-top: 8px;
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-caption);
  font-weight: 800;
}

.tile:disabled .tile-name {
  color: var(--ip-neutral-400);
}

.tile-soon {
  position: absolute;
  top: 8px;
  right: 8px;
  border: 1px solid var(--ip-neutral-100);
  border-radius: var(--ip-radius-full);
  padding: 1px 6px;
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-caption);
  font-weight: 750;
}

.overview-hint {
  margin: auto 14px 14px;
  padding-top: 10px;
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-caption);
  font-weight: 750;
  text-align: center;
}

.workspace {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding-bottom: 10px;
}

.workspace-title {
  margin: 0;
  padding: 10px 14px 4px;
  color: var(--resource-weak);
  font-size: var(--ip-font-caption);
  font-weight: 800;
}

.editor-box {
  margin: 8px 14px;
  display: grid;
  gap: 8px;
  border: 1px solid var(--resource-border);
  border-radius: var(--ip-radius-md);
  padding: 9px;
  background: var(--ip-neutral-50);
}

.editor-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.editor-hint {
  color: var(--ip-neutral-400);
  font-size: var(--ip-font-caption);
  font-weight: 750;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ghost-button,
.primary-button {
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border-radius: var(--ip-radius-sm);
  padding: 0 12px;
  font-size: var(--ip-font-caption);
  font-weight: 800;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid var(--resource-border);
  background: var(--ip-neutral-0);
  color: var(--resource-text);
}

.ghost-button:hover {
  border-color: var(--resource-primary);
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
}

.primary-button {
  border: 1px solid var(--resource-primary);
  background: var(--resource-primary);
  color: var(--ip-neutral-0);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.primary-button:hover:not(:disabled) {
  filter: brightness(0.92);
}

.note-list {
  min-height: 120px;
  display: grid;
  align-content: start;
}

.note-item {
  display: grid;
  gap: 6px;
  border-bottom: 1px solid var(--ip-neutral-100);
  padding: 10px 14px;
  transition: background var(--ip-motion-fast) var(--ip-motion-ease);
}

.note-item:hover {
  background: var(--ip-neutral-50);
}

.note-item header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.note-item header strong {
  min-width: 0;
  overflow: hidden;
  color: var(--resource-title);
  font-size: var(--ip-font-hint);
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-item p {
  margin: 0;
  color: var(--resource-text);
  font-size: var(--ip-font-hint);
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.note-item footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.note-time {
  color: var(--resource-weak);
  font-size: var(--ip-font-caption);
  font-weight: 750;
}

.note-ops {
  display: flex;
  align-items: center;
  gap: 10px;
}

.link-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--resource-primary);
  font-size: var(--ip-font-caption);
  font-weight: 850;
  cursor: pointer;
}

.link-button:disabled {
  opacity: 0.5;
  cursor: default;
}

.link-button.danger {
  color: var(--ip-danger);
}

.rail {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
}

.rail-toggle {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 1px solid var(--resource-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-0);
  color: var(--resource-muted);
  cursor: pointer;
  transition:
    color var(--ip-motion-fast) var(--ip-motion-ease),
    border-color var(--ip-motion-fast) var(--ip-motion-ease);
}

.rail-toggle:hover {
  border-color: var(--ip-primary-200);
  color: var(--resource-primary);
}

.rail-tiles {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  width: 100%;
  overflow-y: auto;
}

.rail-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--ip-neutral-100);
  border-radius: var(--ip-radius-md);
  padding: 8px 2px;
  background: var(--ip-neutral-0);
  cursor: pointer;
  transition:
    transform var(--ip-motion-fast) var(--ip-motion-ease),
    border-color var(--ip-motion-fast) var(--ip-motion-ease);
}

.rail-tile:disabled {
  cursor: default;
}

.rail-tile:disabled .rail-name {
  color: var(--ip-neutral-400);
}

.rail-tile:disabled .tile-chip {
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-400);
}

.rail-tile:not(:disabled):hover {
  border-color: var(--ip-primary-200);
  transform: translateY(-1px);
}

.rail-tile.is-on {
  border-color: var(--resource-primary);
}

.rail-tile .tile-chip {
  width: 28px;
  height: 28px;
}

.rail-name {
  max-width: 100%;
  overflow: hidden;
  color: var(--ip-neutral-700);
  font-size: var(--ip-font-caption);
  font-weight: 800;
  text-align: center;
  white-space: nowrap;
}

.rail-foot {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-top: 1px solid var(--ip-neutral-100);
  padding-top: 10px;
}

@media (max-width: 1180px) {
  /* 堆叠布局下宽度铺满，压过拖拽产生的行内宽度 */
  .context-panel,
  .context-panel.is-collapsed {
    width: 100% !important;
  }

  .resize-handle {
    display: none;
  }

  .rail {
    flex-direction: row;
    padding: 8px 10px;
  }

  .rail-tiles {
    flex-direction: row;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .rail-tile {
    flex: 0 0 auto;
    min-width: 64px;
  }

  .rail-foot {
    flex-direction: row;
    border-top: 0;
    border-left: 1px solid var(--ip-neutral-100);
    padding-top: 0;
    padding-left: 10px;
  }
}

@media (max-width: 760px) {
  .editor-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
