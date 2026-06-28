<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="关键字" prop="keyword">
              <el-input v-model="queryParams.keyword" placeholder="应用/内容/提交人" clearable style="width: 180px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="类型" prop="demandType">
              <el-select v-model="queryParams.demandType" placeholder="请选择类型" clearable style="width: 150px">
                <el-option label="希望上架应用" value="new_app" />
                <el-option label="现有应用建议" value="suggestion" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
                <el-option label="待处理" value="0" />
                <el-option label="处理中" value="1" />
                <el-option label="已处理" value="2" />
                <el-option label="已关闭" value="3" />
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
            <el-button v-hasPermi="['appcenter:demand:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()">
              处理
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['appcenter:demand:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">
              删除
            </el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="demandList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="类型" align="center" prop="demandType" width="130">
          <template #default="scope">
            <el-tag :type="getDemandTypeTag(scope.row.demandType)">{{ getDemandTypeText(scope.row.demandType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应用名称" align="center" prop="appName" min-width="150" show-overflow-tooltip />
        <el-table-column label="需求说明" align="left" prop="content" min-width="260" show-overflow-tooltip />
        <el-table-column label="提交人" align="center" prop="requesterName" width="120">
          <template #default="scope">
            <span>{{ scope.row.requesterName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" align="center" prop="contact" width="150" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.contact || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTag(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" fixed="right" align="center" width="120" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="处理" placement="top">
              <el-button v-hasPermi="['appcenter:demand:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['appcenter:demand:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="720px" append-to-body>
      <el-descriptions v-if="currentDemand" :column="2" border class="demand-desc">
        <el-descriptions-item label="需求类型">
          <el-tag :type="getDemandTypeTag(currentDemand.demandType)">{{ getDemandTypeText(currentDemand.demandType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="getStatusTag(currentDemand.status)">{{ getStatusText(currentDemand.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="应用名称">{{ currentDemand.appName }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ currentDemand.requesterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ currentDemand.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentDemand.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求说明" :span="2">
          <div class="demand-content">{{ currentDemand.content }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">处理结果</el-divider>
      <el-form ref="demandFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="处理状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="1">处理中</el-radio>
            <el-radio value="2">已处理</el-radio>
            <el-radio value="3">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input
            v-model="form.handleRemark"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="请输入处理说明，方便后续追踪"
          />
        </el-form-item>
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

<script setup name="AppDemand" lang="ts">
import { delDemand, getDemand, handleDemand, listDemand } from '@/api/appcenter/demand';
import { AppDemandForm, AppDemandQuery, AppDemandVo } from '@/api/appcenter/demand/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const demandList = ref<AppDemandVo[]>([]);
const currentDemand = ref<AppDemandVo>();
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const demandFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: AppDemandForm = {
  demandId: undefined,
  status: '2',
  handleRemark: ''
};

const data = reactive<PageData<AppDemandForm, AppDemandQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    demandType: '',
    status: ''
  },
  rules: {
    status: [{ required: true, message: '请选择处理状态', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs<PageData<AppDemandForm, AppDemandQuery>>(data);

const getDemandTypeText = (type: string) => {
  if (type === 'new_app') return '希望上架应用';
  if (type === 'suggestion') return '现有应用建议';
  return type || '-';
};

const getDemandTypeTag = (type: string) => {
  if (type === 'new_app') return 'success';
  if (type === 'suggestion') return 'warning';
  return 'info';
};

const getStatusText = (status: string) => {
  if (status === '0') return '待处理';
  if (status === '1') return '处理中';
  if (status === '2') return '已处理';
  if (status === '3') return '已关闭';
  return status || '-';
};

const getStatusTag = (status: string) => {
  if (status === '0') return 'warning';
  if (status === '1') return 'primary';
  if (status === '2') return 'success';
  if (status === '3') return 'info';
  return 'info';
};

const getList = async () => {
  loading.value = true;
  const res = await listDemand(queryParams.value);
  demandList.value = (res as any).rows ?? [];
  total.value = (res as any).total ?? 0;
  loading.value = false;
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const reset = () => {
  currentDemand.value = undefined;
  form.value = { ...initFormData };
  demandFormRef.value?.resetFields();
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

const handleSelectionChange = (selection: AppDemandVo[]) => {
  ids.value = selection.map((item) => item.demandId);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleUpdate = async (row?: AppDemandVo) => {
  reset();
  const demandId = row?.demandId || ids.value[0];
  const res = await getDemand(demandId);
  currentDemand.value = (res as any).data;
  form.value.demandId = currentDemand.value?.demandId;
  form.value.status = currentDemand.value?.status === '1' || currentDemand.value?.status === '3' ? currentDemand.value.status : '2';
  form.value.handleRemark = currentDemand.value?.handleRemark || '';
  dialog.visible = true;
  dialog.title = '处理需求反馈';
};

const submitForm = () => {
  demandFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await handleDemand(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

const handleDelete = async (row?: AppDemandVo) => {
  const demandIds = row?.demandId || ids.value;
  await proxy?.$modal.confirm('是否确认删除需求反馈编号为"' + demandIds + '"的数据项？');
  await delDemand(demandIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.demand-desc {
  margin-bottom: 12px;
}

.demand-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
