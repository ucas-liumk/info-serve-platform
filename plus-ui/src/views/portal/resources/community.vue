<template>
  <section class="rc-empty" aria-labelledby="rc-empty-title">
    <span class="i-material-symbols:forum-outline-rounded rc-empty__icon" aria-hidden="true" />
    <h2 id="rc-empty-title" class="rc-empty__title">交流互动 · 敬请期待</h2>
    <p class="rc-empty__desc">
      未来在这里分享你的学习笔记与读到的好内容——好内容值得被更多人看见。
      上线后将聚合大家在资料预览中留下的公开笔记。
    </p>
    <el-button type="primary" @click="$router.push('/portal/resources')">先去资料中心看看</el-button>
  </section>
</template>

<script setup lang="ts"></script>

<style scoped>
.rc-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 16px; min-height: 60vh; text-align: center; padding: 48px 24px;
}
.rc-empty__icon { font-size: 64px; color: var(--ip-primary-300); }
.rc-empty__title { font-size: var(--ip-font-title); color: var(--ip-neutral-800); margin: 0; }
.rc-empty__desc { font-size: var(--ip-font-body); color: var(--ip-neutral-500); max-width: 420px; margin: 0; line-height: 1.7; }
</style>
