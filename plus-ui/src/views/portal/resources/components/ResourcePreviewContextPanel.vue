<template>
  <aside class="context-panel">
    <el-tabs v-model="activeTab" class="context-tabs" stretch @tab-change="handleTabChange">
      <el-tab-pane label="资料信息" name="info">
        <section class="info-card">
          <div class="file-mark">{{ typeLabel }}</div>
          <div class="file-title">
            <h1>{{ resource?.title || '资料信息' }}</h1>
            <p>{{ resource?.description || '暂无资料简介' }}</p>
          </div>
        </section>

        <section class="metric-card">
          <div>
            <strong>{{ resource?.viewCount || 0 }}</strong>
            <span>阅看</span>
          </div>
          <div>
            <strong>{{ resource?.downloadCount || 0 }}</strong>
            <span>下载</span>
          </div>
          <div>
            <strong>{{ resource?.favoriteCount || 0 }}</strong>
            <span>收藏</span>
          </div>
          <div>
            <strong>{{ resource?.status === '0' ? '上架' : '草稿' }}</strong>
            <span>状态</span>
          </div>
        </section>

        <section class="meta-card">
          <h2>基础信息</h2>
          <dl>
            <div>
              <dt>资料分类</dt>
              <dd>{{ resource?.categoryName || '未分类' }}</dd>
            </div>
            <div>
              <dt>原始文件</dt>
              <dd>{{ resource?.originalName || '-' }}</dd>
            </div>
            <div>
              <dt>文件类型</dt>
              <dd>{{ resource?.mimeType || resource?.fileSuffix || resource?.previewType || '-' }}</dd>
            </div>
            <div>
              <dt>文件大小</dt>
              <dd>{{ formatSize(resource?.fileSize) }}</dd>
            </div>
            <div>
              <dt>发布人</dt>
              <dd>{{ resource?.ownerName || resource?.createByName || '平台用户' }}</dd>
            </div>
            <div>
              <dt>发布时间</dt>
              <dd>{{ resource?.createTime || '-' }}</dd>
            </div>
          </dl>
        </section>
      </el-tab-pane>

      <el-tab-pane label="我的笔记" name="my">
        <section class="note-editor">
          <el-input
            v-model="noteForm.content"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="记录你的阅读理解、待办或摘录"
          />
          <div class="note-editor-actions">
            <el-switch v-model="notePublic" active-text="公开" inactive-text="私有" />
            <div>
              <button v-if="editingNoteId" class="ghost-button compact" type="button" @click="resetNoteForm">取消</button>
              <button class="primary-button compact" type="button" :disabled="noteSubmitting || !noteForm.content.trim()" @click="submitNote">
                <el-icon><Check /></el-icon>
                {{ editingNoteId ? '保存' : '新增' }}
              </button>
            </div>
          </div>
        </section>

        <section v-loading="tabLoading.my" class="list-panel">
          <article v-for="note in myNotes" :key="note.noteId" class="note-item">
            <header>
              <strong>{{ note.authorName || '我' }}</strong>
              <el-tag size="small" :type="note.visibility === 'public' ? 'success' : 'info'">
                {{ note.visibility === 'public' ? '公开' : '私有' }}
              </el-tag>
            </header>
            <p>{{ note.content }}</p>
            <footer>
              <span>{{ formatDateTime(note.updateTime || note.createTime) }}</span>
              <div>
                <button type="button" @click="editNote(note)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </button>
                <button class="danger" type="button" @click="removeNote(note)">
                  <el-icon><Delete /></el-icon>
                  删除
                </button>
              </div>
            </footer>
          </article>
          <el-empty v-if="!tabLoading.my && myNotes.length === 0" :image-size="90" description="还没有笔记" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="公开笔记" name="public">
        <section v-loading="tabLoading.public" class="list-panel">
          <article v-for="note in publicNotes" :key="note.noteId" class="note-item">
            <header>
              <strong>{{ note.authorName || '平台用户' }}</strong>
              <el-tag v-if="note.mine" size="small" type="primary">我的</el-tag>
            </header>
            <p>{{ note.content }}</p>
            <footer>
              <span>{{ formatDateTime(note.updateTime || note.createTime) }}</span>
            </footer>
          </article>
          <el-empty v-if="!tabLoading.public && publicNotes.length === 0" :image-size="90" description="暂无公开笔记" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="阅看记录" name="records">
        <section v-loading="tabLoading.records" class="list-panel timeline-panel">
          <article v-for="record in viewRecords" :key="record.recordId" class="record-item">
            <span></span>
            <div>
              <strong>{{ record.userName || '平台用户' }}</strong>
              <p>{{ record.actionType === 'view' ? '打开资料预览' : '查看资料' }}</p>
              <time>{{ formatDateTime(record.createTime) }}</time>
            </div>
          </article>
          <el-empty v-if="!tabLoading.records && viewRecords.length === 0" :image-size="90" description="暂无阅看记录" />
        </section>
      </el-tab-pane>
    </el-tabs>
  </aside>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Check, Delete, Edit } from '@element-plus/icons-vue';
