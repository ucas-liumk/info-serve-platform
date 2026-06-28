<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="关键字" prop="keyword">
              <el-input v-model="queryParams.keyword" placeholder="标题/内容" clearable style="width: 180px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="版块" prop="boardId">
              <el-select v-model="queryParams.boardId" placeholder="请选择版块" clearable style="width: 150px">
                <el-option v-for="item in boardOptions" :key="item.boardId" :label="item.boardName" :value="item.boardId" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
                <el-option label="显示" value="0" />
                <el-option label="隐藏" value="1" />
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
            <el-button v-hasPermi="['infoservice:forumTopic:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:forumTopic:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()"
              >修改</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:forumTopic:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
              >删除</el-button
            >
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="topicList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="标题" align="center" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="版块" align="center" prop="boardName" width="120" show-overflow-tooltip />
        <el-table-column label="作者" align="center" prop="authorName" width="110" show-overflow-tooltip />
        <el-table-column label="浏览" align="center" prop="viewCount" width="80" />
        <el-table-column label="回复" align="center" prop="replyCount" width="80" />
        <el-table-column label="点赞" align="center" prop="likeCount" width="80" />
        <el-table-column label="置顶" align="center" prop="isTop" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.isTop === '1' ? 'warning' : 'info'">
              {{ scope.row.isTop === '1' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              :active-text="scope.row.status === '0' ? '显示' : ''"
              :inactive-text="scope.row.status === '1' ? '隐藏' : ''"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" fixed="right" align="center" width="130" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['infoservice:forumTopic:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['infoservice:forumTopic:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="720px" append-to-body>
      <el-form ref="topicFormRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主题标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入主题标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属版块" prop="boardId">
              <el-select v-model="form.boardId" placeholder="请选择论坛版块" style="width: 100%">
                <el-option v-for="item in boardOptions" :key="item.boardId" :label="item.boardName" :value="item.boardId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文" prop="content">
              <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入主题正文" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="置顶" prop="isTop">
              <el-radio-group v-model="form.isTop">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关闭回复" prop="isClosed">
              <el-radio-group v-model="form.isClosed">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
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

<script setup name="InfoForumTopic" lang="ts">
import {
  addForumTopic,
  changeForumTopicStatus,
  delForumTopic,
  getForumTopicAdmin,
  listForumBoardOptions,
  listForumTopic,
  updateForumTopic
} from '@/api/infoservice/admin';
import { ForumBoard, ForumTopic, ForumTopicForm, ForumTopicQuery } from '@/api/infoservice/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const topicList = ref<ForumTopic[]>([]);
const boardOptions = ref<ForumBoard[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const topicFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: ForumTopicForm = {
  topicId: undefined,
  boardId: undefined,
  title: '',
  content: '',
  isTop: '0',
  isClosed: '0',
  status: '0',
  remark: ''
};

const data = reactive<PageData<ForumTopicForm, ForumTopicQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    boardId: undefined,
    status: ''
  },
  rules: {
    title: [{ required: true, message: '主题标题不能为空', trigger: 'blur' }],
    boardId: [{ required: true, message: '请选择论坛版块', trigger: 'change' }],
    content: [{ required: true, message: '主题正文不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs<PageData<ForumTopicForm, ForumTopicQuery>>(data);

const loadBoards = async () => {
  const res = await listForumBoardOptions({ status: '0' });
  boardOptions.value = (res as any).data ?? (res as any).rows ?? [];
};

const getList = async () => {
  loading.value = true;
  const res = await listForumTopic(queryParams.value);
  topicList.value = (res as any).rows ?? [];
  total.value = (res as any).total ?? 0;
  loading.value = false;
};

const reset = () => {
  form.value = { ...initFormData };
  topicFormRef.value?.resetFields();
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

const handleSelectionChange = (selection: ForumTopic[]) => {
  ids.value = selection.map((item) => item.topicId);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加论坛主题';
};

const handleUpdate = async (row?: ForumTopic) => {
  reset();
  const topicId = row?.topicId || ids.value[0];
  const res = await getForumTopicAdmin(topicId);
  Object.assign(form.value, (res as any).data);
  dialog.visible = true;
  dialog.title = '修改论坛主题';
};

const handleStatusChange = async (row: ForumTopic) => {
  const text = row.status === '0' ? '显示' : '隐藏';
  try {
    await proxy?.$modal.confirm(`确认要${text}主题"${row.title}"吗？`);
    await changeForumTopicStatus(row.topicId, row.status);
    proxy?.$modal.msgSuccess(`${text}成功`);
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

const submitForm = () => {
  topicFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.topicId ? await updateForumTopic(form.value) : await addForumTopic(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

const handleDelete = async (row?: ForumTopic) => {
  const topicIds = row?.topicId || ids.value;
  await proxy?.$modal.confirm(`是否确认删除论坛主题编号为"${topicIds}"的数据项？`);
  await delForumTopic(topicIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  loadBoards();
  getList();
});
</script>
