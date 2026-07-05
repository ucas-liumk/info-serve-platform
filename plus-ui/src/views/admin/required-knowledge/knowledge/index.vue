<template>
  <main class="rk-admin-page">
    <header class="rk-admin-head">
      <div>
        <p class="rk-admin-kicker">应知应会管理</p>
        <h1>知识点管理</h1>
        <p>按科目维护学习页展示的知识点标题、摘要和正文。</p>
      </div>
      <AdminNav />
    </header>

    <section class="rk-panel">
      <div class="rk-toolbar">
        <el-form ref="queryFormRef" :model="queryParams" inline>
          <el-form-item label="科目" prop="subjectId">
            <el-select v-model="queryParams.subjectId" placeholder="全部科目" clearable style="width: 180px">
              <el-option v-for="subject in subjectOptions" :key="subject.subjectId" :label="subject.subjectName" :value="subject.subjectId" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字" prop="keyword">
            <el-input v-model="queryParams.keyword" placeholder="标题/摘要/正文" clearable style="width: 200px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
              <el-option label="启用" value="0" />
              <el-option label="禁用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button v-hasPermi="['requiredKnowledge:knowledge:add']" type="primary" icon="Plus" @click="handleAdd">新增知识点</el-button>
      </div>

      <el-table v-loading="loading" :data="knowledgeList" border>
        <el-table-column label="知识点" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="rk-name-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.summary || '未填写摘要' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="科目" width="150" show-overflow-tooltip />
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-hasPermi="['requiredKnowledge:knowledge:edit']"
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.updateTime || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120" align="center">
          <template #default="{ row }">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['requiredKnowledge:knowledge:edit']" link type="primary" icon="Edit" @click="handleUpdate(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['requiredKnowledge:knowledge:remove']" link type="primary" icon="Delete" @click="handleDelete(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getKnowledgeList"
      />
    </section>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属科目" prop="subjectId">
          <el-select v-model="form.subjectId" placeholder="请选择科目" style="width: 100%">
            <el-option v-for="subject in subjectOptions" :key="subject.subjectId" :label="subject.subjectName" :value="subject.subjectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="160" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="可用换行分隔学习段落" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup name="RequiredKnowledgeKnowledge" lang="ts">
import AdminNav from '../components/AdminNav.vue';
import {
  addKnowledge,
  changeKnowledgeStatus,
  delKnowledge,
  getKnowledge,
  listKnowledge,
  listSubjectOptions,
  updateKnowledge
} from '@/api/required-knowledge';
import { RkKnowledge, RkKnowledgeForm, RkKnowledgeQuery, RkSubject } from '@/api/required-knowledge/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const total = ref(0);
const knowledgeList = ref<RkKnowledge[]>([]);
const subjectOptions = ref<RkSubject[]>([]);
const queryFormRef = ref<ElFormInstance>();
const formRef = ref<ElFormInstance>();

const initForm: RkKnowledgeForm = {
  subjectId: undefined,
  title: '',
  summary: '',
  content: '',
  orderNum: 0,
  status: '0',
  remark: ''
};

const dialog = reactive<DialogOption>({ visible: false, title: '' });
const data = reactive({
  form: { ...initForm },
  queryParams: { pageNum: 1, pageSize: 10, keyword: '', subjectId: undefined, status: '' } as RkKnowledgeQuery,
  rules: {
    subjectId: [{ required: true, message: '所属科目不能为空', trigger: 'change' }],
    title: [{ required: true, message: '知识点标题不能为空', trigger: 'blur' }],
    content: [{ required: true, message: '知识点正文不能为空', trigger: 'blur' }]
  }
});
const { form, queryParams, rules } = toRefs(data);

const getSubjectOptions = async () => {
  const res = await listSubjectOptions({ status: '0' });
  subjectOptions.value = (res as any).data ?? [];
};

const getKnowledgeList = async () => {
  loading.value = true;
  try {
    const res = await listKnowledge(queryParams.value);
    knowledgeList.value = (res as any).rows ?? [];
    total.value = (res as any).total ?? 0;
  } finally {
    loading.value = false;
  }
};

const reset = () => {
  form.value = { ...initForm, subjectId: queryParams.value.subjectId || subjectOptions.value[0]?.subjectId };
  formRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getKnowledgeList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '新增知识点';
};

const handleUpdate = async (row: RkKnowledge) => {
  reset();
  const res = await getKnowledge(row.knowledgeId);
  Object.assign(form.value, (res as any).data);
  dialog.visible = true;
  dialog.title = '修改知识点';
};

const handleStatusChange = async (row: RkKnowledge) => {
  const text = row.status === '0' ? '启用' : '禁用';
  try {
    await proxy?.$modal.confirm(`确认要${text}知识点"${row.title}"吗？`);
    await changeKnowledgeStatus(row.knowledgeId, row.status || '0');
    proxy?.$modal.msgSuccess(`${text}成功`);
    await getKnowledgeList();
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    form.value.knowledgeId ? await updateKnowledge(form.value) : await addKnowledge(form.value);
    proxy?.$modal.msgSuccess('操作成功');
    dialog.visible = false;
    await getKnowledgeList();
  });
};

const handleDelete = async (row: RkKnowledge) => {
  await proxy?.$modal.confirm(`是否确认删除知识点"${row.title}"？`);
  await delKnowledge(row.knowledgeId);
  proxy?.$modal.msgSuccess('删除成功');
  await getKnowledgeList();
};

onMounted(async () => {
  await getSubjectOptions();
  await getKnowledgeList();
});
</script>

<style scoped>
.rk-admin-page {
  padding: 24px;
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-900);
}

.rk-admin-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.rk-admin-kicker {
  margin: 0 0 4px;
  color: var(--ip-primary-600);
  font-size: var(--ip-font-hint);
  line-height: 1.5;
  font-weight: 700;
}

.rk-admin-head h1 {
  margin: 0;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-headline);
  line-height: 1.25;
  font-weight: 700;
}

.rk-admin-head p {
  margin: 4px 0 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-body);
  line-height: 1.6;
}

.rk-panel {
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.rk-toolbar :deep(.el-form-item) {
  margin-bottom: 0;
}

.rk-name-cell {
  display: grid;
  gap: 2px;
}

.rk-name-cell strong {
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 600;
}

.rk-name-cell span {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

@media (max-width: 900px) {
  .rk-admin-head,
  .rk-toolbar {
    display: grid;
  }
}
</style>
