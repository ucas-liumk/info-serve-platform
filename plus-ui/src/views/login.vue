<template>
  <main class="auth-page auth-page--login" :class="{ 'auth-page--admin': isAdminLogin }">
    <section class="auth-visual" :aria-label="isAdminLogin ? '后台管理登录' : '信息中心数智服务平台'">
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
        <h2>{{ isAdminLogin ? '后台管理登录' : '欢迎登录' }}</h2>
        <p>{{ isAdminLogin ? '信息中心数智服务平台 · 管理端' : '信息中心数智服务平台' }}</p>
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
        <el-checkbox v-model="loginForm.rememberMe">记住账号</el-checkbox>
        <button class="auth-link-button" type="button" @click="showForgetTip">忘记密码?</button>
      </div>

      <el-form-item class="auth-submit">
        <el-button :loading="loading" size="large" type="primary" class="auth-submit-button" @click.prevent="handleLogin">
          <span v-if="!loading">登录</span>
          <span v-else>登录中...</span>
        </el-button>
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
import { ADMIN_BASE_PATH, ADMIN_HOME_PATH, PORTAL_HOME_PATH } from '@/constants/router';

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
const redirect = ref(PORTAL_HOME_PATH);
const loginRef = ref<ElFormInstance>();
const isAdminLogin = computed(() => {
  return redirect.value === '/index' || redirect.value === ADMIN_HOME_PATH || redirect.value.startsWith(`${ADMIN_BASE_PATH}/`);
});

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    const queryRedirect = newRoute.query && newRoute.query.redirect;
    redirect.value = queryRedirect ? decodeURIComponent(queryRedirect) : PORTAL_HOME_PATH;
  },
  { immediate: true }
);

const cacheLoginAccount = () => {
  localStorage.removeItem('password');
  if (loginForm.value.rememberMe) {
    localStorage.setItem('tenantId', String(loginForm.value.tenantId));
    localStorage.setItem('username', String(loginForm.value.username));
    localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
    return;
  }
  localStorage.removeItem('tenantId');
  localStorage.removeItem('username');
  localStorage.removeItem('rememberMe');
};

const handleLogin = () => {
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      cacheLoginAccount();

      const [err] = await to(userStore.login(loginForm.value));
      if (!err) {
        const redirectUrl = redirect.value || PORTAL_HOME_PATH;
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
  const rememberMe = localStorage.getItem('rememberMe');
  localStorage.removeItem('password');
  loginForm.value = {
    tenantId: tenantId === null ? String(loginForm.value.tenantId) : tenantId,
    username: username === null ? '' : username,
    password: '',
    rememberMe: rememberMe === 'true'
  } as LoginData;
};

const getCode = async () => {
  captchaEnabled.value = false;
  loginForm.value.code = '';
  loginForm.value.uuid = '';
};

const showForgetTip = () => {
  ElMessage.info('请联系管理员重置密码');
};

onMounted(() => {
  getLoginData();
  getCode();
});
</script>

<style lang="scss" scoped>
@use "@/assets/styles/auth-page.scss";
</style>
