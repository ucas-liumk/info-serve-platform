<template>
  <AdminModulePlaceholder title="题库管理" description="维护应知应会题目、题型、答案和发布状态。" status="待接入" :items="items" />
</template>

<script setup lang="ts" name="RequiredKnowledgeQuestions">
import AdminModulePlaceholder from '../components/AdminModulePlaceholder.vue';

const items = ['题目列表与筛选', '新增和编辑题目', '题目发布、下架与复核'];
</script>
