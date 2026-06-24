<template>
  <div class="fav-page">
    <h3 class="page-title">收藏应用</h3>
    <div class="grid">
      <AppCard v-for="a in apps" :key="a.appId" :app="a" @changed="reload" />
    </div>
    <el-empty v-if="apps.length === 0" description="还没有收藏任何应用" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import AppCard from './components/AppCard.vue';
import { listFavorites } from '@/api/appcenter/portal';
import { PortalApp } from '@/api/appcenter/types';

const apps = ref<PortalApp[]>([]);

const reload = async () => {
  try {
    const res: any = await listFavorites({ pageNum: 1, pageSize: 100 });
    apps.value = res.rows || [];
  } catch (e) {
    console.error('加载收藏应用失败', e);
    apps.value = [];
  }
};

onMounted(reload);
</script>

<style scoped>
.fav-page { padding: 24px; }
.page-title { margin: 0 0 16px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1366px) { .grid { grid-template-columns: repeat(3, 1fr); } }
</style>
