<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="620px" class="resource-upload-dialog" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="160" :placeholder="titlePlaceholder" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" class="full">
          <el-option v-for="cat in categories" :key="cat.categoryId" :label="cat.categoryName" :value="cat.categoryId" />
        </el-select>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" placeholder="补充资料用途、适用场景或注意事项" />
      </el-form-item>
      <el-form-item label="文件" prop="file">
        <div class="file-field">
          <div v-if="isEdit && resource?.originalName" class="current-file">
            <strong>当前文件</strong>
            <span>{{ resource.originalName }}</span>
          </div>
          <div :class="['native-upload', { selected: selectedFiles.length > 0 }]" @dragover.prevent @drop.prevent="onDropFile">
            <label class="file-picker">
              <input ref="fileInputRef" class="native-file-input" type="file" :multiple="!isEdit" @change="onFileInputChange" />
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <strong>{{ uploadTitle }}</strong>
              <span>{{ selectedFiles.length ? selectedSummary : uploadHelpText }}</span>
            </label>
            <ul v-if="selectedFiles.length" class="selected-files">
              <li v-for="(file, index) in selectedFiles" :key="`${file.name}-${file.lastModified}-${index}`">
                <span>
                  <strong>{{ file.name }}</strong>
                  <em>{{ formatSize(file.size) }}</em>
                </span>
                <button type="button" @click="removeSelectedFile(index)">移除</button>
              </li>
            </ul>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <button class="dialog-cancel" type="button" @click="visible = false">取消</button>
      <button class="dialog-submit" type="button" :disabled="submitting" @click="submit">
        {{ submitting ? '保存中' : isEdit ? '保存修改' : '发布资料' }}
      </button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { InfoResource, ResourceCategory } from '@/api/infoservice/types';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    categories: ResourceCategory[];
    resource?: InfoResource;
    mode?: 'create' | 'edit';
    submitting?: boolean;
  }>(),
  {
    mode: 'create',
    submitting: false
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'submit', payload: { title: string; categoryId: number | string | undefined; description: string; files?: File[] }): void;
}>();

const formRef = ref<FormInstance>();
const fileInputRef = ref<HTMLInputElement>();
const selectedFiles = ref<File[]>([]);
const form = reactive({
  title: '',
  categoryId: undefined as number | string | undefined,
  description: '',
  file: ''
});

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const isEdit = computed(() => props.mode === 'edit' && Boolean(props.resource?.resourceId));
const dialogTitle = computed(() => (isEdit.value ? '编辑资料' : '上传资料'));
const titlePlaceholder = computed(() => (isEdit.value ? '请输入资料标题' : '单文件可自定义标题，多文件默认使用文件名'));
const uploadHelpText = computed(() => (isEdit.value ? '点击选择或拖入新文件，留空则不替换文件' : '点击选择或拖入一个或多个文件'));
const uploadTitle = computed(() => {
  if (selectedFiles.value.length === 0) {
    return isEdit.value ? '选择新文件' : '选择文件';
  }
  if (selectedFiles.value.length === 1) {
    return selectedFiles.value[0].name;
  }
  return `已选择 ${selectedFiles.value.length} 个文件`;
});
const selectedSummary = computed(() => {
  const totalSize = selectedFiles.value.reduce((sum, file) => sum + file.size, 0);
  return `${selectedFiles.value.length} 个文件 · ${formatSize(totalSize)}`;
});

const rules = computed<FormRules>(() => ({
  title: isEdit.value ? [{ required: true, message: '请输入资料标题', trigger: 'blur' }] : [],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  file: isEdit.value ? [] : [{ required: true, message: '请选择文件', trigger: 'change' }]
}));

const resetForm = () => {
  form.title = props.resource?.title || '';
  form.categoryId = props.resource?.categoryId || props.categories[0]?.categoryId;
  form.description = props.resource?.description || '';
  form.file = '';
  selectedFiles.value = [];
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
  nextTick(() => formRef.value?.clearValidate());
};

const setSelectedFiles = (files?: FileList | File[]) => {
  const nextFiles = Array.from(files || []);
  if (nextFiles.length === 0) {
    return;
  }
  selectedFiles.value = isEdit.value ? nextFiles.slice(0, 1) : nextFiles;
  form.file = selectedFiles.value.map((file) => file.name).join('; ');
  if (!form.title && selectedFiles.value.length === 1) {
    form.title = selectedFiles.value[0].name.replace(/\.[^.]+$/, '');
  }
  formRef.value?.validateField('file');
};

const onFileInputChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  setSelectedFiles(input.files || undefined);
  input.value = '';
};

const onDropFile = (event: DragEvent) => {
  setSelectedFiles(event.dataTransfer?.files || undefined);
};

