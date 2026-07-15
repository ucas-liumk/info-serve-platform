<template>
  <section class="workspace" aria-label="文件信息与阅看记录">
    <header class="workspace-intro"><p>资料元数据与最近阅看记录</p></header>
    <PanelFileInfo
      :items="items"
      :records="model.viewRecords.value"
      :loading="model.loading.records"
      :error="model.errors.records"
      @retry="model.loadViewRecords"
    />
  </section>
</template>

<script setup lang="ts">
import type { ResourceInfoItem } from './panelStudio';
import type { useResourcePreviewNotes } from './useResourcePreviewNotes';
import PanelFileInfo from './PanelFileInfo.vue';

type NotesModel = ReturnType<typeof useResourcePreviewNotes>;
defineProps<{ model: NotesModel; items: readonly ResourceInfoItem[] }>();
</script>

<style scoped src="./resource-preview-panels.scss"></style>
