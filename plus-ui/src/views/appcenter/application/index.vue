<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="关键字" prop="keyword">
              <el-input v-model="queryParams.keyword" placeholder="名称/编码" clearable style="width: 160px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 150px">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.categoryId"
                  :label="item.categoryName"
                  :value="item.categoryId"
                />
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
            <el-button v-hasPermi="['appcenter:application:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['appcenter:application:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['appcenter:application:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="appList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="应用ID" align="center" prop="appId" />
        <el-table-column label="名称" align="center" prop="appName" min-width="120" show-overflow-tooltip />
        <el-table-column label="编码" align="center" prop="appCode" min-width="120" show-overflow-tooltip />
        <el-table-column label="分类" align="center" prop="categoryId" width="120">
          <template #default="scope">
            <span>{{ getCategoryName(scope.row.categoryId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" align="center" prop="version" width="90" />
        <el-table-column label="状态" align="center" prop="status" width="90">
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
        <el-table-column label="使用数" align="center" prop="useCount" width="80" />
        <el-table-column label="推荐数" align="center" prop="recommendCount" width="80" />
        <el-table-column label="排序" align="center" prop="orderNum" width="70" />
        <el-table-column label="操作" fixed="right" align="center" width="120" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['appcenter:application:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['appcenter:application:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 添加或修改应用对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="620px" append-to-body>
      <el-form ref="appFormRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="应用名称" prop="appName">
              <el-input v-model="form.appName" placeholder="请输入应用名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用编码" prop="appCode">
              <el-input v-model="form.appCode" placeholder="请输入应用编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" placeholder="如：1.0.0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.categoryId"
                  :label="item.categoryName"
                  :value="item.categoryId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="图标名称或URL" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主题色" prop="accent">
              <el-input v-model="form.accent" placeholder="如：#1890ff" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="访问地址" prop="accessUrl">
              <el-input v-model="form.accessUrl" placeholder="请输入 http:// 或 https:// 开头的地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签" prop="tags">
              <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入应用描述" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全应用" prop="isSecurity">
              <el-radio-group v-model="form.isSecurity">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
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
          <el-col :span="12">
            <el-form-item label="排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
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

<script setup name="AppApplication" lang="ts">
import {
  listApplication,
  getApplication,
  addApplication,
  updateApplication,
  changeApplicationStatus,
  delApplication
} from '@/api/appcenter/application/index';
import { AppApplicationVo, AppApplicationForm, AppApplicationQuery } from '@/api/appcenter/application/types';
import { listCategory } from '@/api/appcenter/category/index';
import { AppCategoryVo } from '@/api/appcenter/category/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const appList = ref<AppApplicationVo[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const categoryOptions = ref<AppCategoryVo[]>([]);

const appFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: AppApplicationForm = {
  appId: undefined,
  appName: '',
  appCode: '',
  version: '',
  categoryId: undefined,
  icon: '',
  accent: '',
  description: '',
  tags: '',
  accessUrl: '',
  isSecurity: '0',
  status: '0',
  orderNum: 0,
  remark: ''
};

const urlPattern = /^https?:\/\/.+/;

const data = reactive<PageData<AppApplicationForm, AppApplicationQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryId: undefined,
    status: '',
    isSecurity: ''
  },
  rules: {
    appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }],
    appCode: [{ required: true, message: '应用编码不能为空', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
    accessUrl: [
      { required: true, message: '访问地址不能为空', trigger: 'blur' },
      { pattern: urlPattern, message: '请输入以 http:// 或 https:// 开头的地址', trigger: 'blur' }
    ]
  }
});

const { queryParams, form, rules } = toRefs<PageData<AppApplicationForm, AppApplicationQuery>>(data);

const getCategoryName = (categoryId: number | string | undefined) => {
  if (!categoryId) return '-';
  const found = categoryOptions.value.find((c) => c.categoryId === categoryId);
  return found ? found.categoryName : '-';
};

const loadCategories = async () => {
  const res = await listCategory();
  categoryOptions.value = (res as any).data ?? (res as any).rows ?? [];
};

const getList = async () => {
  loading.value = true;
  const res = await listApplication(queryParams.value);
  appList.value = (res as any).rows ?? [];
  total.value = (res as any).total ?? 0;
  loading.value = false;
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const reset = () => {
  form.value = { ...initFormData };
  appFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  queryParams.value.pageNum = 1;
  handleQuery();
};

const handleSelectionChange = (selection: AppApplicationVo[]) => {
  ids.value = selection.map((item) => item.appId);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加应用';
};

const handleUpdate = async (row?: AppApplicationVo) => {
  reset();
  const appId = row?.appId || ids.value[0];
  const res = await getApplication(appId);
  Object.assign(form.value, (res as any).data);
  dialog.visible = true;
  dialog.title = '修改应用';
};

const handleStatusChange = async (row: AppApplicationVo) => {
  const text = row.status === '0' ? '上架' : '下架';
  try {
    await proxy?.$modal.confirm('确认要将应用"' + row.appName + '"' + text + '吗？');
    await changeApplicationStatus(row.appId, row.status);
    proxy?.$modal.msgSuccess(text + '成功');
  } catch {
    // 用户取消，恢复原状态
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitForm = () => {
  appFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.appId ? await updateApplication(form.value) : await addApplication(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

const handleDelete = async (row?: AppApplicationVo) => {
  const appIds = row?.appId || ids.value;
  await proxy?.$modal.confirm('是否确认删除应用编号为"' + appIds + '"的数据项？');
  await delApplication(appIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  loadCategories();
  getList();
});
</script>
