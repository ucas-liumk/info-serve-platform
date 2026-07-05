<template>
  <div class="p-2">
    <el-card shadow="never">
      <template #header>
        <div class="flex justify-between items-center">
          <span>门户模块注册表（排序靠前 6 项显示在首页首屏，其余进入更多服务）</span>
          <el-button v-hasPermi="['portal:module:add']" type="primary" plain icon="Plus" @click="openDialog()">新增模块</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="moduleList" border>
        <el-table-column label="首页排序" prop="sortOrder" width="90" align="center" />
        <el-table-column label="编码" prop="moduleCode" width="120" />
        <el-table-column label="名称" prop="moduleName" width="140" />
        <el-table-column label="副标题" prop="description" min-width="160" show-overflow-tooltip />
        <el-table-column label="入口路由" prop="entryPath" min-width="150" show-overflow-tooltip />
        <el-table-column label="权限码" prop="perms" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.perms || '登录即可见' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button v-hasPermi="['portal:module:edit']" link type="primary" icon="Edit" @click="openDialog(row)">修改</el-button>
            <el-button v-hasPermi="['portal:module:remove']" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="模块编码" prop="moduleCode">
          <el-input v-model="form.moduleCode" placeholder="如 resources / appcenter" :disabled="form.moduleId != null" />
        </el-form-item>
        <el-form-item label="模块名称" prop="moduleName">
          <el-input v-model="form.moduleName" placeholder="卡片标题" />
        </el-form-item>
        <el-form-item label="副标题" prop="description">
          <el-input v-model="form.description" placeholder="卡片副标题" />
        </el-form-item>
        <el-form-item label="入口路由" prop="entryPath">
          <el-input v-model="form.entryPath" placeholder="/portal/xxx，敬请期待可留空" />
        </el-form-item>
        <el-form-item label="配图地址" prop="image">
          <el-input v-model="form.image" placeholder="留空使用内置配图" />
        </el-form-item>
        <el-form-item label="权限码" prop="perms">
          <el-input v-model="form.perms" placeholder="留空=登录即可见" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">敬请期待</el-radio>
            <el-radio value="2">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="首页排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <div class="form-tip">数值越小越靠前，前 6 项进入首页首屏。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="PortalModule">
import { ElMessage, ElMessageBox } from 'element-plus';
import { addModule, deleteModule, listModules, updateModule, type PortalModuleItem } from '@/api/portal/module';

const loading = ref(false);
const saving = ref(false);
const moduleList = ref<PortalModuleItem[]>([]);
const formRef = ref<ElFormInstance>();

const emptyForm = (): PortalModuleItem => ({ moduleCode: '', moduleName: '', description: '', entryPath: '', image: '', perms: '', status: '0', sortOrder: 0 });
const form = reactive<PortalModuleItem>(emptyForm());
const dialog = reactive({ visible: false, title: '' });

const rules = {
  moduleCode: [{ required: true, message: '模块编码不能为空', trigger: 'blur' }],
  moduleName: [{ required: true, message: '模块名称不能为空', trigger: 'blur' }]
};

const statusLabel = (s: string) => ({ '0': '启用', '1': '敬请期待', '2': '隐藏' })[s] ?? s;
const statusTag = (s: string) => (({ '0': 'success', '1': 'warning', '2': 'info' })[s] ?? 'info') as 'success' | 'warning' | 'info';

const loadList = async () => {
  loading.value = true;
  try {
    const res = await listModules();
    moduleList.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
};

const openDialog = (row?: PortalModuleItem) => {
  Object.assign(form, emptyForm(), row ?? {});
  dialog.title = row ? `修改模块：${row.moduleName}` : '新增模块';
  dialog.visible = true;
};

const submit = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.moduleId != null) {
      await updateModule({ ...form });
    } else {
      await addModule({ ...form });
    }
    ElMessage.success('操作成功');
    dialog.visible = false;
    await loadList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (row: PortalModuleItem) => {
  await ElMessageBox.confirm(`确认删除模块「${row.moduleName}」？门户首页将不再显示该卡片。`, '提示', { type: 'warning' });
  await deleteModule(row.moduleId!);
  ElMessage.success('删除成功');
  await loadList();
};

onMounted(loadList);
</script>

<style scoped>
.form-tip {
  width: 100%;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}
</style>
