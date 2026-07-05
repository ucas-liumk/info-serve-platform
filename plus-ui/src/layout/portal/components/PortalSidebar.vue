<template>
  <aside class="portal-sidebar">
    <div class="brand">
      <span class="brand-logo" aria-hidden="true">
        <span class="logo-cell"></span>
        <span class="logo-cell"></span>
        <span class="logo-cell"></span>
        <span class="logo-cell"></span>
      </span>
      <span class="brand-text">信息中心<br />数智服务平台</span>
    </div>
    <nav class="nav">
      <router-link class="nav-item" to="/portal" exact-active-class="active">
        <el-icon class="ico"><HomeFilled /></el-icon>
        <span>服务概览</span>
      </router-link>
      <router-link class="nav-item" to="/portal/resources" active-class="active">
        <el-icon class="ico"><FolderOpened /></el-icon>
        <span>资料共享</span>
      </router-link>
      <router-link class="nav-item" to="/portal/tools" active-class="active">
        <el-icon class="ico"><Tools /></el-icon>
        <span>应用中心</span>
      </router-link>
      <router-link class="nav-item" to="/portal/forum" active-class="active">
        <el-icon class="ico"><ChatDotRound /></el-icon>
        <span>服务论坛</span>
      </router-link>
    </nav>
    <div class="sidebar-note">
      <span>一期上线范围</span>
      <strong>资料 · 应用 · 论坛</strong>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ChatDotRound, FolderOpened, HomeFilled, Tools } from '@element-plus/icons-vue';
</script>

<style scoped>
.portal-sidebar {
  position: sticky;
  top: 0;
  width: 276px;
  flex: 0 0 276px;
  min-height: 100vh;
  overflow: hidden;
  background:
    linear-gradient(180deg, #17304e 0%, #10253e 54%, #0d1a2d 100%),
    linear-gradient(135deg, rgba(214, 154, 55, 0.2), transparent 44%);
  color: #dce7f4;
  padding: 30px 18px 24px;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.08), 18px 0 44px rgba(19, 34, 57, 0.08);
}

.portal-sidebar::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, transparent 0 56%, rgba(232, 183, 88, 0.11) 57%, transparent 59%),
    linear-gradient(218deg, transparent 0 68%, rgba(93, 174, 159, 0.13) 69%, transparent 71%);
  pointer-events: none;
}

.brand {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 13px;
  color: #fff;
  padding: 6px 4px 34px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  flex: 0 0 44px;
  padding: 7px;
  border-radius: 8px;
  background: linear-gradient(145deg, #fff7e6, #ead2a0);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.16);
}

.logo-cell {
  border-radius: 4px;
  background: #17304e;
}

.logo-cell:nth-child(2) {
  background: #5aa69d;
}

.logo-cell:nth-child(3) {
  background: #d69a37;
}

.brand-text {
  font-size: 20px;
  line-height: 1.25;
  font-weight: 900;
  white-space: nowrap;
}

.nav {
  position: relative;
  z-index: 2;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 60px;
  padding: 0 17px;
  border-radius: 8px;
  color: #e4edf7;
  text-decoration: none;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 850;
  transition: transform 0.16s ease, background 0.16s ease, color 0.16s ease;
}

.nav-item:hover {
  transform: translateX(2px);
  background: rgba(255, 255, 255, 0.07);
}

.nav-item.active {
  background: linear-gradient(135deg, #f2d398 0%, #c98b2e 100%);
  color: #172033;
  box-shadow: 0 16px 34px rgba(117, 75, 20, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.52);
}

.ico {
  width: 22px;
  height: 22px;
  font-size: 22px;
}

.sidebar-note {
  position: absolute;
  z-index: 2;
  left: 18px;
  right: 18px;
  bottom: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
}

.sidebar-note span {
  color: #b7c6d8;
  font-size: 12px;
  font-weight: 800;
}

.sidebar-note strong {
  color: #fff0c8;
  font-size: 16px;
  font-weight: 900;
}

@media (max-width: 980px) {
  .portal-sidebar {
    position: relative;
    width: 100%;
    min-height: auto;
    flex-basis: auto;
    padding: 14px 16px;
  }

  .brand {
    padding: 0 0 12px;
  }

  .nav {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  .nav-item {
    justify-content: center;
    margin: 0;
    padding: 0 8px;
  }

  .sidebar-note {
    display: none;
  }
}

@media (max-width: 640px) {
  .brand-text {
    font-size: 17px;
  }

  .nav {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
