<template>
  <div>
    <div class="my-demand-head">
      <div>
        <strong>反馈记录</strong>
        <!-- <p>这里只显示你本人提交的反馈，管理员回复后会同步展示。</p> -->
      </div>
      <div class="my-demand-tools">
        <span>共 {{ myDemandTotal }} 条</span>
        <button type="button" @click="loadMyDemands">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </button>
      </div>
    </div>

    <div v-loading="myDemandLoading" class="my-demand-list">
      <article v-for="item in myDemands" :key="item.demandId" class="my-demand-card">
        <header class="my-demand-card-head">
          <div>
            <strong>{{ item.appName }}</strong>
            <span>{{ getDemandTypeText(item.demandType) }}</span>
          </div>
          <el-tag :type="getDemandStatusTag(item.status)">{{ getDemandStatusText(item.status) }}</el-tag>
        </header>
        <p class="my-demand-content">{{ item.content }}</p>
        <div v-if="hasDemandReply(item)" class="my-demand-reply">
          <strong>管理员回复</strong>
          <p>{{ item.handleRemark }}</p>
          <time v-if="item.handledTime">{{ item.handledTime }}</time>
        </div>
        <footer class="my-demand-card-foot">
          <time>提交时间：{{ item.createTime || '-' }}</time>
          <button type="button" @click="removeMyDemand(item)">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </button>
        </footer>
      </article>
      <el-empty v-if="!myDemandLoading && myDemands.length === 0" :image-size="92" description="暂无反馈记录" />
    </div>

    <el-pagination
      v-if="myDemandTotal > myDemandPageSize"
      class="my-demand-pager"
      small
      background
      layout="prev, pager, next"
      :total="myDemandTotal"
      :page-size="myDemandPageSize"
      :current-page="myDemandPageNum"
      @current-change="onMyDemandPage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Refresh } from '@element-plus/icons-vue';
import { listMyDemands, deleteMyDemand } from '@/api/appcenter/portal';
import { PortalDemandItem } from '@/api/appcenter/types';

const props = defineProps<{ active: boolean }>();

const myDemands = ref<PortalDemandItem[]>([]);
const myDemandLoading = ref(false);
const myDemandPageNum = ref(1);
const myDemandPageSize = ref(5);
const myDemandTotal = ref(0);

const getDemandTypeText = (type: PortalDemandItem['demandType']) => {
  if (type === 'new_app') return '希望上架应用';
  if (type === 'suggestion') return '现有应用建议';
  return '需求反馈';
};

const getDemandStatusText = (status: PortalDemandItem['status']) => {
  if (status === '0') return '待处理';
  if (status === '1') return '处理中';
  if (status === '2') return '已处理';
  if (status === '3') return '已关闭';
  return '未知';
};

const getDemandStatusTag = (status: PortalDemandItem['status']) => {
  if (status === '0') return 'warning';
  if (status === '1') return 'primary';
  if (status === '2') return 'success';
  if (status === '3') return 'info';
  return 'info';
};

const hasDemandReply = (item: PortalDemandItem) => Boolean(item.handleRemark?.trim());

const loadMyDemands = async () => {
  myDemandLoading.value = true;
  try {
    const res: any = await listMyDemands({
      pageNum: myDemandPageNum.value,
      pageSize: myDemandPageSize.value
    });
    myDemands.value = res.rows || [];
    myDemandTotal.value = res.total || 0;
  } finally {
    myDemandLoading.value = false;
  }
};

const onMyDemandPage = (page: number) => {
  myDemandPageNum.value = page;
  loadMyDemands();
};

const removeMyDemand = async (item: PortalDemandItem) => {
  try {
    await ElMessageBox.confirm(`确认删除“${item.appName}”这条反馈吗？`, '删除反馈', {
      type: 'warning',
      confirmButtonText: '删 除',
      cancelButtonText: '取 消'
    });
    await deleteMyDemand(item.demandId);
    ElMessage.success('删除成功');
    if (myDemands.value.length === 1 && myDemandPageNum.value > 1) {
      myDemandPageNum.value -= 1;
    }
    await loadMyDemands();
  } catch {
    // 用户取消删除时不提示
  }
};

watch(
  () => props.active,
  (active) => {
    if (active) {
      myDemandPageNum.value = 1;
      loadMyDemands();
    }
  }
);
</script>

<style scoped>
.my-demand-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #dce5ed;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff9ed 0%, #fff 100%);
}

.my-demand-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 15px;
  line-height: 1.2;
  font-weight: 850;
}

.my-demand-head p {
  margin: 6px 0 0;
  color: var(--tool-muted);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 650;
}

.my-demand-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.my-demand-tools > span {
  color: var(--tool-muted);
  font-size: 13px;
  font-weight: 750;
  white-space: nowrap;
}

.my-demand-tools button {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--tool-input-border);
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: var(--tool-primary);
  font-size: 13px;
  font-weight: 750;
  white-space: nowrap;
  cursor: pointer;
}

.my-demand-tools button:hover {
  border-color: #b8c9d9;
  background: #f8fafc;
}

.my-demand-list {
  min-height: 332px;
  max-height: 430px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding: 2px 4px 2px 0;
}

.my-demand-card {
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  padding: 15px 16px;
  background: linear-gradient(180deg, #fff 0%, #fafbf7 100%);
  box-shadow: 0 8px 22px rgba(31, 54, 76, 0.06);
}

.my-demand-card-head,
.my-demand-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.my-demand-card-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 16px;
  line-height: 1.35;
  font-weight: 850;
}

.my-demand-card-head span {
  display: block;
  margin-top: 4px;
  color: #6b7a99;
  font-size: 13px;
  line-height: 1.2;
  font-weight: 650;
}

.my-demand-content {
  margin: 12px 0 0;
  border-left: 3px solid #d7c4a7;
  padding-left: 12px;
  color: var(--tool-text);
  font-size: 14px;
  line-height: 1.65;
  font-weight: 600;
  white-space: pre-wrap;
}

.my-demand-reply {
  margin-top: 12px;
  border: 1px solid #d7c4a7;
  border-radius: 8px;
  padding: 11px 12px;
  background: #f8f3eb;
  color: var(--tool-text);
}

.my-demand-reply strong {
  display: block;
  color: var(--tool-primary);
  font-size: 13px;
  line-height: 1.2;
  font-weight: 850;
}

.my-demand-reply p {
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 650;
  white-space: pre-wrap;
}

.my-demand-reply time {
  display: block;
  margin-top: 7px;
  color: #7c8ca7;
  font-size: 12px;
  line-height: 1.2;
}

.my-demand-card-foot {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--tool-border);
}

.my-demand-card-foot time {
  color: #7c8ca7;
  font-size: 12px;
  line-height: 1.2;
  font-weight: 650;
}

.my-demand-card-foot button {
  height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid #ffd1d1;
  border-radius: 7px;
  padding: 0 12px;
  background: #fff;
  color: #d93026;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}

.my-demand-card-foot button:hover {
  background: #fff6f6;
}

.my-demand-pager {
  margin-top: 14px;
  justify-content: center;
}

@media (max-width: 520px) {
  .my-demand-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .my-demand-tools {
    width: 100%;
    justify-content: space-between;
  }

  .my-demand-card-head,
  .my-demand-card-foot {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
}
</style>
