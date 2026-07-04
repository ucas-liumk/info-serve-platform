<template>
  <main class="portal-home">
    <section class="home-shell">
      <header class="topbar">
        <div class="brand">
          <img class="brand-logo" :src="logoUrl" alt="信息中心数智服务平台" />
          <div class="brand-copy">
            <h1>信息中心数智服务平台</h1>
            <p>数智驱动 · 高效服务 · 共创共享</p>
          </div>
        </div>

        <div class="status-panel">
          <div class="status-row date-row">
            <span>{{ dateText }}</span>
            <span class="divider" aria-hidden="true"></span>
            <span>{{ timeText }}</span>
            <span class="divider" aria-hidden="true"></span>
            <span>{{ weekText }}</span>
          </div>
          <div class="status-row user-row">
            <span class="weather">
              <el-icon><Sunny /></el-icon>
              <b>26°C</b>
              <em>晴</em>
            </span>
            <span class="divider" aria-hidden="true"></span>
            <PortalNotificationBell />
            <span class="divider" aria-hidden="true"></span>
            <el-dropdown trigger="click" @command="handleUserCommand">
              <button class="user-pill" type="button">
                <span class="avatar">
                  <img v-if="userStore.avatar" :src="userStore.avatar" alt="" />
                  <el-icon v-else><UserFilled /></el-icon>
                </span>
                <span>{{ userLabel }}</span>
                <el-icon class="down"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="password">修改密码</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </header>

      <nav class="module-grid" aria-label="数智服务入口">
        <button v-for="item in modules" :key="item.title" class="module-card" type="button" @click="openModule(item)">
          <span class="module-visual">
            <img :src="item.image" :alt="item.title" />
          </span>
          <span class="module-title">{{ item.title }}</span>
          <span class="module-desc">{{ item.desc }}</span>
          <span class="module-action" aria-hidden="true">
            <IconArrowRight />
          </span>
        </button>
      </nav>

      <section class="stats-band" v-loading="loading">
        <div class="stats-heading">
          <strong>数智服务概览</strong>
          <span>SERVICE OVERVIEW</span>
        </div>
        <div class="stats-list">
          <div v-for="item in statsItems" :key="item.label" class="stat-item">
            <span class="stat-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="stat-copy">
              <em>{{ item.label }}</em>
              <strong
                >{{ item.value }}<small>{{ item.unit }}</small></strong
              >
            </span>
          </div>
        </div>
      </section>
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
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Calendar, ChatLineRound, Collection, Connection, Service, Sunny, UserFilled } from '@element-plus/icons-vue';
import { getPortalStats } from '@/api/infoservice/portal';
import { PortalStats } from '@/api/infoservice/types';
import { listPortalModules } from '@/api/portal/module';
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/user';
import type { UserForm } from '@/api/system/user/types';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { useUserStore } from '@/store/modules/user';
import PortalNotificationBell from '@/layout/portal/components/PortalNotificationBell.vue';
import UserAvatar from '@/views/admin/system/user/profile/userAvatar.vue';
import logoUrl from '@/assets/portal/home-logo.png';
import moduleResource from '@/assets/portal/module-resource.png';
import moduleTools from '@/assets/portal/module-tools.png';
import moduleQa from '@/assets/portal/module-qa.png';
import moduleHot from '@/assets/portal/module-hot.png';
import moduleForum from '@/assets/portal/module-forum.png';
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';

interface HomeModule {
  title: string;
  desc: string;
  image: string;
  path?: string;
}

const router = useRouter();
const userStore = useUserStore();
const now = ref(new Date());
const loading = ref(false);
const profileFormRef = ref<ElFormInstance>();
const passwordFormRef = ref<ElFormInstance>();
let timer: ReturnType<typeof setInterval> | undefined;

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
  qa: moduleQa,
  news: moduleHot,
  forum: moduleForum
};

/** 注册表不可用时的兜底卡片（与种子数据一致） */
const DEFAULT_MODULES: HomeModule[] = [
  { title: '资料共享', desc: '数据汇聚  共享共用', image: moduleResource, path: '/portal/resources' },
  { title: '工具即用', desc: '开箱即用  提升效率', image: moduleTools, path: '/portal/tools' },
  { title: '智能问答', desc: '智慧问答  快速响应', image: moduleQa },
  { title: '时事热点', desc: '热点速递  洞察先机', image: moduleHot },
  { title: '服务论坛', desc: '交流互动  共建共治', image: moduleForum, path: '/portal/forum' }
];

