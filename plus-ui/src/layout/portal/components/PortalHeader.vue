<template>
  <header class="portal-header">
    <div class="title">应用广场</div>
    <el-input v-model="kw" class="search" placeholder="搜索应用名称、描述..." clearable @keyup.enter="emitSearch" @clear="emitSearch" />
    <div class="right">
      <el-button v-if="isAdmin" link type="primary" @click="goAdmin">后台管理</el-button>
      <span class="user">{{ nickName }}</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';

const emit = defineEmits<{ (e: 'search', kw: string): void }>();

const router = useRouter();
const userStore = useUserStore();
const kw = ref('');

// User store uses 'nickname' (lowercase), not 'nickName'
const nickName = computed(() => userStore.nickname);

// Show admin entry if user has super admin or any appcenter management permission
const isAdmin = computed(() => {
  const ps: string[] = userStore.permissions || [];
  return ps.includes('*:*:*') || ps.some((p) => p.startsWith('appcenter:'));
});

const emitSearch = () => emit('search', kw.value);
const goAdmin = () => router.push('/index');
</script>

<style scoped>
.portal-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.search {
  max-width: 380px;
  margin-left: 8px;
}
.right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  color: #5b6577;
}
</style>
