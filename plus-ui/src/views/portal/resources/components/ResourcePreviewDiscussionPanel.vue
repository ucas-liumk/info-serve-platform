<template>
  <section class="workspace" aria-label="公开讨论区">
    <header class="workspace-intro"><p>围绕当前资料展开公开讨论</p></header>
    <div class="editor-box">
      <el-input v-model="chatContent" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="发表你的看法，所有人可见…" />
      <div class="editor-row">
        <span class="editor-hint">{{ model.chatEditingNoteId.value ? '正在编辑我的发言' : '将以公开身份发布' }}</span>
        <div class="editor-actions">
          <button v-if="model.chatEditingNoteId.value" class="secondary-button" type="button" @click="model.resetChatForm">取消</button
          ><button class="primary-button" type="button" :disabled="model.chatSubmitting.value || !chatContent.trim()" @click="model.submitChat">
            {{ model.chatSubmitting.value ? '发布中…' : model.chatEditingNoteId.value ? '保存修改' : '发布' }}
          </button>
        </div>
      </div>
    </div>
    <div v-if="model.errors.public" class="data-state" role="alert">
      <strong>讨论加载失败</strong><span>{{ model.errors.public }}</span
      ><button type="button" @click="model.loadPublicNotes">重新加载</button>
    </div>
    <div v-else v-loading="model.loading.public" class="note-list">
      <article v-for="note in model.publicNotes.value" :key="note.noteId" class="note-item">
        <header>
          <strong>{{ note.authorName || '平台用户' }}</strong
          ><span v-if="note.mine" class="status-tag">我的</span>
        </header>
        <p>{{ note.content }}</p>
        <footer>
          <time>{{ formatDateTime(note.updateTime || note.createTime) }}</time>
          <div v-if="note.mine" class="note-actions">
            <button type="button" @click="model.editChatNote(note)">编辑</button
            ><button class="danger-link" type="button" @click="remove(note)">删除</button>
          </div>
        </footer>
      </article>
      <div v-if="!model.loading.public && model.publicNotes.value.length === 0" class="data-state">
        <strong>还没有公开讨论</strong><span>发布第一条看法，与大家交流。</span>
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
const chatContent = computed({
  get: () => props.model.chatForm.content,
  set: (value: string) => {
    props.model.setChatContent(value);
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
