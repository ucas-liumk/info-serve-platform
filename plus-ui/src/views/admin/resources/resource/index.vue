<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="关键字" prop="keyword">
              <el-input v-model="queryParams.keyword" placeholder="标题/文件名" clearable style="width: 180px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 150px">
                <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
                <el-option label="上架" value="0" />
                <el-option label="下架" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:resource:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:resource:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()"
              >修改</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:resource:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
              >删除</el-button
            >
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="resourceList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="标题" align="center" prop="title" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" align="center" prop="categoryName" width="120" show-overflow-tooltip />
        <el-table-column label="文件名" align="center" prop="originalName" min-width="160" show-overflow-tooltip />
        <el-table-column label="大小" align="center" prop="fileSize" width="100">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="下载" align="center" prop="downloadCount" width="80" />
        <el-table-column label="浏览" align="center" prop="viewCount" width="80" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              :active-text="scope.row.status === '0' ? '上架' : ''"
              :inactive-text="scope.row.status === '1' ? '下架' : ''"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" fixed="right" align="center" width="130" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['infoservice:resource:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['infoservice:resource:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="680px" append-to-body>
      <el-form ref="resourceFormRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资料标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入资料标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资料分类" prop="categoryIds">
              <el-select v-model="form.categoryIds" multiple placeholder="请选择资料分类（可多选）" style="width: 100%">
                <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资料文件" prop="ossId">
              <div class="resource-file">
                <el-upload action="#" :show-file-list="false" :http-request="handleUpload">
                  <el-button v-hasPermi="['infoservice:resource:upload']" type="primary" plain icon="Upload">上传文件</el-button>
                </el-upload>
                <span class="resource-file__name">{{ form.originalName || '未选择文件' }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资料简介" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入资料简介" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预览类型" prop="previewType">
              <el-input v-model="form.previewType" placeholder="如 pdf/image/text" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio value="0">上架</el-radio>
                <el-radio value="1">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="InfoResource" lang="ts">
import {
  addResource,
  changeResourceStatus,
  delResource,
  getResourceAdmin,
  listResource,
  listResourceCategoryOptions,
  updateResource,
  uploadAdminResourceFile
} from '@/api/infoservice/admin';
import { InfoResource, InfoResourceForm, InfoResourceQuery, ResourceCategory } from '@/api/infoservice/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const resourceList = ref<InfoResource[]>([]);
const categoryOptions = ref<ResourceCategory[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const resourceFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: InfoResourceForm = {
  resourceId: undefined,
  title: '',
  description: '',
  categoryId: undefined,
  categoryIds: [],
  ossId: undefined,
  originalName: '',
  fileSuffix: '',
  mimeType: '',
  fileSize: undefined,
  previewType: '',
  status: '0',
  remark: ''
};

const data = reactive<PageData<InfoResourceForm, InfoResourceQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryId: undefined,
    status: ''
  },
  rules: {
    title: [{ required: true, message: '资料标题不能为空', trigger: 'blur' }],
    categoryIds: [
      {
        required: true,
        validator: (_rule: unknown, value: Array<number | string>, callback: (error?: Error) => void) =>
          value && value.length > 0 ? callback() : callback(new Error('请至少选择一个资料分类')),
        trigger: 'change'
      }
    ],
    ossId: [{ required: true, message: '请先上传资料文件', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs<PageData<InfoResourceForm, InfoResourceQuery>>(data);

const loadCategories = async () => {
  const res = await listResourceCategoryOptions({ status: '0' });
  categoryOptions.value = (res as any).data ?? (res as any).rows ?? [];
};

const getList = async () => {
  loading.value = true;
  const res = await listResource(queryParams.value);
  resourceList.value = (res as any).rows ?? [];
  total.value = (res as any).total ?? 0;
  loading.value = false;
};

const reset = () => {
  form.value = { ...initFormData };
  resourceFormRef.value?.resetFields();
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleSelectionChange = (selection: InfoResource[]) => {
  ids.value = selection.map((item) => item.resourceId);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加资料';
};

const handleUpdate = async (row?: InfoResource) => {
  reset();
  const resourceId = row?.resourceId || ids.value[0];
  const res = await getResourceAdmin(resourceId);
  Object.assign(form.value, (res as any).data);
  dialog.visible = true;
  dialog.title = '修改资料';
};

const handleUpload = async (options: any) => {
  const uploadForm = new FormData();
  uploadForm.append('file', options.file);
  const res = await uploadAdminResourceFile(uploadForm);
  const fileInfo = (res as any).data;
  Object.assign(form.value, fileInfo);
  proxy?.$modal.msgSuccess('上传成功');
};

const handleStatusChange = async (row: InfoResource) => {
  const text = row.status === '0' ? '上架' : '下架';
  try {
    await proxy?.$modal.confirm(`确认要${text}资料"${row.title}"吗？`);
    await changeResourceStatus(row.resourceId, row.status);
    proxy?.$modal.msgSuccess(`${text}成功`);
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitForm = () => {
  resourceFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      // categoryId=主分类（首个），满足后端 Bo 校验与旧展示路径；categoryIds 为全量
      form.value.categoryId = form.value.categoryIds?.[0];
      form.value.resourceId ? await updateResource(form.value) : await addResource(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

const handleDelete = async (row?: InfoResource) => {
  const resourceIds = row?.resourceId || ids.value;
  await proxy?.$modal.confirm(`是否确认删除资料编号为"${resourceIds}"的数据项？`);
  await delResource(resourceIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

const formatFileSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

onMounted(() => {
  loadCategories();
  getList();
});
</script>

<style scoped lang="scss">
.resource-file {
  display: flex;
  align-items: center;
  gap: 12px;

  &__name {
    color: #606266;
  }
}
</style>
