<template>
  <AdminModulePlaceholder title="OCR 导入" description="从 PDF、截图等材料识别题目草稿，复核后进入题库。" status="待接入" :items="items" />
</template>

<script setup lang="ts" name="RequiredKnowledgeOcr">
import AdminModulePlaceholder from '../components/AdminModulePlaceholder.vue';

const items = ['材料上传与识别任务', '识别结果转题目草稿', '人工复核后入库发布'];
</script>
