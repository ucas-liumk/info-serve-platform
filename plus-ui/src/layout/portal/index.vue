<template>
  <div class="portal-layout">
    <PortalSidebar />
    <section class="portal-main">
      <router-view />
    </section>
  </div>
</template>

<script setup lang="ts">
import PortalSidebar from './components/PortalSidebar.vue';
</script>

<style scoped>
.portal-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fb;
}
.portal-main {
  flex: 1;
  min-width: 0;
}
</style>
