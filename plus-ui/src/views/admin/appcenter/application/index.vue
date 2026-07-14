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
                <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
              </el-select>
            </el-form-item>
            <el-form-item label="应用类型" prop="appType">
              <el-select v-model="queryParams.appType" placeholder="请选择类型" clearable style="width: 140px">
                <el-option label="自研应用" value="business" />
                <el-option label="开源应用" value="online" />
                <el-option label="离线应用" value="offline" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
                <el-option label="上架" value="0" />
                <el-option label="下架" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="开放范围" prop="accessMode">
              <el-select v-model="queryParams.accessMode" placeholder="请选择范围" clearable style="width: 130px">
                <el-option label="全部用户" value="all" />
                <el-option label="指定角色" value="role" />
                <el-option label="指定用户" value="user" />
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
            <el-button v-hasPermi="['appcenter:application:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()"
              >修改</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['appcenter:application:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
              >删除</el-button
            >
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
        <el-table-column label="类型" align="center" prop="appType" width="100">
          <template #default="scope">
            <el-tag :type="getAppTypeTag(scope.row.appType)">{{ getAppTypeLabel(scope.row.appType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="安装包" align="center" prop="packageName" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.packageName || '-' }}</span>
          </template>
        </el-table-column>
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
        <el-table-column label="开放范围" align="center" prop="accessMode" width="100">
          <template #default="scope">
            <el-tag :type="getAccessModeTag(scope.row.accessMode)">{{ getAccessModeLabel(scope.row.accessMode) }}</el-tag>
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

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <!-- 添加或修改应用对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="760px" append-to-body>
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
                <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="应用类型" prop="appType">
              <el-radio-group v-model="form.appType">
                <el-radio value="business">自研应用</el-radio>
                <el-radio value="online">开源应用</el-radio>
                <el-radio value="offline">离线应用</el-radio>
              </el-radio-group>
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
          <el-col v-if="!isOfflineForm" :span="24">
            <el-form-item label="访问地址" prop="accessUrl">
              <el-input v-model="form.accessUrl" placeholder="请输入 http(s):// 外部地址或 / 开头的站内路径" />
            </el-form-item>
          </el-col>
          <el-col v-if="isOfflineForm" :span="24">
            <el-form-item label="离线安装包" prop="packageOssId">
              <div class="package-file">
                <el-upload action="#" :show-file-list="false" :http-request="handlePackageUpload">
                  <el-button v-hasPermi="['appcenter:application:add', 'appcenter:application:edit']" type="primary" plain icon="Upload">
                    上传安装包
                  </el-button>
                </el-upload>
                <span class="package-file__name">{{ form.packageName || '未上传安装包' }}</span>
                <span v-if="form.packageSize" class="package-file__size">{{ formatFileSize(form.packageSize) }}</span>
              </div>
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
          <el-col :span="24">
            <el-form-item label="开放范围" prop="accessMode">
              <el-radio-group v-model="form.accessMode" @change="handleAccessModeChange">
                <el-radio value="all">全部用户</el-radio>
                <el-radio value="role">指定角色</el-radio>
                <el-radio value="user">指定用户</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.accessMode === 'role'" :span="24">
            <el-form-item label="开放角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple filterable placeholder="请选择可访问该应用的角色" style="width: 100%">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.accessMode === 'user'" :span="24">
            <el-form-item label="开放用户" prop="userIds">
              <el-select v-model="form.userIds" multiple filterable placeholder="请选择可访问该应用的用户" style="width: 100%">
                <el-option
                  v-for="item in userOptions"
                  :key="item.userId"
                  :label="`${item.nickName || item.userName}（${item.userName}）`"
                  :value="item.userId"
                />
              </el-select>
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
  uploadApplicationPackage,
  delApplication
} from '@/api/appcenter/application/index';
import { AppApplicationVo, AppApplicationForm, AppApplicationQuery } from '@/api/appcenter/application/types';
import { listCategory } from '@/api/appcenter/category/index';
import { AppCategoryVo } from '@/api/appcenter/category/types';
import { listRole } from '@/api/system/role/index';
import { RoleVO } from '@/api/system/role/types';
import { listUser } from '@/api/system/user/index';
import { UserVO } from '@/api/system/user/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const appList = ref<AppApplicationVo[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const categoryOptions = ref<AppCategoryVo[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const userOptions = ref<UserVO[]>([]);

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
  appType: 'online',
  packageOssId: undefined,
  packageName: '',
  packageSize: undefined,
  packageUrl: '',
  isSecurity: '0',
  status: '0',
  accessMode: 'all',
  roleIds: [],
  userIds: [],
  orderNum: 0,
  remark: ''
};

const urlPattern = /^(https?:\/\/.+|\/(?!\/).*)$/;

