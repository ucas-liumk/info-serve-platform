<template>
  <!-- 自 HomeTopbar/home-index 抽取的自包含用户菜单；TopBar 与首页共用 -->
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

  <el-dialog
    v-model="profileDialog.visible"
    title="修改个人信息"
    width="520px"
    align-center
    append-to-body
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
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, UserFilled } from '@element-plus/icons-vue';
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/user';
import type { UserForm } from '@/api/system/user/types';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { useUserStore } from '@/store/modules/user';
import UserAvatar from '@/views/admin/system/user/profile/userAvatar.vue';

const router = useRouter();
const userStore = useUserStore();
const profileFormRef = ref<ElFormInstance>();
const passwordFormRef = ref<ElFormInstance>();

const userLabel = computed(() => userStore.nickname || userStore.name || '当前用户');

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
</script>

<style scoped>
.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ip-primary-900);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.user-pill:hover,
.user-pill:focus-visible {
  color: var(--ip-primary-600);
  outline: none;
}

.avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
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
  border: 1px solid var(--ip-neutral-200);
  border-radius: 999px;
  object-fit: cover;
  box-shadow: var(--ip-shadow-md);
}

.profile-avatar-tip {
  color: var(--ip-neutral-500);
  font-size: 13px;
}
</style>
