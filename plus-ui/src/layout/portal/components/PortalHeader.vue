<template>
  <header class="portal-header">
    <div class="heading">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
    </div>
    <div class="header-tools">
      <el-input
        v-if="showSearch"
        v-model="kw"
        class="search"
        :placeholder="searchPlaceholder"
        clearable
        @keyup.enter="emitSearch"
        @clear="emitSearch"
      >
        <template #suffix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <button v-if="isAdmin" class="admin-link" type="button" @click="goAdmin">后台管理</button>
      <span v-if="nickName" class="user">{{ nickName }}</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Search } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { ADMIN_HOME_PATH } from '@/constants/router';

const props = withDefaults(
  defineProps<{
    title: string;
    subtitle?: string;
    eyebrow?: string;
    searchPlaceholder?: string;
    showSearch?: boolean;
    modelValue?: string;
  }>(),
  {
    subtitle: '',
    eyebrow: '信息中心数智服务平台',
    searchPlaceholder: '搜索资料、应用或话题',
    showSearch: true,
    modelValue: ''
  }
);

const emit = defineEmits<{ (e: 'search', kw: string): void; (e: 'update:modelValue', kw: string): void }>();

const router = useRouter();
const userStore = useUserStore();
const kw = ref(props.modelValue);

watch(
  () => props.modelValue,
  (value) => {
    kw.value = value;
  }
);

const nickName = computed(() => userStore.nickname);

const isAdmin = computed(() => {
  const ps: string[] = userStore.permissions || [];
  return ps.includes('*:*:*') || ps.some((p) => p.startsWith('appcenter:') || p.startsWith('infoservice:'));
});

const emitSearch = () => {
  emit('update:modelValue', kw.value);
  emit('search', kw.value);
};
const goAdmin = () => router.push(ADMIN_HOME_PATH);
</script>

<style scoped>
.portal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 28px;
  padding: 30px 40px 20px;
}

.heading {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 7px;
  color: #8a5d1d;
  font-size: 13px;
  font-weight: 900;
}

.heading h1 {
  margin: 0;
  color: #172033;
  font-size: 30px;
  line-height: 1.18;
  font-weight: 900;
  letter-spacing: 0;
}

.subtitle {
  margin: 8px 0 0;
  color: #526078;
  font-size: 15px;
  font-weight: 700;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 5px;
}

.search {
  width: 420px;
  height: 48px;
  --el-input-height: 48px;
  --el-input-border-radius: 8px;
}

:deep(.search .el-input__wrapper) {
  padding: 0 14px 0 18px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 0 0 1px #d5dce8 inset,
    0 10px 26px rgba(30, 45, 76, 0.06);
}

:deep(.search .el-input__inner) {
  color: #1d2a44;
  font-size: 14px;
  font-weight: 700;
}

:deep(.search .el-input__inner::placeholder) {
  color: #7b879a;
}

:deep(.search .el-icon) {
  width: 22px;
  height: 22px;
  color: #40506c;
  font-size: 20px;
}

.admin-link {
  height: 38px;
  padding: 0 12px;
  border: 1px solid rgba(118, 86, 35, 0.2);
  border-radius: 8px;
  background: #fff9ed;
  color: #7a4c13;
  font-weight: 850;
  cursor: pointer;
}

.admin-link:hover {
  background: #f4e2bd;
}

.user {
  max-width: 112px;
  overflow: hidden;
  color: #5f6c80;
  font-size: 14px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1220px) {
  .portal-header {
    flex-direction: column;
    padding-inline: 24px;
  }

  .header-tools {
    width: 100%;
  }

  .search {
    width: min(100%, 520px);
  }
}

@media (max-width: 760px) {
  .portal-header {
    padding: 22px 18px 14px;
  }

  .heading h1 {
    font-size: 24px;
  }

  .header-tools {
    flex-wrap: wrap;
  }
}
</style>
