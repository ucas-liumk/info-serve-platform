<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="popoverWidth"
    trigger="click"
    popper-class="portal-notification-popper"
    @show="onShow"
  >
    <template #reference>
      <button class="portal-notification-btn" type="button" title="通知" aria-label="通知">
        <el-icon><Bell /></el-icon>
        <span v-if="unread > 0" class="portal-notification-badge">{{ badgeText }}</span>
      </button>
    </template>

    <div class="portal-notice-panel">
      <header class="notice-head">
        <div>
          <strong>门户通知</strong>
          <span>版本升级、资源新增、应用上架与论坛反馈</span>
        </div>
        <div class="notice-tools">
          <button v-if="tab === 'history' && total > 0" class="clear-btn" type="button" @click="clearHistory">
            <el-icon><Delete /></el-icon>
            <span>清空历史</span>
          </button>
          <button class="refresh-btn" type="button" @click="loadList">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </button>
        </div>
      </header>

      <el-tabs v-model="tab" class="notice-tabs" @tab-change="changeTab">
        <el-tab-pane label="未读" name="unread" />
        <el-tab-pane label="历史" name="history" />
      </el-tabs>

      <div v-if="!isLoggedIn" class="login-empty">
        <el-empty :image-size="82" description="登录后查看门户通知" />
      </div>

      <div v-else v-loading="loading" class="notice-list">
        <article v-for="item in items" :key="item.messageId" :class="['notice-item', { unread: item.isRead !== '1' }]">
          <span :class="['type-dot', getTypeMeta(item.msgType).className]">
            <el-icon><component :is="getTypeMeta(item.msgType).icon" /></el-icon>
          </span>
          <div class="notice-main">
            <div class="notice-title-row">
              <strong>{{ item.title }}</strong>
              <em>{{ getTypeMeta(item.msgType).label }}</em>
            </div>
            <p>{{ item.content }}</p>
            <time>{{ item.createTime || '-' }}</time>
          </div>
          <div class="notice-actions">
            <button v-if="item.isRead !== '1'" type="button" @click="markRead(item)">已读</button>
            <button v-else class="delete-btn" type="button" @click="removeRead(item)">
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </button>
          </div>
        </article>

        <el-empty v-if="!loading && items.length === 0" :image-size="82" :description="emptyText" />
      </div>

      <el-pagination
        v-if="isLoggedIn && total > pageSize"
        class="notice-pager"
        small
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="changePage"
      />
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useWindowSize } from '@vueuse/core';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Bell, Box, ChatDotRound, Delete, Promotion, Refresh, UploadFilled, Tools } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import {
  clearPortalReadNotifications,
  deletePortalNotification,
  getPortalUnreadCount,
  listPortalNotifications,
  PortalNotification,
  readPortalNotification
} from '@/api/portal/notification';

type NoticeTab = 'unread' | 'history';

const userStore = useUserStore();
const { width: viewportWidth } = useWindowSize();
const visible = ref(false);
const loading = ref(false);
const tab = ref<NoticeTab>('unread');
const pageNum = ref(1);
const pageSize = ref(6);
const total = ref(0);
const unread = ref(0);
const items = ref<PortalNotification[]>([]);
let unreadTimer: number | undefined;

const isLoggedIn = computed(() => Boolean(userStore.token || getToken()));
const badgeText = computed(() => (unread.value > 99 ? '99+' : String(unread.value)));
const popoverWidth = computed(() => Math.max(288, Math.min(500, viewportWidth.value - 32)));
const emptyText = computed(() => (tab.value === 'unread' ? '暂无未读通知' : '暂无历史通知'));

const typeMeta = {
  version: { label: '版本', className: 'version', icon: Promotion },
  resource: { label: '资源', className: 'resource', icon: UploadFilled },
  app: { label: '应用', className: 'app', icon: Tools },
  forum: { label: '论坛', className: 'forum', icon: ChatDotRound },
  demand: { label: '反馈', className: 'demand', icon: ChatDotRound },
  system: { label: '系统', className: 'system', icon: Box }
};

const getTypeMeta = (type?: string) => typeMeta[type as keyof typeof typeMeta] || typeMeta.system;

const loadUnread = async () => {
  if (!isLoggedIn.value) {
    unread.value = 0;
    return;
  }
  try {
    const res: any = await getPortalUnreadCount();
    unread.value = Number(res.data || 0);
  } catch {
    unread.value = 0;
  }
};

const loadList = async () => {
  if (!isLoggedIn.value) {
    items.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res: any = await listPortalNotifications({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      isRead: tab.value === 'unread' ? '0' : '1'
    });
    items.value = res.rows || res.data || [];
    total.value = Number(res.total || 0);
    await loadUnread();
  } finally {
    loading.value = false;
  }
};

const onShow = () => {
  pageNum.value = 1;
  void loadList();
};

const changeTab = () => {
  pageNum.value = 1;
  void loadList();
};

const changePage = (page: number) => {
  pageNum.value = page;
  void loadList();
};

const markRead = async (item: PortalNotification) => {
  await readPortalNotification(item.messageId);
  if (items.value.length === 1 && pageNum.value > 1) {
    pageNum.value -= 1;
  }
  await loadList();
};