const removeSelectedFile = (index: number) => {
  selectedFiles.value.splice(index, 1);
  form.file = selectedFiles.value.map((file) => file.name).join('; ');
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
  formRef.value?.validateField('file');
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

const submit = async () => {
  await formRef.value?.validate();
  if (!isEdit.value && selectedFiles.value.length === 0) {
    ElMessage.warning('请选择文件');
    return;
  }
  emit('submit', {
    title: form.title,
    categoryId: form.categoryId,
    description: form.description,
    files: [...selectedFiles.value]
  });
};

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm();
    }
  }
);
</script>

<style scoped>
.full {
  width: 100%;
}

.file-field {
  width: 100%;
  display: grid;
  gap: 12px;
}

.current-file {
  display: grid;
  gap: 4px;
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 8px;
  padding: 10px 12px;
  background: #f5f7fa;
}

.current-file strong {
  color: var(--resource-primary, #245f8f);
  font-size: 12px;
  font-weight: 800;
}

.current-file span {
  overflow: hidden;
  color: var(--resource-muted, #68788c);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.native-upload {
  position: relative;
  display: grid;
  gap: 10px;
}

.native-file-input {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.file-picker {
  position: relative;
  width: 100%;
  min-height: 148px;
  display: grid;
  place-items: center;
  gap: 8px;
  border: 1px dashed var(--resource-input-border, #d3dee8);
  border-radius: 8px;
  padding: 24px 16px;
  background: #f5f7fa;
  color: var(--resource-muted, #68788c);
  text-align: center;
  cursor: pointer;
  overflow: hidden;
}

.file-picker:hover,
.file-picker:focus-within,
.native-upload.selected .file-picker {
  border-color: var(--resource-primary, #245f8f);
  background: var(--resource-primary-soft, #eaf2f8);
}

.native-upload.selected .file-picker {
  min-height: 102px;
  padding: 18px 16px;
}

.file-picker .el-icon {
  color: var(--resource-primary, #245f8f);
  font-size: 42px;
}

.file-picker strong {
  max-width: 100%;
  overflow: hidden;
  color: var(--resource-title, #14243a);
  font-size: 15px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-picker span {
  color: var(--resource-muted, #68788c);
  font-size: 13px;
  font-weight: 700;
}

.selected-files {
  max-height: min(260px, 34vh);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 8px;
  margin: 0;
  overflow: auto;
  padding: 2px;
  list-style: none;
}

.selected-files li {
  min-height: 40px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 8px;
  padding: 8px 9px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(20, 36, 67, 0.04);
}

.selected-files li span {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.selected-files li strong,
.selected-files li em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-files li strong {
  color: var(--resource-title, #14243a);
  font-size: 13px;
  font-weight: 850;
}

.selected-files li em {
  color: var(--resource-weak, #96a1af);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.selected-files li button {
  height: 28px;
  border: 1px solid var(--resource-border, #dce5ed);
  border-radius: 7px;
  padding: 0 9px;
  background: #fff;
  color: var(--resource-text, #32445c);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.selected-files li button:hover {
  border-color: #d93026;
  background: #fff2f1;
  color: #d93026;
}

.dialog-submit,
.dialog-cancel {
  height: 36px;
  border-radius: 8px;
  padding: 0 15px;
  font-weight: 800;
  cursor: pointer;
}

.dialog-submit {
  border: 0;
  background: var(--resource-primary, #245f8f);
  color: #fff;
}

.dialog-cancel {
  border: 1px solid var(--resource-border, #dce5ed);
  background: #fff;
  color: var(--resource-text, #32445c);
}

.dialog-submit:hover {
  background: var(--resource-primary-deep, #183f63);
}

.dialog-cancel:hover {
  border-color: var(--resource-primary, #245f8f);
  color: var(--resource-primary, #245f8f);
  background: var(--resource-primary-soft, #eaf2f8);
}

.dialog-submit:disabled {
  opacity: 0.66;
  cursor: default;
}

.resource-upload-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 12px;
  border-bottom: 1px solid var(--resource-border, #dce5ed);
}

.resource-upload-dialog :deep(.el-dialog__title) {
  color: var(--resource-title, #14243a);
  font-size: 22px;
  font-weight: 850;
}

.resource-upload-dialog :deep(.el-dialog__body) {
  padding: 20px 24px 8px;
}

.resource-upload-dialog :deep(.el-form-item__label) {
  color: var(--resource-text, #32445c);
  font-weight: 700;
}

.resource-upload-dialog :deep(.el-input__wrapper),
.resource-upload-dialog :deep(.el-textarea__inner),
.resource-upload-dialog :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--resource-input-border, #d3dee8) inset;
}

.resource-upload-dialog :deep(.el-textarea__inner) {
  min-height: 112px;
  color: var(--resource-text, #32445c);
  font-weight: 650;
}

.resource-upload-dialog :deep(.el-icon--upload) {
  color: var(--resource-primary, #245f8f);
}
</style>
