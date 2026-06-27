<template>
  <main class="auth-page auth-page--register">
    <section class="auth-visual" aria-label="信息中心数智服务平台">
      <div class="auth-brand">
        <img class="auth-brand__logo" :src="logoUrl" alt="信息中心数智服务平台" />
        <div>
          <h1>信息中心数智服务平台</h1>
          <p>数智驱动 · 高效服务 · 共创共享</p>
        </div>
      </div>
    </section>

    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="auth-card auth-card--register">
      <div class="auth-card__heading">
        <h2>欢迎注册</h2>
        <p>信息中心数智服务平台</p>
      </div>

      <el-form-item prop="username" class="auth-form-item">
        <el-input v-model="registerForm.username" type="text" size="large" autocomplete="off" placeholder="请输入用户名">
          <template #prefix>
            <el-icon class="auth-input-icon"><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="phonenumber" class="auth-form-item">
        <el-input v-model="registerForm.phonenumber" type="text" size="large" autocomplete="off" placeholder="请输入手机号">
          <template #prefix>
            <el-icon class="auth-input-icon"><Iphone /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password" class="auth-form-item">
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large"
          autocomplete="off"
          placeholder="请输入密码"
          show-password
          @keyup.enter="handleRegister"
        >
          <template #prefix>
            <el-icon class="auth-input-icon"><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="confirmPassword" class="auth-form-item">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large"
          autocomplete="off"
          placeholder="请再次输入密码"
          show-password
          @keyup.enter="handleRegister"
        >
          <template #prefix>
            <el-icon class="auth-input-icon"><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item class="auth-submit">
        <el-button :loading="loading" size="large" type="primary" class="auth-submit-button" @click.prevent="handleRegister">
          <span v-if="!loading">注 册</span>
          <span v-else>注册中...</span>
        </el-button>

        <div class="auth-divider"><span>或</span></div>
        <div class="auth-switch-line">
          <span>已有账号?</span>
          <router-link :to="'/login'">返回登录</router-link>
        </div>
      </el-form-item>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import { Iphone, Lock, User } from '@element-plus/icons-vue';
import { to } from 'await-to-js';
import { register } from '@/api/login';
import { RegisterForm } from '@/api/types';
import logoUrl from '@/assets/portal/home-logo.png';

const router = useRouter();

const registerForm = ref<RegisterForm>({
  tenantId: '000000',
  username: '',
  phonenumber: '',
  password: '',
  confirmPassword: '',
  userType: 'sys_user'
});

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (registerForm.value.password !== value) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const registerRules: ElFormRules = {
  username: [
    { required: true, trigger: 'blur', message: '请输入用户名' },
    { min: 2, max: 20, message: '用户名长度应在 2 到 20 个字符之间', trigger: 'blur' }
  ],
  phonenumber: [
    { required: true, trigger: 'blur', message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, trigger: 'blur', message: '请输入密码' },
    { min: 5, max: 20, message: '密码长度应在 5 到 20 个字符之间', trigger: 'blur' },
    { pattern: /^[^<>"'|\\]+$/, message: '密码不能包含 < > " \' \\ |', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', message: '请再次输入密码' },
    { validator: equalToPassword, trigger: 'blur' }
  ]
};

const loading = ref(false);
const registerRef = ref<ElFormInstance>();

const handleRegister = () => {
  registerRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    loading.value = true;
    const [err] = await to(register(registerForm.value));
    loading.value = false;
    if (!err) {
      await ElMessageBox.alert('注册成功，请使用新账号登录。', '系统提示', {
        app: undefined,
        type: 'success'
      });
      await router.push('/login');
    }
  });
};
</script>

<style lang="scss" scoped>
@use "@/assets/styles/auth-page.scss";
</style>
