<template>
  <main class="rk-admin-page">
    <header class="rk-admin-head">
      <div>
        <p class="rk-admin-kicker">应知应会管理</p>
        <h1>栏目科目</h1>
      </div>
      <AdminNav />
    </header>

    <section class="rk-panel">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="科目配置" name="subjects">
          <div class="rk-toolbar">
            <el-form ref="subjectQueryFormRef" :model="subjectQuery" inline>
              <el-form-item label="关键字" prop="keyword">
                <el-input v-model="subjectQuery.keyword" placeholder="名称/编码" clearable style="width: 180px" @keyup.enter="handleSubjectQuery" />
              </el-form-item>
              <el-form-item label="栏目" prop="groupId">
                <el-select v-model="subjectQuery.groupId" placeholder="全部栏目" clearable style="width: 160px">
                  <el-option v-for="group in groupOptions" :key="group.groupId" :label="group.groupName" :value="group.groupId" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="subjectQuery.status" placeholder="全部状态" clearable style="width: 120px">
                  <el-option label="启用" value="0" />
                  <el-option label="禁用" value="1" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="handleSubjectQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetSubjectQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-button v-hasPermi="['requiredKnowledge:subject:add']" type="primary" icon="Plus" @click="handleSubjectAdd">新增科目</el-button>
          </div>

          <el-table v-loading="subjectLoading" :data="subjectList" border>
            <el-table-column label="科目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="rk-name-cell">
                  <strong>{{ row.subjectName }}</strong>
                  <span>{{ row.subjectCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="groupName" label="栏目" width="150" show-overflow-tooltip />
            <el-table-column prop="knowledgeCount" label="知识点" width="90" align="center" />
            <el-table-column prop="questionCount" label="题目" width="90" align="center" />
            <el-table-column prop="examCount" label="考试" width="90" align="center" />
            <el-table-column prop="orderNum" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  v-hasPermi="['requiredKnowledge:subject:edit']"
                  v-model="row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleSubjectStatusChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="120" align="center">
              <template #default="{ row }">
                <el-tooltip content="修改" placement="top">
                  <el-button v-hasPermi="['requiredKnowledge:subject:edit']" link type="primary" icon="Edit" @click="handleSubjectUpdate(row)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button v-hasPermi="['requiredKnowledge:subject:remove']" link type="primary" icon="Delete" @click="handleSubjectDelete(row)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="subjectTotal > 0"
            v-model:page="subjectQuery.pageNum"
            v-model:limit="subjectQuery.pageSize"
            :total="subjectTotal"
            @pagination="getSubjectList"
          />
        </el-tab-pane>

        <el-tab-pane label="栏目配置" name="groups">
          <div class="rk-toolbar">
            <el-form ref="groupQueryFormRef" :model="groupQuery" inline>
              <el-form-item label="关键字" prop="keyword">
                <el-input v-model="groupQuery.keyword" placeholder="名称/编码" clearable style="width: 180px" @keyup.enter="handleGroupQuery" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="groupQuery.status" placeholder="全部状态" clearable style="width: 120px">
                  <el-option label="启用" value="0" />
                  <el-option label="禁用" value="1" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="handleGroupQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetGroupQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-button v-hasPermi="['requiredKnowledge:group:add']" type="primary" icon="Plus" @click="handleGroupAdd">新增栏目</el-button>
          </div>

          <el-table v-loading="groupLoading" :data="groupList" border>
            <el-table-column label="栏目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="rk-name-cell">
                  <strong>{{ row.groupName }}</strong>
                  <span>{{ row.groupCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="subjectCount" label="科目数" width="90" align="center" />
            <el-table-column prop="orderNum" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  v-hasPermi="['requiredKnowledge:group:edit']"
                  v-model="row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleGroupStatusChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="120" align="center">
              <template #default="{ row }">
                <el-tooltip content="修改" placement="top">
                  <el-button v-hasPermi="['requiredKnowledge:group:edit']" link type="primary" icon="Edit" @click="handleGroupUpdate(row)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button v-hasPermi="['requiredKnowledge:group:remove']" link type="primary" icon="Delete" @click="handleGroupDelete(row)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="groupTotal > 0"
            v-model:page="groupQuery.pageNum"
            v-model:limit="groupQuery.pageSize"
            :total="groupTotal"
            @pagination="getGroupList"
          />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="groupDialog.visible" :title="groupDialog.title" width="520px" append-to-body>
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="90px">
        <el-form-item label="栏目名称" prop="groupName"><el-input v-model="groupForm.groupName" /></el-form-item>
        <el-form-item label="栏目编码" prop="groupCode"><el-input v-model="groupForm.groupCode" /></el-form-item>
        <el-form-item label="说明" prop="description"><el-input v-model="groupForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="排序" prop="orderNum"><el-input-number v-model="groupForm.orderNum" controls-position="right" :min="0" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="groupForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="groupForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitGroupForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="subjectDialog.visible" :title="subjectDialog.title" width="560px" append-to-body>
      <el-form ref="subjectFormRef" :model="subjectForm" :rules="subjectRules" label-width="90px">
        <el-form-item label="所属栏目" prop="groupId">
          <el-select v-model="subjectForm.groupId" style="width: 100%">
            <el-option v-for="group in groupOptions" :key="group.groupId" :label="group.groupName" :value="group.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="科目名称" prop="subjectName"><el-input v-model="subjectForm.subjectName" /></el-form-item>
        <el-form-item label="科目编码" prop="subjectCode"><el-input v-model="subjectForm.subjectCode" /></el-form-item>
        <el-form-item label="说明" prop="description"><el-input v-model="subjectForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="图标" prop="icon"><el-input v-model="subjectForm.icon" maxlength="8" /></el-form-item>
        <el-form-item label="统计">
          <div class="rk-number-row">
            <el-input-number v-model="subjectForm.knowledgeCount" :min="0" controls-position="right" />
            <el-input-number v-model="subjectForm.questionCount" :min="0" controls-position="right" />
            <el-input-number v-model="subjectForm.examCount" :min="0" controls-position="right" />
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="orderNum"
          ><el-input-number v-model="subjectForm.orderNum" controls-position="right" :min="0"
        /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="subjectForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="subjectForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subjectDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitSubjectForm">确定</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup name="RequiredKnowledgeSubjects" lang="ts">
import AdminNav from '../components/AdminNav.vue';
import {
  addSubject,
  addSubjectGroup,
  changeSubjectGroupStatus,
  changeSubjectStatus,
  delSubject,
  delSubjectGroup,
  getSubject,
  getSubjectGroup,
  listSubject,
  listSubjectGroup,
  listSubjectGroupOptions,
  updateSubject,
  updateSubjectGroup
} from '@/api/required-knowledge';
import { RkSubject, RkSubjectForm, RkSubjectGroup, RkSubjectGroupForm, RkSubjectGroupQuery, RkSubjectQuery } from '@/api/required-knowledge/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const activeTab = ref<'subjects' | 'groups'>('subjects');
const groupList = ref<RkSubjectGroup[]>([]);
const groupOptions = ref<RkSubjectGroup[]>([]);
const subjectList = ref<RkSubject[]>([]);
const groupLoading = ref(false);
const subjectLoading = ref(false);
const groupTotal = ref(0);
const subjectTotal = ref(0);
const groupFormRef = ref<ElFormInstance>();
const subjectFormRef = ref<ElFormInstance>();
const groupQueryFormRef = ref<ElFormInstance>();
const subjectQueryFormRef = ref<ElFormInstance>();

const groupInitForm: RkSubjectGroupForm = { groupName: '', groupCode: '', description: '', orderNum: 0, status: '0', remark: '' };
const subjectInitForm: RkSubjectForm = {
  groupId: undefined,
  subjectName: '',
  subjectCode: '',
  description: '',
  icon: '',
  knowledgeCount: 0,
  questionCount: 0,
  examCount: 0,
  orderNum: 0,
  status: '0',
  remark: ''
};

const groupDialog = reactive<DialogOption>({ visible: false, title: '' });
const subjectDialog = reactive<DialogOption>({ visible: false, title: '' });
const data = reactive({
  groupForm: { ...groupInitForm },
  subjectForm: { ...subjectInitForm },
  groupQuery: { pageNum: 1, pageSize: 10, keyword: '', status: '' } as RkSubjectGroupQuery,
  subjectQuery: { pageNum: 1, pageSize: 10, keyword: '', groupId: undefined, status: '' } as RkSubjectQuery,
  groupRules: {
    groupName: [{ required: true, message: '栏目名称不能为空', trigger: 'blur' }],
    groupCode: [{ required: true, message: '栏目编码不能为空', trigger: 'blur' }]
  },
  subjectRules: {
    groupId: [{ required: true, message: '所属栏目不能为空', trigger: 'change' }],
    subjectName: [{ required: true, message: '科目名称不能为空', trigger: 'blur' }],
    subjectCode: [{ required: true, message: '科目编码不能为空', trigger: 'blur' }]
  }
});
const { groupForm, subjectForm, groupQuery, subjectQuery, groupRules, subjectRules } = toRefs(data);

const getGroupOptions = async () => {
  const res = await listSubjectGroupOptions({ status: '0' });
  groupOptions.value = (res as any).data ?? [];
};

const getGroupList = async () => {
  groupLoading.value = true;
  const res = await listSubjectGroup(groupQuery.value);
  groupList.value = (res as any).rows ?? [];
  groupTotal.value = (res as any).total ?? 0;
  groupLoading.value = false;
};

const getSubjectList = async () => {
  subjectLoading.value = true;
  const res = await listSubject(subjectQuery.value);
  subjectList.value = (res as any).rows ?? [];
  subjectTotal.value = (res as any).total ?? 0;
  subjectLoading.value = false;
};

const handleTabChange = () => {
  if (activeTab.value === 'groups') {
    getGroupList();
  } else {
    getSubjectList();
  }
};

const resetGroup = () => {
  groupForm.value = { ...groupInitForm };
  groupFormRef.value?.resetFields();
};

const resetSubject = () => {
  subjectForm.value = { ...subjectInitForm, groupId: groupOptions.value[0]?.groupId };
  subjectFormRef.value?.resetFields();
};

const handleGroupQuery = () => {
  groupQuery.value.pageNum = 1;
  getGroupList();
};

const resetGroupQuery = () => {
  groupQueryFormRef.value?.resetFields();
  handleGroupQuery();
};

const handleSubjectQuery = () => {
  subjectQuery.value.pageNum = 1;
  getSubjectList();
};

const resetSubjectQuery = () => {
  subjectQueryFormRef.value?.resetFields();
  handleSubjectQuery();
};

const handleGroupAdd = () => {
  resetGroup();
  groupDialog.visible = true;
  groupDialog.title = '新增栏目';
};

const handleGroupUpdate = async (row: RkSubjectGroup) => {
  resetGroup();
  const res = await getSubjectGroup(row.groupId);
  Object.assign(groupForm.value, (res as any).data);
  groupDialog.visible = true;
  groupDialog.title = '修改栏目';
};

const handleGroupStatusChange = async (row: RkSubjectGroup) => {
  const text = row.status === '0' ? '启用' : '禁用';
  try {
    await proxy?.$modal.confirm(`确认要${text}栏目"${row.groupName}"吗？`);
    await changeSubjectGroupStatus(row.groupId, row.status || '0');
    proxy?.$modal.msgSuccess(`${text}成功`);
    await getGroupOptions();
    await getSubjectList();
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitGroupForm = () => {
  groupFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    groupForm.value.groupId ? await updateSubjectGroup(groupForm.value) : await addSubjectGroup(groupForm.value);
    proxy?.$modal.msgSuccess('操作成功');
    groupDialog.visible = false;
    await getGroupList();
    await getGroupOptions();
  });
};

const handleGroupDelete = async (row: RkSubjectGroup) => {
  await proxy?.$modal.confirm(`是否确认删除栏目"${row.groupName}"？`);
  await delSubjectGroup(row.groupId);
  proxy?.$modal.msgSuccess('删除成功');
  await getGroupList();
  await getGroupOptions();
};

const handleSubjectAdd = () => {
  resetSubject();
  subjectDialog.visible = true;
  subjectDialog.title = '新增科目';
};

const handleSubjectUpdate = async (row: RkSubject) => {
  resetSubject();
  const res = await getSubject(row.subjectId);
  Object.assign(subjectForm.value, (res as any).data);
  subjectDialog.visible = true;
  subjectDialog.title = '修改科目';
};

const handleSubjectStatusChange = async (row: RkSubject) => {
  const text = row.status === '0' ? '启用' : '禁用';
  try {
    await proxy?.$modal.confirm(`确认要${text}科目"${row.subjectName}"吗？`);
    await changeSubjectStatus(row.subjectId, row.status || '0');
    proxy?.$modal.msgSuccess(`${text}成功`);
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitSubjectForm = () => {
  subjectFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    subjectForm.value.subjectId ? await updateSubject(subjectForm.value) : await addSubject(subjectForm.value);
    proxy?.$modal.msgSuccess('操作成功');
    subjectDialog.visible = false;
    await getSubjectList();
    await getGroupList();
  });
};

const handleSubjectDelete = async (row: RkSubject) => {
  await proxy?.$modal.confirm(`是否确认删除科目"${row.subjectName}"？`);
  await delSubject(row.subjectId);
  proxy?.$modal.msgSuccess('删除成功');
  await getSubjectList();
  await getGroupList();
};

onMounted(async () => {
  await getGroupOptions();
  await Promise.all([getSubjectList(), getGroupList()]);
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

.rk-number-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

@media (max-width: 900px) {
  .rk-admin-head,
  .rk-toolbar {
    display: grid;
  }

  .rk-number-row {
    grid-template-columns: 1fr;
  }
}
</style>
