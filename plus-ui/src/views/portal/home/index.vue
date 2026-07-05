<template>
  <main class="portal-home">
    <section class="home-shell">
      <HomeTopbar @command="handleUserCommand" @open-manual="openManualDialog" />

      <ModuleGrid :modules="featuredModules" :total="modules.length" @open="openModule" @more="openModuleDialog" />

      <StatsBand :stats="stats" :loading="loading" />
    </section>

    <el-dialog
      v-model="profileDialog.visible"
      title="修改个人信息"
      width="520px"
      align-center
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetProfileForm"
    >
      <el-form
        ref="profileFormRef"
        v-loading="profileDialog.loading"
        :model="profileForm"
        :rules="profileRules"
        label-width="86px"
        class="profile-form"
      >
        <el-form-item label="头像">
          <div class="profile-avatar-field">
            <UserAvatar />
            <span class="profile-avatar-tip">点击头像可上传新头像</span>
          </div>
        </el-form-item>
        <el-form-item label="用户名称">
          <el-input v-model="profileForm.userName" disabled />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="profileForm.nickName" maxlength="30" placeholder="请输入用户昵称" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phonenumber">
          <el-input v-model="profileForm.phonenumber" maxlength="11" placeholder="请输入手机号码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" maxlength="50" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.sex">
            <el-radio value="0">男</el-radio>
            <el-radio value="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="profileDialog.saving" @click="submitProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordDialog.visible"
      title="修改密码"
      width="440px"
      align-center
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetPasswordForm"
    >
      <el-alert
        title="为确保账号安全，请定期修改密码。修改成功后，下次登录请使用新密码。"
        type="info"
        show-icon
        :closable="false"
        class="password-alert"
      />
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="88px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="passwordDialog.saving" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>

    <AllModuleDialog v-model="moduleDialog.visible" :modules="modules" :saving="moduleDialog.saving" @open="openModule" @reorder="saveModuleOrder" />
    <SystemManualDialog v-model="manualDialogVisible" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getPortalStats } from '@/api/portal/stats';
import { PortalStats } from '@/api/infoservice/types';
import { listPortalModules, updatePortalModuleOrder } from '@/api/portal/module';
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/user';
import type { UserForm } from '@/api/system/user/types';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { useUserStore } from '@/store/modules/user';
import UserAvatar from '@/views/admin/system/user/profile/userAvatar.vue';
import AllModuleDialog from './components/AllModuleDialog.vue';
import HomeTopbar from './components/HomeTopbar.vue';
import ModuleGrid from './components/ModuleGrid.vue';
import StatsBand from './components/StatsBand.vue';
import SystemManualDialog from './components/SystemManualDialog.vue';
import moduleResource from '@/assets/portal/module-resource.png';
import moduleTools from '@/assets/portal/module-tools.png';
import moduleQa from '@/assets/portal/module-qa.png';
import moduleHot from '@/assets/portal/module-hot.png';
import moduleForum from '@/assets/portal/module-forum.png';
import moduleLowcode from '@/assets/portal/module-lowcode.png';
import moduleAnalysis from '@/assets/portal/module-analysis.png';
import moduleUsageDashboard from '@/assets/portal/module-usage-dashboard.png';
import moduleDashboard from '@/assets/portal/module-dashboard.png';

interface HomeModule {
  code?: string;
  title: string;
  desc: string;
  image: string;
  path?: string;
  sortOrder?: number;
}

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const profileFormRef = ref<ElFormInstance>();
const passwordFormRef = ref<ElFormInstance>();
const manualDialogVisible = ref(false);

type PortalProfileForm = Partial<UserForm> & {
  userName?: string;
};

interface PasswordForm {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const profileDialog = reactive({
  visible: false,
  loading: false,
  saving: false
});

const passwordDialog = reactive({
  visible: false,
  saving: false
});

const moduleDialog = reactive({
  visible: false,
  saving: false
});

const profileForm = reactive<PortalProfileForm>({
  userName: '',
  nickName: '',
  phonenumber: '',
  email: '',
  sex: '0'
});

const passwordForm = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const validateOptionalPhone = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || /^1[3456789][0-9]\d{8}$/.test(value)) {
    callback();
    return;
  }
  callback(new Error('请输入正确的手机号码'));
};

const profileRules: ElFormRules = {
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  phonenumber: [{ validator: validateOptionalPhone, trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }]
};

const validateNewPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && passwordForm.oldPassword && value === passwordForm.oldPassword) {
    callback(new Error('新密码不能和旧密码一致'));
    return;
  }
  callback();
};

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'));
    return;
  }
  callback();
};

