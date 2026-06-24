<template>
  <aside class="portal-sidebar">
    <div class="brand">
      <span class="brand-logo">∞</span>
      <span class="brand-text">开源应用试用平台</span>
    </div>
    <nav class="nav">
      <router-link class="nav-item" to="/appcenter" exact-active-class="active">
        <span class="ico">▦</span> 应用广场
      </router-link>
      <router-link class="nav-item" to="/appcenter/favorites" active-class="active">
        <span class="ico">☆</span> 收藏应用
      </router-link>
      <router-link class="nav-item" to="/appcenter/messages" active-class="active">
        <span class="ico">✉</span> 消息中心
        <span v-if="unread > 0" class="badge">{{ unread }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { unreadCount } from '@/api/appcenter/portal';

const unread = ref(0);

onMounted(async () => {
  const res: any = await unreadCount();
  unread.value = res.data || 0;
});
</script>

<style scoped>
.portal-sidebar {
  width: 220px;
  min-height: 100vh;
  background: linear-gradient(180deg, #0b2a5b, #0a1f44);
  color: #cfe0ff;
  padding: 16px 12px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #fff;
  padding: 8px;
  margin-bottom: 16px;
}
.brand-logo {
  background: #1f6feb;
  border-radius: 8px;
  padding: 2px 8px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  color: #cfe0ff;
  text-decoration: none;
  margin-bottom: 6px;
}
.nav-item.active {
  background: #1f6feb;
  color: #fff;
}
.badge {
  margin-left: auto;
  background: #3b82f6;
  color: #fff;
  border-radius: 10px;
  padding: 0 8px;
  font-size: 12px;
}
</style>
