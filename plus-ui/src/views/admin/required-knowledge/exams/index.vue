<template>
  <AdminModulePlaceholder title="考试配置" description="配置模拟考试、题量、时长、分值和科目范围。" status="待接入" :items="items" />
</template>

<script setup lang="ts" name="RequiredKnowledgeExams">
import AdminModulePlaceholder from '../components/AdminModulePlaceholder.vue';

const items = ['考试列表与状态管理', '题目抽取规则配置', '考试时长、分值与通过线设置'];
</script>