const passwordRules: ElFormRules = {
  oldPassword: [{ required: true, message: 'Old password is required', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, max: 20, message: 'Password length must be 6 to 20 characters', trigger: 'blur' },
    { pattern: /^[^<>"'|\\]+$/, message: 'Password contains invalid characters', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: 'Confirm password is required', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

const stats = ref<PortalStats>({
  resourceCount: 0,
  toolCount: 0,
  topicCount: 0,
  activeUserCount: 0,
  todayVisitCount: 0
});

/** 各模块默认配图（注册表 image 为空时按 moduleCode 兜底） */
const MODULE_ART: Record<string, string> = {
  resources: moduleResource,
  appcenter: moduleTools,
  lowcode: moduleLowcode,
  analysis: moduleAnalysis,
  'usage-dashboard': moduleUsageDashboard,
  dashboard: moduleDashboard,
  qa: moduleQa,
  news: moduleHot,
  forum: moduleForum
};

const MODULE_NAME_ART: Record<string, string> = {
  低代码: moduleLowcode,
  运行分析: moduleAnalysis,
  应用态势: moduleUsageDashboard,
  态势: moduleDashboard
};

const HOME_MODULE_LIMIT = 6;

/** 注册表不可用时的兜底卡片（与种子数据一致） */
const DEFAULT_MODULES: HomeModule[] = [
  { code: 'resources', title: '资料共享', desc: '数据汇聚  共享共用', image: moduleResource, path: '/portal/resources', sortOrder: 10 },
  { code: 'appcenter', title: '应用中心', desc: '应用聚合  即取即用', image: moduleTools, path: '/portal/tools', sortOrder: 20 },
  { code: 'usage-dashboard', title: '应用态势', desc: '运行洞察  转型透明', image: moduleUsageDashboard, path: '/portal/usage-dashboard', sortOrder: 30 },
  { code: 'forum', title: '服务论坛', desc: '交流互动  共建共治', image: moduleForum, path: '/portal/forum', sortOrder: 40 },
  { code: 'qa', title: '智能问答', desc: '智慧问答  快速响应', image: moduleQa, sortOrder: 80 },
  { code: 'news', title: '时事热点', desc: '热点速递  洞察先机', image: moduleHot, sortOrder: 90 }
];

const modules = ref<HomeModule[]>(DEFAULT_MODULES);
const featuredModules = computed(() => modules.value.slice(0, HOME_MODULE_LIMIT));

/** 首页卡片改从 portal_module 注册表渲染（后台可配启停/排序/权限） */
const loadModules = async () => {
  try {
    const res = await listPortalModules();
    const rows = res.data ?? [];
    if (rows.length > 0) {
      modules.value = rows
        .map((row) => ({
          code: row.moduleCode,
          title: row.moduleName,
          desc: row.description || '',
          image: row.image || resolveModuleImage(row.moduleCode, row.moduleName),
          path: row.status === '0' && row.entryPath ? row.entryPath : undefined,
          sortOrder: Number(row.sortOrder || 0)
        }))
        .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
    }
  } catch {
    // 注册表不可用时保留兜底卡片，不阻塞首页
  }
};

const resolveModuleImage = (moduleCode?: string, moduleName?: string) => {
  if (moduleCode && MODULE_ART[moduleCode]) {
    return MODULE_ART[moduleCode];
  }
  const matchedName = Object.keys(MODULE_NAME_ART).find((keyword) => moduleName?.includes(keyword));
  return matchedName ? MODULE_NAME_ART[matchedName] : moduleTools;
};

const openModule = (item: HomeModule) => {
  if (item.path) {
    if (/^https?:\/\//.test(item.path)) {
      window.open(normalizeServiceUrl(item.path), '_blank');
      return;
    }
    router.push(item.path);
    return;
  }
  ElMessage.info(`${item.title}将在后续版本开放`);
};

const openModuleDialog = () => {
  moduleDialog.visible = true;
};

const openManualDialog = () => {
  manualDialogVisible.value = true;
};

const saveModuleOrder = async (orderedModules: HomeModule[]) => {
  modules.value = orderedModules;
  const moduleCodes = orderedModules.map((item) => item.code).filter((code): code is string => Boolean(code));
  if (moduleCodes.length === 0) {
    return;
  }

  moduleDialog.saving = true;
  try {
    await updatePortalModuleOrder(moduleCodes);
    ElMessage.success('首页服务顺序已保存');
  } catch {
    ElMessage.error('顺序已调整，但保存失败，请稍后重试');
  } finally {
    moduleDialog.saving = false;
  }
};

const normalizeServiceUrl = (raw: string) => {
  try {
    const url = new URL(raw);
    if (['127.0.0.1', 'localhost'].includes(url.hostname)) {
      url.hostname = window.location.hostname;
    }
    return url.toString();
  } catch {
    return raw;
  }
};

const openProfile = () => {
  profileDialog.visible = true;
  void loadProfile();
};

const resetProfileForm = () => {
  Object.assign(profileForm, {
    userName: '',
    nickName: '',
    phonenumber: '',
    email: '',
    sex: '0'
  });
  profileFormRef.value?.clearValidate();
};

const loadProfile = async () => {
  profileDialog.loading = true;
  try {
    const { data } = await getUserProfile();
    Object.assign(profileForm, data.user || {});
  } finally {
    profileDialog.loading = false;
  }
};

const submitProfile = () => {
  profileFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }

    profileDialog.saving = true;
    try {
      const payload = {
        ...profileForm,
        phonenumber: profileForm.phonenumber || undefined,
        email: profileForm.email || undefined
      } as UserForm;
      await updateUserProfile(payload);
      await userStore.getInfo();
      ElMessage.success('个人信息修改成功');
      profileDialog.visible = false;
    } finally {
      profileDialog.saving = false;
    }
  });
};

const openPasswordDialog = () => {
  passwordDialog.visible = true;
};

const resetPasswordForm = () => {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  passwordDialog.saving = false;
  passwordFormRef.value?.clearValidate();
};

const submitPassword = () => {
  passwordFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }

    passwordDialog.saving = true;
    try {
      await updateUserPwd(passwordForm.oldPassword, passwordForm.newPassword);
      ElMessage.success('密码修改成功');
      passwordDialog.visible = false;
    } finally {
      passwordDialog.saving = false;
    }
  });
};

