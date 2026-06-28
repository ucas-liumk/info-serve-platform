<template>
  <el-dialog
    :model-value="visible"
    width="440px"
    align-center
    destroy-on-close
    class="force-password-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <template #header>
      <div class="force-password-title">
        <span>修改初始密码</span>
      </div>
    </template>

    <el-alert title="为确保账号安全，首次登录请修改密码。" type="warning" :closable="false" show-icon class="force-password-tip" />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="force-password-form">
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" autocomplete="off" placeholder="请输入当前密码" show-password>
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" autocomplete="off" placeholder="请输入新密码" show-password>
          <template #prefix>
            <el-icon><Key /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          autocomplete="off"
          placeholder="请再次输入新密码"
          show-password
          @keyup.enter="submit"
        >
          <template #prefix>
            <el-icon><CircleCheck /></el-icon>
          </template>
        </el-input>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button type="primary" class="force-password-submit" :loading="loading" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CircleCheck, Key, Lock } from '@element-plus/icons-vue';
import { getConfigKey } from '@/api/system/config';
import { updateUserPwd } from '@/api/system/user';
import { useUserStore } from '@/store/modules/user';

const USER_INIT_PASSWORD_KEY = 'sys.user.initPassword';

const userStore = useUserStore();
const formRef = ref<ElFormInstance>();
const loading = ref(false);
const defaultPassword = ref('');
const visible = computed(() => Boolean(userStore.token && userStore.forcePasswordChange));

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const validateNewPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (form.oldPassword && value === form.oldPassword) {
    callback(new Error('新密码不能与当前密码相同'));
    return;
  }
  if (defaultPassword.value && value === defaultPassword.value) {
    callback(new Error('新密码不能与系统默认密码相同'));
    return;
  }
  callback();
};

const validateConfirmPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'));
    return;
  }
  callback();
};

const rules: ElFormRules = {
  oldPassword: [{ required: true, trigger: 'blur', message: '请输入当前密码' }],
  newPassword: [
    { required: true, trigger: 'blur', message: '请输入新密码' },
    { min: 6, max: 20, trigger: 'blur', message: '密码长度需要在 6 到 20 位之间' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', message: '请再次输入新密码' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

const resetForm = () => {
  form.oldPassword = '';
  form.newPassword = '';
  form.confirmPassword = '';
  formRef.value?.clearValidate();
};

const loadDefaultPassword = async () => {
  try {
    const { data } = await getConfigKey(USER_INIT_PASSWORD_KEY);
    defaultPassword.value = String(data || '');
  } catch {
    defaultPassword.value = '';
  }
};

const submit = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }

    loading.value = true;
    try {
      await updateUserPwd(form.oldPassword, form.newPassword);
      userStore.setForcePasswordChange(false);
      resetForm();
      ElMessage.success('密码修改成功');
    } finally {
      loading.value = false;
    }
  });
};

watch(
  visible,
  (value) => {
    if (value) {
      void loadDefaultPassword();
    } else {
      resetForm();
    }
  },
  { immediate: true }
);
</script>

<style lang="scss" scoped>
.force-password-title {
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.force-password-tip {
  margin-bottom: 18px;
}

.force-password-form {
  padding-top: 2px;
}

.force-password-submit {
  width: 100%;
}
</style>