import {
  createResourceNote,
  deleteResourceNote,
  listMyResourceNotes,
  listPublicResourceNotes,
  listResourceViewRecords,
  updateResourceNote
} from '@/api/portal/resources';
import type { InfoResource, ResourceNote, ResourceViewRecord } from '@/api/infoservice/types';

type PreviewTab = 'info' | 'my' | 'public' | 'records';

const props = defineProps<{
  resource?: InfoResource;
  resourceId: string;
  typeLabel: string;
}>();

const activeTab = ref<PreviewTab>('info');
const myNotes = ref<ResourceNote[]>([]);
const publicNotes = ref<ResourceNote[]>([]);
const viewRecords = ref<ResourceViewRecord[]>([]);
const editingNoteId = ref<number | string>();
const noteSubmitting = ref(false);
const notePublic = ref(false);
const noteForm = reactive({ content: '' });
const tabLoading = reactive({ my: false, public: false, records: false });

const unwrapRows = <T,>(payload: any): T[] => {
  const body = payload?.data !== undefined && payload?.rows === undefined ? payload.data : payload;
  const table = body?.rows !== undefined ? body : body?.data;
  return Array.isArray(table?.rows) ? table.rows : [];
};

const loadMyNotes = async () => {
  if (!props.resourceId) return;
  tabLoading.my = true;
  try {
    const res: any = await listMyResourceNotes(props.resourceId, { pageNum: 1, pageSize: 50 });
    myNotes.value = unwrapRows<ResourceNote>(res);
  } finally {
    tabLoading.my = false;
  }
};

const loadPublicNotes = async () => {
  if (!props.resourceId) return;
  tabLoading.public = true;
  try {
    const res: any = await listPublicResourceNotes(props.resourceId, { pageNum: 1, pageSize: 50 });
    publicNotes.value = unwrapRows<ResourceNote>(res);
  } finally {
    tabLoading.public = false;
  }
};

const loadViewRecords = async () => {
  if (!props.resourceId) return;
  tabLoading.records = true;
  try {
    const res: any = await listResourceViewRecords(props.resourceId, { pageNum: 1, pageSize: 50 });
    viewRecords.value = unwrapRows<ResourceViewRecord>(res);
  } finally {
    tabLoading.records = false;
  }
};

const handleTabChange = (tabName: string | number) => {
  if (tabName === 'my' && myNotes.value.length === 0) {
    loadMyNotes();
  }
  if (tabName === 'public' && publicNotes.value.length === 0) {
    loadPublicNotes();
  }
  if (tabName === 'records' && viewRecords.value.length === 0) {
    loadViewRecords();
  }
};

const submitNote = async () => {
  const content = noteForm.content.trim();
  if (!content || !props.resourceId) return;
  noteSubmitting.value = true;
  try {
    const payload = { content, visibility: notePublic.value ? ('public' as const) : ('private' as const) };
    if (editingNoteId.value) {
      await updateResourceNote(props.resourceId, editingNoteId.value, payload);
      ElMessage.success('笔记已保存');
    } else {
      await createResourceNote(props.resourceId, payload);
      ElMessage.success('笔记已新增');
    }
    resetNoteForm();
    await loadMyNotes();
    if (publicNotes.value.length > 0 || payload.visibility === 'public') {
      await loadPublicNotes();
    }
  } finally {
    noteSubmitting.value = false;
  }
};

const editNote = (note: ResourceNote) => {
  editingNoteId.value = note.noteId;
  noteForm.content = note.content;
  notePublic.value = note.visibility === 'public';
};

const resetNoteForm = () => {
  editingNoteId.value = undefined;
  noteForm.content = '';
  notePublic.value = false;
};

