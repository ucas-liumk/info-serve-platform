import type { Ref } from 'vue';
import { reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createResourceNote,
  deleteResourceNote,
  listMyResourceNotes,
  listPublicResourceNotes,
  listResourceViewRecords,
  updateResourceNote
} from '@/api/portal/resources';
import type { ResourceNote, ResourceNotePayload, ResourceViewRecord } from '@/api/infoservice/types';

type DataKey = 'my' | 'public' | 'records';

const unwrapRows = <T>(payload: any): T[] => {
  const body = payload?.data !== undefined && payload?.rows === undefined ? payload.data : payload;
  const table = body?.rows !== undefined ? body : body?.data;
  return Array.isArray(table?.rows) ? table.rows : [];
};

export const useResourcePreviewNotes = (resourceId: Ref<string>) => {
  const myNotes = ref<ResourceNote[]>([]);
  const publicNotes = ref<ResourceNote[]>([]);
  const viewRecords = ref<ResourceViewRecord[]>([]);
  const loading = reactive<Record<DataKey, boolean>>({ my: false, public: false, records: false });
  const loaded = reactive<Record<DataKey, boolean>>({ my: false, public: false, records: false });
  const errors = reactive<Record<DataKey, string>>({ my: '', public: '', records: '' });

  const noteForm = reactive({ content: '' });
  const notePublic = ref(false);
  const editingNoteId = ref<number | string>();
  const noteSubmitting = ref(false);
  const chatForm = reactive({ content: '' });
  const chatEditingNoteId = ref<number | string>();
  const chatSubmitting = ref(false);
  const visibilityUpdatingId = ref<number | string>();

  const loadData = async <T>(key: DataKey, request: () => Promise<any>, target: Ref<T[]>) => {
    if (!resourceId.value || loading[key]) return;
    loading[key] = true;
    errors[key] = '';
    try {
      target.value = unwrapRows<T>(await request());
      loaded[key] = true;
    } catch {
      errors[key] = '加载失败，请稍后重试';
    } finally {
      loading[key] = false;
    }
  };

  const loadMyNotes = () => loadData('my', () => listMyResourceNotes(resourceId.value, { pageNum: 1, pageSize: 50 }), myNotes);
  const loadPublicNotes = () => loadData('public', () => listPublicResourceNotes(resourceId.value, { pageNum: 1, pageSize: 50 }), publicNotes);
  const loadViewRecords = () => loadData('records', () => listResourceViewRecords(resourceId.value, { pageNum: 1, pageSize: 50 }), viewRecords);
  const refreshNotes = () => Promise.all([loadMyNotes(), loadPublicNotes()]);

  const resetNoteForm = () => {
    editingNoteId.value = undefined;
    noteForm.content = '';
    notePublic.value = false;
  };
  const resetChatForm = () => {
    chatEditingNoteId.value = undefined;
    chatForm.content = '';
  };

  const setNoteContent = (value: string) => {
    noteForm.content = value;
  };
  const setNotePublic = (value: boolean) => {
    notePublic.value = value;
  };
  const setChatContent = (value: string) => {
    chatForm.content = value;
  };

  const saveNote = (payload: ResourceNotePayload, noteId?: number | string) =>
    noteId === undefined ? createResourceNote(resourceId.value, payload) : updateResourceNote(resourceId.value, noteId, payload);

  const submitNote = async () => {
    const content = noteForm.content.trim();
    if (!content || !resourceId.value || noteSubmitting.value) return;
    noteSubmitting.value = true;
    try {
      await saveNote({ content, visibility: notePublic.value ? 'public' : 'private' }, editingNoteId.value);
      ElMessage.success(editingNoteId.value ? '笔记已保存' : '笔记已新增');
      resetNoteForm();
      await refreshNotes();
    } finally {
      noteSubmitting.value = false;
    }
  };

  const submitChat = async () => {
    const content = chatForm.content.trim();
    if (!content || !resourceId.value || chatSubmitting.value) return;
    chatSubmitting.value = true;
    try {
      await saveNote({ content, visibility: 'public' }, chatEditingNoteId.value);
      ElMessage.success(chatEditingNoteId.value ? '发言已更新' : '发言已发布');
      resetChatForm();
      await refreshNotes();
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

  const removeNote = async (note: ResourceNote) => {
    await ElMessageBox.confirm('确认删除这条笔记吗？', '删除笔记', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    await deleteResourceNote(resourceId.value, note.noteId);
    ElMessage.success('笔记已删除');
    if (editingNoteId.value === note.noteId) resetNoteForm();
    if (chatEditingNoteId.value === note.noteId) resetChatForm();
    await refreshNotes();
  };

  const changeNoteVisibility = async (note: ResourceNote, visibility: 'public' | 'private') => {
    if (visibilityUpdatingId.value !== undefined) return;
    visibilityUpdatingId.value = note.noteId;
    try {
      await updateResourceNote(resourceId.value, note.noteId, { content: note.content, visibility });
      ElMessage.success(visibility === 'public' ? '已分享到交流互动' : '已取消分享');
      await refreshNotes();
    } finally {
      visibilityUpdatingId.value = undefined;
    }
  };

  const quoteSelection = (text: string) => {
    noteForm.content = noteForm.content.trim() ? `${noteForm.content.trimEnd()}\n\n${text}` : text;
    ElMessage.success('已引用到我的笔记，可补充批注后保存');
  };

  watch(
    resourceId,
    () => {
      myNotes.value = [];
      publicNotes.value = [];
      viewRecords.value = [];
      (Object.keys(loaded) as DataKey[]).forEach((key) => {
        loaded[key] = false;
        errors[key] = '';
      });
      resetNoteForm();
      resetChatForm();
    },
    { immediate: true }
  );

  return {
    myNotes,
    publicNotes,
    viewRecords,
    loading,
    loaded,
    errors,
    noteForm,
    notePublic,
    editingNoteId,
    noteSubmitting,
    chatForm,
    chatEditingNoteId,
    chatSubmitting,
    visibilityUpdatingId,
    loadMyNotes,
    loadPublicNotes,
    loadViewRecords,
    submitNote,
    submitChat,
    editNote,
    editChatNote,
    resetNoteForm,
    resetChatForm,
    setNoteContent,
    setNotePublic,
    setChatContent,
    removeNote,
    changeNoteVisibility,
    quoteSelection
  };
};