const modules = ref<HomeModule[]>(DEFAULT_MODULES);

/** 首页卡片改从 portal_module 注册表渲染（后台可配启停/排序/权限） */
const loadModules = async () => {
  try {
    const res = await listPortalModules();
    const rows = res.data ?? [];
    if (rows.length > 0) {
      modules.value = rows.map((row) => ({
        title: row.moduleName,
        desc: row.description || '',
        image: row.image || MODULE_ART[row.moduleCode] || moduleTools,
        path: row.status === '0' && row.entryPath ? row.entryPath : undefined
      }));
    }
  } catch {
    // 注册表不可用时保留兜底卡片，不阻塞首页
  }
};

const userLabel = computed(() => userStore.nickname || userStore.name || '当前用户');

const dateText = computed(() => {
  const year = now.value.getFullYear();
  const month = `${now.value.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.value.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
});

const timeText = computed(() => {
  const hour = `${now.value.getHours()}`.padStart(2, '0');
  const minute = `${now.value.getMinutes()}`.padStart(2, '0');
  const second = `${now.value.getSeconds()}`.padStart(2, '0');
  return `${hour}:${minute}:${second}`;
});

const weekText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.getDay()]);

const formatNumber = (value: number) => Number(value || 0).toLocaleString('en-US');

const statsItems = computed(() => {
  const serviceTotal =
    Number(stats.value.resourceCount || 0) +
    Number(stats.value.toolCount || 0) +
    Number(stats.value.topicCount || 0) +
    Number(stats.value.todayVisitCount || 0);

  return [
    { label: '数据资源总量', value: formatNumber(stats.value.resourceCount), unit: '个', icon: Collection },
    { label: '服务调用总量', value: formatNumber(serviceTotal), unit: '次', icon: Service },
    { label: '活跃用户数', value: formatNumber(stats.value.activeUserCount), unit: '人', icon: UserFilled },
    { label: '论坛话题数', value: formatNumber(stats.value.topicCount), unit: '个', icon: ChatLineRound },
    { label: '今日访问量', value: formatNumber(stats.value.todayVisitCount), unit: '次', icon: Calendar },
    { label: '在线服务数', value: formatNumber(stats.value.toolCount), unit: '个', icon: Connection }
  ];
});

const openModule = (item: HomeModule) => {
  if (item.path) {
    router.push(item.path);
    return;
  }
  ElMessage.info(`${item.title}将在后续版本开放`);
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
  timer = setInterval(() => {
    now.value = new Date();
  }, 1000);

  void loadModules();

  loading.value = true;
  try {
    const res: any = await getPortalStats();
    stats.value = { ...stats.value, ...(res.data || {}) };
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped>
.portal-home {
  --portal-max: 1544px;
  --portal-blue: #082b68;
  --portal-text: #071f4b;
  --portal-muted: rgba(7, 31, 75, 0.72);

  min-height: 100vh;
  overflow: hidden;
  color: var(--portal-text);
  background-image:
    linear-gradient(180deg, rgba(247, 252, 255, 0.04) 0%, rgba(247, 252, 255, 0.1) 44%, rgba(247, 252, 255, 0.68) 78%, #f7fbff 100%),
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

.topbar {
  max-width: var(--portal-max);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand-logo {
  width: 64px;
  height: 64px;
  object-fit: contain;
  filter: drop-shadow(0 12px 18px rgba(37, 116, 214, 0.2));
}

.brand-copy h1 {
  margin: 0 0 8px;
  color: var(--portal-blue);
  font-size: 38px;
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: 0;
  text-shadow: 0 2px 10px rgba(255, 255, 255, 0.62);
}

.brand-copy p {
  margin: 0;
  color: rgba(8, 43, 104, 0.78);
  font-size: 21px;
  line-height: 1.25;
  font-weight: 600;
}

.status-panel {
  width: 390px;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 20px;
  background: rgba(230, 244, 255, 0.74);
  box-shadow:
    0 16px 42px rgba(44, 112, 184, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px);
}

.status-row {
  display: flex;
  align-items: center;
  color: #09285d;
  white-space: nowrap;
}

.date-row {
  height: 28px;
  justify-content: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
}

.divider {
  width: 1px;
  height: 22px;
  display: inline-block;
  background: rgba(48, 96, 151, 0.36);
}

.user-row {
  justify-content: center;
  gap: 14px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(52, 100, 151, 0.16);
}

.weather {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
}

.weather .el-icon {
  color: #ffb31c;
  font-size: 25px;
  filter: drop-shadow(0 5px 9px rgba(255, 173, 35, 0.28));
}

.weather em {
  font-style: normal;
  font-weight: 600;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--portal-blue);
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
}

.user-pill:hover,
.user-pill:focus-visible {
  color: #1257c8;
  outline: none;
}

.avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #6fb1ff 0%, #1f68e3 100%);
  color: #fff;
  box-shadow: 0 8px 14px rgba(33, 103, 220, 0.24);
}

.avatar .el-icon {
  font-size: 22px;
}

.avatar img {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
  object-fit: cover;
}

.down {
  font-size: 16px;
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
  border: 1px solid rgba(8, 43, 104, 0.18);
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(8, 43, 104, 0.12);
}

.profile-avatar-tip {
  color: rgba(7, 31, 75, 0.56);
  font-size: 13px;
}

.module-grid {
  max-width: 1440px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
  margin: 248px auto 0;
}

.module-card {
  position: relative;
  height: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 20px 20px;
  border: 1px solid rgba(207, 224, 245, 0.92);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 252, 255, 0.94) 100%);
  box-shadow:
    0 18px 42px rgba(35, 90, 151, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.96);
  color: var(--portal-blue);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.module-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(circle at 50% 18%, rgba(54, 142, 255, 0.16), transparent 46%);
  opacity: 0;
  transition: opacity 0.18s ease;
  pointer-events: none;
}

.module-card:hover {
  transform: translateY(-6px);
  border-color: rgba(122, 179, 244, 0.92);
  box-shadow:
    0 28px 58px rgba(32, 98, 180, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.module-card:hover::before {
  opacity: 1;
}

.module-visual {
  width: 166px;
  height: 166px;
  display: block;
  margin-bottom: 14px;
}

.module-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.module-title {
  position: relative;
  z-index: 1;
  color: var(--portal-blue);
  font-size: 28px;
  line-height: 1.15;
  font-weight: 800;
}

.module-desc {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--portal-muted);
  font-size: 16px;
  line-height: 1.35;
  font-weight: 600;
}

.module-action {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  flex: 0 0 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 14px;
  box-sizing: border-box;
  border-radius: 50%;
  background: linear-gradient(180deg, #4fb0ff 0%, #2e86f5 52%, #1f6fe5 100%);
  color: #fff;
  line-height: 0;
  box-shadow:
    inset 0 1px 2px rgba(255, 255, 255, 0.72),
    inset 0 -4px 7px rgba(20, 91, 204, 0.36),
    0 8px 13px rgba(36, 116, 236, 0.28);
}

.module-action svg {
  width: 21px;
  height: 21px;
  color: #fff;
  transform: translateX(1px);
  filter: drop-shadow(0 1px 1px rgba(7, 61, 148, 0.2));
}

.stats-band {
  max-width: var(--portal-max);
  min-height: 108px;
  display: grid;
  grid-template-columns: 196px 1fr;
  align-items: center;
  gap: 24px;
  margin: 24px auto 0;
  padding: 18px 28px;
  border: 1px solid rgba(211, 226, 244, 0.96);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    0 20px 46px rgba(35, 85, 146, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
}

.stats-heading {
  min-height: 70px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid rgba(70, 113, 160, 0.3);
}

.stats-heading strong {
  color: #0a54aa;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 800;
}

.stats-heading span {
  margin-top: 10px;
  color: rgba(7, 31, 75, 0.56);
  font-size: 14px;
  line-height: 1;
  font-weight: 500;
}

.stats-list {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.stat-item {
  min-height: 68px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  border-right: 1px solid rgba(70, 113, 160, 0.24);
}

.stat-item:last-child {
  border-right: 0;
}

.stat-icon {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #eff8ff 0%, #cfe9ff 100%);
  color: #1f74e8;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 10px 20px rgba(42, 114, 206, 0.12);
}

.stat-icon .el-icon {
  font-size: 28px;
}

.stat-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-copy em {
  color: rgba(7, 31, 75, 0.72);
  font-size: 14px;
  line-height: 1;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.stat-copy strong {
  color: var(--portal-blue);
  font-size: 24px;
  line-height: 1;
  font-weight: 800;
  white-space: nowrap;
}

.stat-copy small {
  margin-left: 6px;
  color: var(--portal-blue);
  font-size: 15px;
  font-weight: 650;
}

@media (max-width: 1460px) {
  .home-shell {
    padding: 28px 32px;
  }

  .brand-logo {
    width: 58px;
    height: 58px;
  }

  .brand-copy h1 {
    font-size: 33px;
  }

  .brand-copy p {
    font-size: 18px;
  }

  .status-panel {
    width: 336px;
    padding: 12px 16px;
    border-radius: 18px;
  }

  .module-grid {
    gap: 16px;
    margin-top: 128px;
  }

  .module-card {
    height: 318px;
    padding-top: 16px;
  }

  .module-visual {
    width: 148px;
    height: 148px;
  }

  .module-title {
    font-size: 25px;
  }

  .module-desc {
    font-size: 15px;
  }

  .stats-band {
    grid-template-columns: 184px 1fr;
    padding: 16px 24px;
  }

  .stat-item {
    gap: 10px;
    padding-inline: 10px;
  }

  .stat-copy strong {
    font-size: 22px;
  }
}

@media (max-width: 1180px) {
  .portal-home {
    background-position:
      center top,
      center -48px;
  }

  .module-grid {
    margin-top: 96px;
  }

  .module-card {
    height: 288px;
    padding: 14px 14px 18px;
  }

  .module-visual {
    width: 124px;
    height: 124px;
  }

  .module-title {
    font-size: 22px;
  }

  .module-desc {
    font-size: 14px;
  }

  .module-action {
    width: 34px;
    height: 34px;
    min-width: 34px;
    min-height: 34px;
    flex-basis: 34px;
    margin-top: 12px;
  }

  .stats-band,
  .stats-list {
    grid-template-columns: 1fr;
  }

  .stats-band {
    align-items: stretch;
    gap: 16px;
    margin-top: 24px;
  }

  .stats-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    row-gap: 0;
  }

  .stats-heading {
    min-height: auto;
    padding-bottom: 14px;
    border-right: 0;
    border-bottom: 1px solid rgba(70, 113, 160, 0.22);
  }

  .stat-item {
    min-height: 76px;
    border-bottom: 1px solid rgba(70, 113, 160, 0.18);
  }

  .stat-item:nth-child(3n) {
    border-right: 0;
  }

  .stat-item:last-child {
    border-bottom: 0;
  }
}

@media (max-width: 1023px) {
  .portal-home {
    overflow: auto;
  }

  .topbar {
    gap: 20px;
  }

  .brand-logo {
    width: 54px;
    height: 54px;
  }

  .brand-copy h1 {
    font-size: 30px;
  }

  .brand-copy p {
    font-size: 16px;
  }

  .status-panel {
    width: 328px;
  }

  .date-row {
    font-size: 16px;
  }

  .weather,
  .admin-pill {
    font-size: 15px;
  }

  .module-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-top: 64px;
  }

  .module-card {
    height: 252px;
  }

  .module-visual {
    width: 112px;
    height: 112px;
    margin-bottom: 10px;
  }

  .module-title {
    font-size: 21px;
  }
}

@media (max-width: 840px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-panel {
    width: 100%;
    max-width: 360px;
  }
}

@media (max-width: 767px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-top: 48px;
  }

  .stats-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-item:nth-child(3n) {
    border-right: 1px solid rgba(70, 113, 160, 0.24);
  }

  .stat-item:nth-child(2n) {
    border-right: 0;
  }
}

@media (max-width: 640px) {
  .home-shell {
    padding: 18px;
  }

  .brand {
    gap: 12px;
  }

  .brand-logo {
    width: 48px;
    height: 48px;
  }

  .brand-copy h1 {
    font-size: 25px;
  }

  .brand-copy p {
    font-size: 15px;
  }

  .status-panel {
    padding: 14px;
    border-radius: 18px;
  }

  .date-row {
    gap: 9px;
    font-size: 15px;
  }

  .user-row {
    gap: 12px;
  }

  .weather,
  .admin-pill {
    font-size: 15px;
  }

  .module-grid {
    margin-top: 40px;
  }

  .module-card {
    height: 236px;
  }

  .module-visual {
    width: 104px;
    height: 104px;
  }

  .module-title {
    font-size: 20px;
  }

  .stats-band {
    padding: 20px;
    border-radius: 18px;
  }
}

@media (max-width: 520px) {
  .module-grid,
  .stats-list {
    grid-template-columns: 1fr;
  }

  .module-card {
    height: 232px;
  }

  .stat-item,
  .stat-item:nth-child(2n),
  .stat-item:nth-child(3n) {
    border-right: 0;
  }
}
</style>
