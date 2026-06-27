<template>
  <main class="auth-page auth-page--login">
    <section class="auth-visual" aria-label="信息中心数智服务平台">
      <div class="auth-brand">
        <img class="auth-brand__logo" :src="logoUrl" alt="信息中心数智服务平台" />
        <div>
          <h1>信息中心数智服务平台</h1>
          <p>数智驱动 · 高效服务 · 共创共享</p>
        </div>
      </div>
    </section>

    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="auth-card auth-card--login">
      <div class="auth-card__heading">
        <h2>欢迎登录</h2>
        <p>信息中心数智服务平台</p>
      </div>

      <el-form-item prop="username" class="auth-form-item">
        <el-input v-model="loginForm.username" type="text" size="large" autocomplete="off" placeholder="请输入用户名">
          <template #prefix>
            <el-icon class="auth-input-icon"><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password" class="auth-form-item">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          autocomplete="off"
          placeholder="请输入密码"
          show-password
          @keyup.enter="handleLogin"
        >
          <template #prefix>
            <el-icon class="auth-input-icon"><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <div class="auth-options">
        <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
        <button class="auth-link-button" type="button" @click="showForgetTip">忘记密码?</button>
      </div>

      <el-form-item class="auth-submit">
        <el-button :loading="loading" size="large" type="primary" class="auth-submit-button" @click.prevent="handleLogin">
          <span v-if="!loading">登 录</span>
          <span v-else>登录中...</span>
        </el-button>

        <template v-if="register">
          <div class="auth-divider"><span>或</span></div>
          <div class="auth-switch-line">
            <span>还没有账号?</span>
            <router-link :to="'/register'">立即注册</router-link>
          </div>
        </template>
      </el-form-item>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue';
import { to } from 'await-to-js';
import { LoginData } from '@/api/types';
import { useUserStore } from '@/store/modules/user';
import logoUrl from '@/assets/portal/home-logo.png';

const userStore = useUserStore();
const router = useRouter();

const loginForm = ref<LoginData>({
  tenantId: '000000',
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
} as LoginData);

const loginRules: ElFormRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入用户名' }],
  password: [{ required: true, trigger: 'blur', message: '请输入密码' }]
};

const loading = ref(false);
const captchaEnabled = ref(false);
const register = ref(true);
const redirect = ref('/portal');
const loginRef = ref<ElFormInstance>();

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    const queryRedirect = newRoute.query && newRoute.query.redirect;
    redirect.value = queryRedirect ? decodeURIComponent(queryRedirect) : '/portal';
  },
  { immediate: true }
);

const handleLogin = () => {
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      if (loginForm.value.rememberMe) {
        localStorage.setItem('tenantId', String(loginForm.value.tenantId));
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('password', String(loginForm.value.password));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        localStorage.removeItem('tenantId');
        localStorage.removeItem('username');
        localStorage.removeItem('password');
        localStorage.removeItem('rememberMe');
      }

      const [err] = await to(userStore.login(loginForm.value));
      if (!err) {
        const redirectUrl = redirect.value || '/portal';
        await router.push(redirectUrl);
        loading.value = false;
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          await getCode();
        }
      }
    } else {
      console.log('error submit!', fields);
    }
  });
};

const getLoginData = () => {
  const tenantId = localStorage.getItem('tenantId');
  const username = localStorage.getItem('username');
  const password = localStorage.getItem('password');
  const rememberMe = localStorage.getItem('rememberMe');
  loginForm.value = {
    tenantId: tenantId === null ? String(loginForm.value.tenantId) : tenantId,
    username: username === null ? '' : username,
    password: password === null ? '' : String(password),
    rememberMe: rememberMe === 'true'
  } as LoginData;
};

const getCode = async () => {
  captchaEnabled.value = false;
  loginForm.value.code = '';
  loginForm.value.uuid = '';
};

const showForgetTip = () => {
  ElMessage.info('第一版本暂未开放找回密码');
};

onMounted(() => {
  getLoginData();
  getCode();
});
</script>

<style lang="scss" scoped>
@use "@/assets/styles/auth-page.scss";
</style>
