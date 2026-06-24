<template>
  <div class="app-card">
    <div class="card-top">
      <div class="icon" :data-accent="app.accent">{{ app.icon }}</div>
      <div class="meta">
        <div class="name">{{ app.appName }}</div>
        <div class="ver">{{ app.version }}</div>
      </div>
      <el-button class="use-btn" size="small" type="primary" @click="onUse">使用</el-button>
    </div>
    <div class="desc">{{ app.description }}</div>
    <div class="tags">
      <el-tag v-for="t in tagList" :key="t" size="small" effect="plain">{{ t }}</el-tag>
    </div>
    <div class="card-foot">
      <span class="stat">使用 {{ app.useCount }} 次</span>
      <span class="stat">推荐 {{ app.recommendCount }} 次</span>
      <span class="icons">
        <i :class="['act', { on: app.favorited }]" title="收藏" @click="onFav">☆</i>
        <i :class="['act', { on: app.recommended }]" title="推荐" @click="onRec">👍</i>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { PortalApp } from '@/api/appcenter/types';
import { useApp, favorite, unfavorite, recommend, unrecommend } from '@/api/appcenter/portal';

const props = defineProps<{ app: PortalApp }>();
const emit = defineEmits<{ (e: 'changed'): void }>();

const tagList = computed(() => (props.app.tags || '').split(',').filter(Boolean));

const onUse = async () => {
  const res: any = await useApp(props.app.appId);
  const url = res.data;
  if (url) window.open(url, '_blank');
  emit('changed');
};

const onFav = async () => {
  props.app.favorited ? await unfavorite(props.app.appId) : await favorite(props.app.appId);
  emit('changed');
};

const onRec = async () => {
  props.app.recommended ? await unrecommend(props.app.appId) : await recommend(props.app.appId);
  emit('changed');
};
</script>

<style scoped>
.app-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.card-top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #1f6feb;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.meta {
  flex: 1;
  min-width: 0;
}
.name {
  font-weight: 600;
}
.ver {
  color: #8a94a6;
  font-size: 12px;
}
.desc {
  color: #5b6577;
  font-size: 13px;
  min-height: 38px;
}
.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.card-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #8a94a6;
  font-size: 12px;
}
.icons {
  margin-left: auto;
  display: flex;
  gap: 12px;
}
.act {
  cursor: pointer;
}
.act.on {
  color: #f59e0b;
}
</style>