const data = reactive<PageData<AppApplicationForm, AppApplicationQuery>>({
  form: { ...initFormData, roleIds: [], userIds: [] },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryId: undefined,
    appType: '',
    status: '',
    isSecurity: '',
    accessMode: ''
  },
  rules: {
    appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }],
    appCode: [{ required: true, message: '应用编码不能为空', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
    appType: [{ required: true, message: '请选择应用类型', trigger: 'change' }],
    accessMode: [{ required: true, message: '请选择开放范围', trigger: 'change' }],
    roleIds: [{ validator: validateRoleScope, trigger: 'change' }],
    userIds: [{ validator: validateUserScope, trigger: 'change' }],
    accessUrl: [{ validator: validateAccessUrl, trigger: 'blur' }],
    packageOssId: [{ validator: validatePackage, trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs<PageData<AppApplicationForm, AppApplicationQuery>>(data);
const isOfflineForm = computed(() => form.value.appType === 'offline');

const getCategoryName = (categoryId: number | string | undefined) => {
  if (!categoryId) return '-';
  const found = categoryOptions.value.find((c) => String(c.categoryId) === String(categoryId));
  return found ? found.categoryName : '-';
};

function validateAccessUrl(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (form.value.appType === 'offline') {
    callback();
    return;
  }
  if (!value) {
    callback(new Error('访问地址不能为空'));
    return;
  }
  if (!urlPattern.test(value)) {
    callback(new Error('请输入 http(s):// 外部地址或 / 开头的站内路径'));
    return;
  }
  callback();
}

function validatePackage(_rule: unknown, value: number | string | undefined, callback: (error?: Error) => void) {
  if (form.value.appType === 'offline' && !value) {
    callback(new Error('请先上传离线安装包'));
    return;
  }
  callback();
}

function validateRoleScope(_rule: unknown, value: Array<number | string>, callback: (error?: Error) => void) {
  if (form.value.accessMode === 'role' && (!value || !value.length)) {
    callback(new Error('请选择开放角色'));
    return;
  }
  callback();
}

function validateUserScope(_rule: unknown, value: Array<number | string>, callback: (error?: Error) => void) {
  if (form.value.accessMode === 'user' && (!value || !value.length)) {
    callback(new Error('请选择开放用户'));
    return;
  }
  callback();
}

const getAppTypeLabel = (appType?: string) => {
  if (appType === 'business') return '自研';
  if (appType === 'offline') return '离线';
  return '开源';
};

const getAppTypeTag = (appType?: string) => {
  if (appType === 'business') return 'success';
  if (appType === 'offline') return 'warning';
  return 'primary';
};

const getAccessModeLabel = (accessMode?: string) => {
  if (accessMode === 'role') return '指定角色';
  if (accessMode === 'user') return '指定用户';
  return '全部用户';
};

const getAccessModeTag = (accessMode?: string) => {
  if (accessMode === 'role') return 'warning';
  if (accessMode === 'user') return 'danger';
  return 'success';
};

const loadCategories = async () => {
  const res = await listCategory();
  categoryOptions.value = (res as any).data ?? (res as any).rows ?? [];
};

const loadAccessOptions = async () => {
  const [roleRes, userRes] = await Promise.all([
    listRole({ pageNum: 1, pageSize: 100, roleName: '', roleKey: '', status: '0' }),
    listUser({ pageNum: 1, pageSize: 100, status: '0' })
  ]);
  roleOptions.value = (roleRes as any).rows ?? [];
  userOptions.value = (userRes as any).rows ?? [];
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
  form.value = { ...initFormData, roleIds: [], userIds: [] };
  appFormRef.value?.resetFields();
};

const handleAccessModeChange = () => {
  if (form.value.accessMode !== 'role') {
    form.value.roleIds = [];
  }
  if (form.value.accessMode !== 'user') {
    form.value.userIds = [];
  }
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
  form.value.appType = form.value.appType || 'online';
  form.value.accessMode = form.value.accessMode || 'all';
  form.value.roleIds = form.value.roleIds || [];
  form.value.userIds = form.value.userIds || [];
  dialog.visible = true;
  dialog.title = '修改应用';
};

const handlePackageUpload = async (options: any) => {
  const uploadForm = new FormData();
  uploadForm.append('file', options.file);
  const res = await uploadApplicationPackage(uploadForm);
  const uploaded = (res as any).data;
  form.value.packageOssId = uploaded.ossId;
  form.value.packageName = uploaded.fileName || options.file.name;
  form.value.packageSize = options.file.size;
  form.value.packageUrl = uploaded.url;
  appFormRef.value?.validateField('packageOssId');
  proxy?.$modal.msgSuccess('上传成功');
};

const formatFileSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
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
  loadAccessOptions();
  getList();
});
</script>

<style scoped lang="scss">
.package-file {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;

  &__name {
    min-width: 0;
    max-width: 320px;
    overflow: hidden;
    color: #606266;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__size {
    color: #909399;
    font-size: 12px;
  }
}
</style>
