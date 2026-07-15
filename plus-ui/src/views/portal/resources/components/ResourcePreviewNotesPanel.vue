<template>
  <section class="workspace" aria-label="我的笔记编辑区">
    <header class="workspace-intro"><p>私有保存，也可选择公开到交流互动</p></header>
    <div class="editor-box">
      <el-input v-model="noteContent" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="记录想法、要点或正文摘录…" />
      <div class="editor-row">
        <el-switch v-model="noteVisibility" active-text="公开" inactive-text="私有" />
        <div class="editor-actions">
          <button v-if="model.editingNoteId.value" class="secondary-button" type="button" @click="model.resetNoteForm">取消</button>
          <button class="primary-button" type="button" :disabled="model.noteSubmitting.value || !noteContent.trim()" @click="model.submitNote">
            {{ model.noteSubmitting.value ? '保存中…' : model.editingNoteId.value ? '保存修改' : '保存笔记' }}
          </button>
        </div>
      </div>
    </div>
    <div v-if="model.errors.my" class="data-state" role="alert">
      <strong>笔记加载失败</strong><span>{{ model.errors.my }}</span
      ><button type="button" @click="model.loadMyNotes">重新加载</button>
    </div>
    <div v-else v-loading="model.loading.my" class="note-list">
      <article v-for="note in model.myNotes.value" :key="note.noteId" class="note-item">
        <header>
          <strong>{{ note.authorName || '我' }}</strong
          ><span class="status-tag">{{ note.visibility === 'public' ? '公开' : '私有' }}</span>
        </header>
        <p>{{ note.content }}</p>
        <footer>
          <time>{{ formatDateTime(note.updateTime || note.createTime) }}</time>
          <div class="note-actions">
            <button type="button" @click="model.editNote(note)">编辑</button
            ><button
              type="button"
              :disabled="model.visibilityUpdatingId.value === note.noteId"
              @click="model.changeNoteVisibility(note, note.visibility === 'private' ? 'public' : 'private')"
            >
              {{ note.visibility === 'private' ? '分享到交流' : '取消分享' }}</button
            ><button class="danger-link" type="button" @click="remove(note)">删除</button>
          </div>
        </footer>
      </article>
      <div v-if="!model.loading.my && model.myNotes.value.length === 0" class="data-state">
        <strong>还没有笔记</strong><span>可在正文中选中文字并引用到这里。</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ResourceNote } from '@/api/infoservice/types';
import { formatDateTime } from './panelStudio';
import type { useResourcePreviewNotes } from './useResourcePreviewNotes';

type NotesModel = ReturnType<typeof useResourcePreviewNotes>;
const props = defineProps<{ model: NotesModel }>();
const noteContent = computed({
  get: () => props.model.noteForm.content,
  set: (value: string) => {
    props.model.setNoteContent(value);
  }
});
const noteVisibility = computed({
  get: () => props.model.notePublic.value,
  set: (value: boolean) => {
    props.model.setNotePublic(value);
  }
});
const remove = async (note: ResourceNote) => {
  try {
    await props.model.removeNote(note);
  } catch {
    /* 用户取消或请求层已提示 */
  }
};
</script>

<style scoped src="./resource-preview-panels.scss"></style>
