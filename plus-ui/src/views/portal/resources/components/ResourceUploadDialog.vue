<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="620px" class="resource-upload-dialog" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="160" placeholder="请输入资料标题" />
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
          <el-upload class="full" drag :auto-upload="false" :limit="1" :file-list="fileList" :on-change="onFileChange" :on-remove="onFileRemove">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">{{ isEdit ? '拖入新文件或点击选择，留空则不替换文件' : '拖入文件或点击选择' }}</div>
          </el-upload>
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
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus';
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
  (e: 'submit', payload: { title: string; categoryId: number | string | undefined; description: string; file?: File }): void;
}>();

const formRef = ref<FormInstance>();
const selectedFile = ref<UploadFile>();
const fileList = ref<UploadUserFile[]>([]);
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

const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: '请输入资料标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  file: isEdit.value ? [] : [{ required: true, message: '请选择文件', trigger: 'change' }]
}));

const resetForm = () => {
  form.title = props.resource?.title || '';
  form.categoryId = props.resource?.categoryId || props.categories[0]?.categoryId;
  form.description = props.resource?.description || '';
  form.file = '';
  selectedFile.value = undefined;
  fileList.value = [];
  nextTick(() => formRef.value?.clearValidate());
};

const onFileChange = (file: UploadFile) => {
  selectedFile.value = file;
  fileList.value = [file];
  form.file = file.name;
  if (!form.title) {
    form.title = file.name.replace(/\.[^.]+$/, '');
  }
  formRef.value?.validateField('file');
};

const onFileRemove = () => {
  selectedFile.value = undefined;
  fileList.value = [];
  form.file = '';
};

const submit = async () => {
  await formRef.value?.validate();
  if (!isEdit.value && !selectedFile.value?.raw) {
    ElMessage.warning('请选择文件');
    return;
  }
  emit('submit', {
    title: form.title,
    categoryId: form.categoryId,
    description: form.description,
    file: selectedFile.value?.raw
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
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 10px 12px;
  background: #f7faff;
}

.current-file strong {
  color: #1260e8;
  font-size: 12px;
  font-weight: 800;
}

.current-file span {
  overflow: hidden;
  color: #53668f;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  background: #1260e8;
  color: #fff;
}

.dialog-cancel {
  border: 1px solid #e1e9f6;
  background: #fff;
  color: #25395f;
}

.dialog-submit:hover {
  background: #0f55cf;
}

.dialog-cancel:hover {
  border-color: #1260e8;
  color: #1260e8;
  background: #edf4ff;
}

.dialog-submit:disabled {
  opacity: 0.66;
  cursor: default;
}

.resource-upload-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 12px;
  border-bottom: 1px solid #e1e9f6;
}

.resource-upload-dialog :deep(.el-dialog__title) {
  color: #0b1833;
  font-size: 22px;
  font-weight: 850;
}

.resource-upload-dialog :deep(.el-dialog__body) {
  padding: 20px 24px 8px;
}

.resource-upload-dialog :deep(.el-form-item__label) {
  color: #25395f;
  font-weight: 700;
}

.resource-upload-dialog :deep(.el-input__wrapper),
.resource-upload-dialog :deep(.el-textarea__inner),
.resource-upload-dialog :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe5f4 inset;
}

.resource-upload-dialog :deep(.el-textarea__inner) {
  min-height: 112px;
  color: #25395f;
  font-weight: 650;
}

.resource-upload-dialog :deep(.el-upload-dragger) {
  border-color: #dbe5f4;
  border-radius: 8px;
  background: #f7faff;
}

.resource-upload-dialog :deep(.el-upload-dragger:hover) {
  border-color: #1260e8;
}

.resource-upload-dialog :deep(.el-icon--upload) {
  color: #1260e8;
}
</style>