const logout = async () => {
  const confirmed = await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => false);

  if (!confirmed) {
    return;
  }

  await userStore.logout();
  await router.replace({
    path: '/login',
    query: {
      redirect: encodeURIComponent(router.currentRoute.value.fullPath || PORTAL_HOME_PATH)
    }
  });
};

const handleUserCommand = (command: string | number | object) => {
  if (command === 'profile') {
    openProfile();
    return;
  }
  if (command === 'password') {
    openPasswordDialog();
    return;
  }
  if (command === 'logout') {
    void logout();
  }
};

onMounted(async () => {
  void loadModules();

  loading.value = true;
  try {
    const res: any = await getPortalStats();
    stats.value = { ...stats.value, ...(res.data || {}) };
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.portal-home {
  --portal-max: var(--ip-layout-max);
  --portal-blue: var(--ip-primary-900);
  --portal-text: var(--ip-neutral-900);
  --portal-muted: var(--ip-neutral-600);

  min-height: 100vh;
  overflow: hidden;
  color: var(--portal-text);
  background-image:
    linear-gradient(180deg, rgba(247, 252, 255, 0.04) 0%, rgba(247, 252, 255, 0.1) 44%, rgba(247, 252, 255, 0.72) 78%, var(--ip-neutral-50) 100%),
    url('@/assets/portal/portal-home-bg.png');
  background-position:
    center top,
    center -104px;
  background-repeat: no-repeat;
  background-size:
    100% 100%,
    cover;
}

.home-shell {
  min-height: 100vh;
  padding: 32px 48px;
}

.profile-form {
  padding: 4px 10px 0 0;
}

.profile-avatar-field {
  display: flex;
  min-height: 112px;
  align-items: center;
  gap: 16px;
}

.profile-avatar-field :deep(.user-info-head) {
  height: 112px;
}

.profile-avatar-field :deep(.user-info-head:hover::after) {
  line-height: 112px;
}

.profile-avatar-field :deep(.img-lg) {
  width: 112px;
  height: 112px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 999px;
  object-fit: cover;
  box-shadow: var(--ip-shadow-md);
}

.profile-avatar-tip {
  color: var(--ip-neutral-500);
  font-size: 13px;
}

@media (max-width: 1460px) {
  .home-shell {
    padding: 28px 32px;
  }
}

@media (max-width: 1180px) {
  .portal-home {
    background-position:
      center top,
      center -48px;
  }
}

@media (max-width: 1023px) {
  .portal-home {
    overflow: auto;
  }
}

@media (max-width: 640px) {
  .home-shell {
    padding: 18px;
  }
}
</style>