const removeNote = async (note: ResourceNote) => {
  await ElMessageBox.confirm('确认删除这条笔记吗？', '删除笔记', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
  await deleteResourceNote(props.resourceId, note.noteId);
  ElMessage.success('笔记已删除');
  await loadMyNotes();
  if (note.visibility === 'public') {
    await loadPublicNotes();
  }
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
};

watch(
  () => props.resourceId,
  () => {
    activeTab.value = 'info';
    myNotes.value = [];
    publicNotes.value = [];
    viewRecords.value = [];
    resetNoteForm();
  }
);
</script>

<style scoped>
.context-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 0 14px 14px;
}

.context-tabs {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.context-tabs :deep(.el-tabs__content) {
  min-height: 0;
  overflow: auto;
}

.context-tabs :deep(.el-tab-pane) {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 2px 0 12px;
}

.context-tabs :deep(.el-tabs__item) {
  height: 48px;
  color: var(--resource-muted);
  font-weight: 850;
}

.context-tabs :deep(.el-tabs__item.is-active) {
  color: var(--resource-primary);
}

.info-card,
.meta-card,
.metric-card,
.note-editor,
.note-item,
.record-item {
  border: 1px solid var(--resource-border);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(31, 54, 76, 0.07);
}

.info-card,
.meta-card,
.metric-card,
.note-editor {
  padding: 14px;
}

.info-card {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
}

.file-mark {
  width: 64px;
  height: 78px;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border: 1px solid #d7e5fb;
  border-radius: 8px;
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
  font-size: 12px;
  font-weight: 900;
}

.file-title {
  min-width: 0;
}

.file-title h1 {
  margin: 0;
  color: var(--resource-title);
  font-size: 18px;
  line-height: 1.35;
  font-weight: 900;
}

.file-title p {
  margin: 9px 0 0;
  display: -webkit-box;
  overflow: hidden;
  color: var(--resource-muted);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 700;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.metric-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card div {
  border-radius: 8px;
  padding: 13px 8px;
  background: #f5f7fa;
  text-align: center;
}

.metric-card strong {
  display: block;
  overflow: hidden;
  color: var(--resource-title);
  font-size: 22px;
  line-height: 1.1;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-card span {
  display: block;
  margin-top: 5px;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 800;
}

.meta-card h2 {
  margin: 0 0 14px;
  color: var(--resource-title);
  font-size: 16px;
  font-weight: 900;
}

.meta-card dl {
  margin: 0;
  display: grid;
  gap: 12px;
}

.meta-card dl div {
  display: grid;
  gap: 5px;
}

.meta-card dt {
  color: var(--resource-weak);
  font-size: 12px;
  font-weight: 800;
}

.meta-card dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--resource-text);
  font-size: 14px;
  line-height: 1.45;
  font-weight: 760;
}

.note-editor {
  display: grid;
  gap: 10px;
}

.note-editor-actions,
.note-editor-actions > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-editor-actions {
  justify-content: space-between;
}

.list-panel {
  min-height: 220px;
  display: grid;
  align-content: start;
  gap: 10px;
}

.note-item {
  padding: 13px;
}

.note-item header,
.note-item footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.note-item header strong {
  min-width: 0;
  overflow: hidden;
  color: var(--resource-title);
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-item p {
  margin: 10px 0;
  color: var(--resource-text);
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.note-item footer span,
.record-item time {
  color: var(--resource-weak);
  font-size: 12px;
  font-weight: 750;
}

.note-item footer div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-item footer button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--resource-primary);
  font-size: 12px;
  font-weight: 850;
  cursor: pointer;
}

.note-item footer button.danger {
  color: #c2410c;
}

.timeline-panel {
  gap: 8px;
}

.record-item {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
}

.record-item > span {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--resource-primary);
  box-shadow: 0 0 0 4px rgba(36, 95, 143, 0.12);
}

.record-item strong {
  display: block;
  color: var(--resource-title);
  font-size: 14px;
  font-weight: 900;
}

.record-item p {
  margin: 4px 0;
  color: var(--resource-muted);
  font-size: 13px;
  font-weight: 760;
}

.ghost-button,
.primary-button {
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  padding: 0 13px;
  font-weight: 850;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid #d3dee8;
  background: #fff;
  color: var(--resource-text);
}

.primary-button {
  border: 1px solid var(--resource-primary);
  background: var(--resource-primary);
  color: #fff;
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.primary-button:hover:not(:disabled) {
  background: #183f63;
}

.ghost-button:hover {
  border-color: var(--resource-primary);
  background: var(--resource-primary-soft);
  color: var(--resource-primary);
}

.compact {
  height: 34px;
  padding: 0 11px;
  font-size: 13px;
}

@media (max-width: 760px) {
  .context-panel {
    padding: 0 10px 10px;
  }

  .note-editor-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
