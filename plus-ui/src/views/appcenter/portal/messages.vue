<template>
  <div class="msg-page">
    <h3 class="page-title">消息中心</h3>
    <el-table :data="rows" @row-click="onRead">
      <el-table-column width="60" label="">
        <template #default="{ row }">
          <el-badge is-dot v-if="row.isRead === '0'" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { listMessages, readMessage } from '@/api/appcenter/portal';
import { PortalMessage } from '@/api/appcenter/types';

const rows = ref<PortalMessage[]>([]);

const reload = async () => {
  try {
    const res: any = await listMessages({ pageNum: 1, pageSize: 50 });
    rows.value = res.rows || [];
  } catch (e) {
    console.error('加载消息列表失败', e);
    rows.value = [];
  }
};

const onRead = async (row: PortalMessage) => {
  if (row.isRead === '0') {
    try {
      await readMessage(row.messageId);
      row.isRead = '1';
    } catch (e) {
      console.error('标记消息已读失败', e);
    }
  }
};

onMounted(reload);
</script>

<style scoped>
.msg-page { padding: 24px; }
.page-title { margin: 0 0 16px; }
</style>
