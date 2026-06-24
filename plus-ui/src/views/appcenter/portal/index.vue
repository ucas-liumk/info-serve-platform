<template>
  <div class="square">
    <PortalHeader @search="onSearch" />
    <div class="body">
      <CategoryTabs v-model:model="categoryCode" v-model:sort="sort" :categories="categories" />
      <div class="grid">
        <AppCard v-for="a in apps" :key="a.appId" :app="a" @changed="reload" />
      </div>
      <el-empty v-if="!loading && apps.length === 0" description="暂无应用" />
      <el-pagination
        class="pager"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import PortalHeader from '@/layout/portal/components/PortalHeader.vue';
import CategoryTabs from './components/CategoryTabs.vue';
import AppCard from './components/AppCard.vue';
import { listApps, listCategories } from '@/api/appcenter/portal';
import { PortalApp, PortalCategory } from '@/api/appcenter/types';

const apps = ref<PortalApp[]>([]);
const categories = ref<PortalCategory[]>([]);
const categoryCode = ref('all');
const sort = ref('latest');
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const loading = ref(false);

const reload = async () => {
  loading.value = true;
  try {
    const res: any = await listApps({
      categoryCode: categoryCode.value,
      keyword: keyword.value,
      sort: sort.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    apps.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const onSearch = (kw: string) => {
  keyword.value = kw;
  pageNum.value = 1;
  reload();
};

const onPage = (p: number) => {
  pageNum.value = p;
  reload();
};

watch([categoryCode, sort], () => {
  pageNum.value = 1;
  reload();
});

onMounted(async () => {
  const c: any = await listCategories();
  categories.value = c.data || [];
  reload();
});
</script>

<style scoped>
.body {
  padding: 8px 24px 24px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
@media (max-width: 1366px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
