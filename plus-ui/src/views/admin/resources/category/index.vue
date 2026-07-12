<template>
  <div class="p-2">
    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['infoservice:resourceCategory:add']" type="primary" plain icon="Plus" @click="handleAdd()">新增栏目</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="Sort" @click="handleToggleExpandAll">展开/折叠</el-button>
          </el-col>
          <right-toolbar :search="false" @query-table="getList" />
        </el-row>
      </template>

      <el-table
        ref="categoryTableRef"
        v-loading="loading"
        :data="categoryList"
        row-key="categoryId"
        border
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :default-expand-all="isExpandAll"
      >
        <el-table-column label="名称" prop="categoryName" min-width="200" show-overflow-tooltip />
        <el-table-column label="编码" align="center" prop="categoryCode" min-width="120" show-overflow-tooltip />
        <el-table-column label="图标" align="center" prop="icon" width="90" />
        <el-table-column label="资料数" align="center" width="90">
          <template #default="scope">
            <span>{{ isSectionRow(scope.row) ? '—' : (scope.row.resourceCount ?? 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排序" align="center" prop="orderNum" width="80" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" align="center" width="160" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['infoservice:resourceCategory:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip v-if="isSectionRow(scope.row)" content="新增分类" placement="top">
              <el-button v-hasPermi="['infoservice:resourceCategory:add']" link type="primary" icon="Plus" @click="handleAdd(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['infoservice:resourceCategory:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="520px" append-to-body>
      <el-form ref="categoryFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="isCategoryForm" label="上级栏目" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择上级栏目" style="width: 100%">
            <el-option
              v-for="item in sectionOptions"
              :key="item.categoryId"
              :label="item.status === '1' ? `${item.categoryName}（已停用）` : item.categoryName"
              :value="item.categoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="isCategoryForm ? '分类名称' : '栏目名称'" prop="categoryName">
          <el-input v-model="form.categoryName" :placeholder="isCategoryForm ? '请输入分类名称' : '请输入栏目名称'" />
        </el-form-item>
        <el-form-item :label="isCategoryForm ? '分类编码' : '栏目编码'" prop="categoryCode">
          <el-input v-model="form.categoryCode" :placeholder="isCategoryForm ? '请输入分类编码' : '请输入栏目编码'" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入说明" />
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

<script setup name="InfoResourceCategory" lang="ts">
import {
  addResourceCategory,
  changeResourceCategoryStatus,
  delResourceCategory,
  getResourceCategory,
  listResourceCategoryTree,
  updateResourceCategory
} from '@/api/infoservice/admin';
import { ResourceCategory, ResourceCategoryForm } from '@/api/infoservice/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

/** 树形表格数据（handleTree 组装：一级=栏目，二级=分类） */
const categoryList = ref<ResourceCategory[]>([]);
/** 弹窗「上级栏目」候选（仅一级栏目行，含停用） */
const sectionOptions = ref<ResourceCategory[]>([]);
const loading = ref(true);
const isExpandAll = ref(true);
/** 弹窗当前是否在编辑二级分类（true=须选上级栏目；false=栏目） */
const isCategoryForm = ref(false);

const categoryTableRef = ref<ElTableInstance>();
const categoryFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: ResourceCategoryForm = {
  categoryId: undefined,
  categoryName: '',
  categoryCode: '',
  description: '',
  icon: '',
  parentId: undefined,
  orderNum: 0,
  status: '0',
  remark: ''
};

const form = ref<ResourceCategoryForm>({ ...initFormData });

const rules = reactive<ElFormRules>({
  parentId: [{ required: true, message: '上级栏目不能为空', trigger: 'change' }],
  categoryName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  categoryCode: [
    { required: true, message: '编码不能为空', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{1,80}$/, message: '仅支持字母、数字、中划线、下划线，不超过 80 字符', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) =>
        value && value.toLowerCase() === 'all' ? callback(new Error('all 为系统保留编码，请更换')) : callback(),
      trigger: 'blur'
    }
  ]
});

/** 是否栏目行（parentId 为空=一级栏目） */
const isSectionRow = (row: ResourceCategory): boolean => row.parentId == null;

/** 查询栏目/分类树 */
const getList = async () => {
  loading.value = true;
  try {
    const res = await listResourceCategoryTree();
    const rows = res.data ?? [];
    sectionOptions.value = rows.filter((row) => row.parentId == null);
    categoryList.value = proxy?.handleTree<ResourceCategory>(rows, 'categoryId') ?? [];
  } finally {
    loading.value = false;
  }
};

/** 表单重置 */
const reset = () => {
  form.value = { ...initFormData };
  categoryFormRef.value?.resetFields();
};

/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};

/** 展开/折叠操作 */
const handleToggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value;
  toggleExpandAll(categoryList.value, isExpandAll.value);
};
/** 展开/折叠所有 */
const toggleExpandAll = (data: ResourceCategory[], status: boolean) => {
  data.forEach((item) => {
    categoryTableRef.value?.toggleRowExpansion(item, status);
    if (item.children && item.children.length > 0) toggleExpandAll(item.children, status);
  });
};

/** 新增按钮操作：不传 row=新增栏目；传栏目行=在该栏目下新增分类 */
const handleAdd = (row?: ResourceCategory) => {
  reset();
  isCategoryForm.value = !!row;
  if (row) {
    form.value = { ...form.value, parentId: row.categoryId };
  }
  dialog.visible = true;
  dialog.title = row ? '添加分类' : '添加栏目';
};

/** 修改按钮操作 */
const handleUpdate = async (row: ResourceCategory) => {
  reset();
  const res = await getResourceCategory(row.categoryId);
  const data = res.data;
  form.value = {
    ...initFormData,
    categoryId: data.categoryId,
    categoryName: data.categoryName ?? '',
    categoryCode: data.categoryCode ?? '',
    description: data.description ?? '',
    icon: data.icon ?? '',
    parentId: data.parentId ?? undefined,
    orderNum: data.orderNum ?? 0,
    status: data.status ?? '0',
    remark: data.remark ?? ''
  };
  isCategoryForm.value = form.value.parentId != null;
  dialog.visible = true;
  dialog.title = isCategoryForm.value ? '修改分类' : '修改栏目';
};

/** 状态切换（停用不级联删除，门户侧自行过滤） */
const handleStatusChange = async (row: ResourceCategory) => {
  const label = isSectionRow(row) ? '栏目' : '分类';
  const text = row.status === '0' ? '启用' : '禁用';
  try {
    await proxy?.$modal.confirm(`确认要${text}${label}"${row.categoryName}"吗？`);
    await changeResourceCategoryStatus(row.categoryId, row.status || '0');
    proxy?.$modal.msgSuccess(`${text}成功`);
  } catch {
    row.status = row.status === '0' ? '1' : '0';
  }
};

/** 提交按钮 */
const submitForm = () => {
  categoryFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      const payload: ResourceCategoryForm = {
        ...form.value,
        parentId: isCategoryForm.value ? form.value.parentId : null
      };
      payload.categoryId ? await updateResourceCategory(payload) : await addResourceCategory(payload);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

/** 删除按钮操作（行内删除；栏目须无子分类、分类须无挂接资料，由服务端校验并报中文错误） */
const handleDelete = async (row: ResourceCategory) => {
  const label = isSectionRow(row) ? '栏目' : '分类';
  await proxy?.$modal.confirm(`是否确认删除${label}"${row.categoryName}"？`);
  await delResourceCategory(row.categoryId);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>
