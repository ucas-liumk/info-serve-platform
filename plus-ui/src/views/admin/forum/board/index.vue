<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="关键字" prop="keyword">
              <el-input v-model="queryParams.keyword" placeholder="名称/编码" clearable style="width: 180px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
                <el-option label="启用" value="0" />
                <el-option label="禁用" value="1" />
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
            <el-button v-hasPermi="['infoservice:forumBoard:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:forumBoard:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()"
              >修改</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:forumBoard:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
              >删除</el-button
            >
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="boardList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="版块名称" align="center" prop="boardName" min-width="130" show-overflow-tooltip />
        <el-table-column label="版块编码" align="center" prop="boardCode" min-width="120" show-overflow-tooltip />
        <el-table-column label="主题数" align="center" prop="topicCount" width="90" />
        <el-table-column label="排序" align="center" prop="orderNum" width="80" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" align="center" width="130" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['infoservice:forumBoard:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['infoservice:forumBoard:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="520px" append-to-body>
      <el-form ref="boardFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="版块名称" prop="boardName">
          <el-input v-model="form.boardName" placeholder="请输入版块名称" />
        </el-form-item>
        <el-form-item label="版块编码" prop="boardCode">
          <el-input v-model="form.boardCode" placeholder="请输入版块编码" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入版块说明" />
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
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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

<script setup name="InfoForumBoard" lang="ts">
import { addForumBoard, changeForumBoardStatus, delForumBoard, getForumBoard, listForumBoard, updateForumBoard } from '@/api/infoservice/admin';
import { ForumBoard, ForumBoardForm, ForumBoardQuery } from '@/api/infoservice/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const boardList = ref<ForumBoard[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const boardFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: ForumBoardForm = {
  boardId: undefined,
  boardName: '',
  boardCode: '',
  description: '',
  orderNum: 0,
  status: '0',
  remark: ''
};

const data = reactive<PageData<ForumBoardForm, ForumBoardQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    status: ''
  },
  rules: {
    boardName: [{ required: true, message: '版块名称不能为空', trigger: 'blur' }],
    boardCode: [{ required: true, message: '版块编码不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs<PageData<ForumBoardForm, ForumBoardQuery>>(data);

const getList = async () => {
  loading.value = true;
  const res = await listForumBoard(queryParams.value);
  boardList.value = (res as any).rows ?? [];
  total.value = (res as any).total ?? 0;
  loading.value = false;
};

const reset = () => {
  form.value = { ...initFormData };
  boardFormRef.value?.resetFields();
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

const handleSelectionChange = (selection: ForumBoard[]) => {
  ids.value = selection.map((item) => item.boardId);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加论坛版块';
};

const handleUpdate = async (row?: ForumBoard) => {
  reset();
  const boardId = row?.boardId || ids.value[0];
  const res = await getForumBoard(boardId);
  Object.assign(form.value, (res as any).data);
  dialog.visible = true;
  dialog.title = '修改论坛版块';
};

const handleStatusChange = async (row: ForumBoard) => {
  const text = row.status === '0' ? '启用' : '禁用';
  try {
    await proxy?.$modal.confirm(`确认要${text}版块"${row.boardName}"吗？`);
    await changeForumBoardStatus(row.boardId, row.status || '0');
    proxy?.$modal.msgSuccess(`${text}成功`);
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitForm = () => {
  boardFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.boardId ? await updateForumBoard(form.value) : await addForumBoard(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

const handleDelete = async (row?: ForumBoard) => {
  const boardIds = row?.boardId || ids.value;
  await proxy?.$modal.confirm(`是否确认删除论坛版块编号为"${boardIds}"的数据项？`);
  await delForumBoard(boardIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>