const removeRead = async (item: PortalNotification) => {
  try {
    await ElMessageBox.confirm(`确认删除“${item.title}”这条历史通知吗？`, '删除通知', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    await deletePortalNotification(item.messageId);
    ElMessage.success('通知已删除');
    if (items.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1;
    }
    await loadList();
  } catch {
    // 用户取消删除时不提示
  }
};

const clearHistory = async () => {
  try {
    await ElMessageBox.confirm('确认清空全部历史通知吗？清空后不可恢复。', '清空历史通知', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    });
    await clearPortalReadNotifications();
    ElMessage.success('历史通知已清空');
    pageNum.value = 1;
    await loadList();
  } catch {
    // 用户取消清空时不提示
  }
};

onMounted(() => {
  void loadUnread();
  unreadTimer = window.setInterval(loadUnread, 60000);
});

onBeforeUnmount(() => {
  if (unreadTimer) {
    window.clearInterval(unreadTimer);
  }
});
</script>

<style scoped>
.portal-notification-btn {
  position: relative;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--ip-neutral-300) 72%, transparent);
  border-radius: var(--ip-radius-full);
  background: color-mix(in srgb, var(--ip-neutral-0) 52%, transparent);
  color: var(--ip-neutral-600);
  cursor: pointer;
  transition:
    border-color var(--ip-motion-fast) var(--ip-motion-ease),
    background var(--ip-motion-fast) var(--ip-motion-ease),
    color var(--ip-motion-fast) var(--ip-motion-ease),
    box-shadow var(--ip-motion-fast) var(--ip-motion-ease);
}

.portal-notification-btn:hover,
.portal-notification-btn:focus-visible {
  border-color: var(--ip-primary-200);
  background: color-mix(in srgb, var(--ip-neutral-0) 78%, transparent);
  color: var(--ip-primary-700);
}

.portal-notification-btn:focus-visible {
  outline: none;
  box-shadow: var(--ip-focus-ring);
}

.portal-notification-btn .el-icon {
  font-size: 19px;
}

.portal-notification-badge {
  position: absolute;
  top: 6px;
  right: 5px;
  min-width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
  border-radius: 999px;
  padding: 0 4px;
  background: #d93026;
  color: #fff;
  font-size: 11px;
  line-height: 1;
  font-weight: 900;
}

.portal-notice-panel {
  color: #14243a;
  font-family: 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #dce5ed;
}

.notice-head strong {
  display: block;
  color: #14243a;
  font-size: 17px;
  line-height: 1.2;
  font-weight: 900;
}

.notice-head span {
  display: block;
  margin-top: 4px;
  color: #68788c;
  font-size: 13px;
  line-height: 1.2;
  font-weight: 650;
}

.notice-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.refresh-btn,
.clear-btn {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d3dee8;
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: #245f8f;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}

.clear-btn {
  border-color: #ffd1d1;
  color: #d93026;
}

.notice-tabs {
  margin-top: 8px;
}

:deep(.notice-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.notice-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background: #dce5ed;
}

.login-empty,
.notice-list {
  min-height: 130px;
}

.notice-list {
  max-height: 430px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  padding-top: 12px;
}

.notice-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dce5ed;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
}

.notice-item.unread {
  border-color: #b8c9d9;
  background: #f8fafc;
}

.type-dot {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #eef4fb;
  color: #245f8f;
}

.type-dot.version {
  background: #fff3dc;
  color: #b7791f;
}

.type-dot.resource {
  background: #e7f4f0;
  color: #2f8a7a;
}

.type-dot.app {
  background: #eaf2f8;
  color: #245f8f;
}

.type-dot.forum,
.type-dot.demand {
  background: #edf4ff;
  color: #2f65c8;
}

.notice-main {
  min-width: 0;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-title-row strong {
  min-width: 0;
  color: #14243a;
  font-size: 15px;
  line-height: 1.35;
  font-weight: 900;
}

.notice-title-row em {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 2px 7px;
  background: #eef4fb;
  color: #58708c;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.notice-main p {
  margin: 7px 0 0;
  color: #4c5f86;
  font-size: 13px;
  line-height: 1.5;
  font-weight: 600;
}

.notice-main time {
  display: block;
  margin-top: 7px;
  color: #96a1af;
  font-size: 12px;
  line-height: 1.2;
  font-weight: 600;
}

.notice-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-actions button {
  height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid #b8c9d9;
  border-radius: 7px;
  padding: 0 10px;
  background: #fff;
  color: #245f8f;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}

.notice-actions .delete-btn {
  border-color: #ffd1d1;
  color: #d93026;
}

.notice-pager {
  margin-top: 12px;
  justify-content: center;
}

:global(.portal-notification-popper) {
  max-width: calc(100vw - 32px);
  box-sizing: border-box;
  border: 1px solid #d3dee8 !important;
  border-radius: 12px !important;
  padding: 14px !important;
  box-shadow: 0 18px 46px rgba(31, 54, 76, 0.16) !important;
}

@media (prefers-reduced-motion: reduce) {
  .portal-notification-btn {
    transition: none;
  }
}

@media (max-width: 840px) {
  .portal-notification-btn {
    width: 44px;
    height: 44px;
    flex-basis: 44px;
  }
}

@media (max-width: 760px) {
  .notice-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .notice-tools {
    width: 100%;
    justify-content: flex-end;
  }

  .notice-item {
    grid-template-columns: 34px minmax(0, 1fr);
  }

  .notice-actions {
    grid-column: 2;
  }
}
</style>
