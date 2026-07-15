<template>
  <div
    ref="workbenchRef"
    class="preview-workbench"
    :class="{ 'is-open': panel.view, 'is-overlay': overlayMode, 'is-mobile': mobileMode, 'is-resizing': resizing }"
    :style="workbenchStyle"
  >
    <button v-if="panel.view && overlayMode" class="drawer-backdrop" type="button" aria-label="关闭功能区" @click="closeWorkspace"></button>
    <aside
      v-if="panel.view"
      ref="drawerRef"
      class="workspace-drawer"
      :role="mobileMode ? 'dialog' : 'complementary'"
      :aria-modal="mobileMode ? 'true' : undefined"
      :aria-labelledby="`preview-${panel.view}-drawer-title`"
      tabindex="-1"
      @keydown="handleDrawerKeydown"
    >
      <div
        v-if="!overlayMode"
        class="resize-handle"
        role="separator"
        aria-orientation="vertical"
        aria-label="拖拽调节功能区宽度"
        title="拖拽调宽，双击复位"
        @pointerdown="startResize"
        @dblclick="resetPanelWidth"
      ></div>
      <header class="drawer-header">
        <div>
          <span class="drawer-kicker">阅读工作台</span><strong :id="`preview-${panel.view}-drawer-title`">{{ activeTile?.name }}</strong>
        </div>
        <button ref="drawerCloseRef" class="drawer-close" type="button" aria-label="关闭功能区" @click="closeWorkspace"><IconClose /></button>
      </header>
      <div class="drawer-content">
        <ResourcePreviewNotesPanel v-if="panel.view === 'note'" :model="notesModel" />
        <ResourcePreviewDiscussionPanel v-else-if="panel.view === 'chat'" :model="notesModel" />
        <ResourcePreviewInfoPanel v-else :model="notesModel" :items="infoItems" />
      </div>
    </aside>

    <nav class="workspace-rail" aria-label="文件辅助功能">
      <button
        v-for="tile in tiles"
        :key="tile.key"
        :ref="(element) => setTriggerRef(tile.key, element)"
        class="rail-button"
        :class="{ 'is-active': panel.view === tile.key }"
        type="button"
        :aria-label="tile.name"
        :aria-pressed="panel.view === tile.key"
        :title="tile.hint"
        @click="selectTile(tile)"
      >
        <IconNote v-if="tile.key === 'note'" />
        <IconChat v-else-if="tile.key === 'chat'" />
        <IconInfo v-else />
        <span>{{ tile.name }}</span>
      </button>
      <button v-if="!mobileMode && panel.view" class="rail-collapse" type="button" aria-label="收起功能区" @click="closeWorkspace">
        <IconCollapse />
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import type { CSSProperties, ComponentPublicInstance } from 'vue';
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, toRef, watch } from 'vue';
import IconChat from '~icons/material-symbols/forum-outline-rounded';
import IconClose from '~icons/material-symbols/close-rounded';
import IconCollapse from '~icons/material-symbols/keyboard-double-arrow-right-rounded';
import IconInfo from '~icons/material-symbols/info-outline-rounded';
import IconNote from '~icons/material-symbols/edit-note-outline-rounded';
import type { InfoResource } from '@/api/infoservice/types';
import ResourcePreviewDiscussionPanel from './ResourcePreviewDiscussionPanel.vue';
import ResourcePreviewInfoPanel from './ResourcePreviewInfoPanel.vue';
import ResourcePreviewNotesPanel from './ResourcePreviewNotesPanel.vue';
import type { StudioTile, WorkspaceKey } from './panelStudio';
import {
  buildResourceInfoItems,
  clampPanelWidth,
  DEFAULT_PANEL_STATE,
  DEFAULT_PANEL_WIDTH,
  reducePanelState,
  shouldOverlayPanel,
  STUDIO_TILES
} from './panelStudio';
import { useResourcePreviewNotes } from './useResourcePreviewNotes';

const props = defineProps<{ resource?: InfoResource; resourceId: string }>();
const panel = ref(DEFAULT_PANEL_STATE);
const tiles = STUDIO_TILES;
const activeTile = computed(() => tiles.find((tile) => tile.key === panel.value.view));
const infoItems = computed(() => buildResourceInfoItems(props.resource));
const notesModel = useResourcePreviewNotes(toRef(props, 'resourceId'));

const viewportWidth = ref(window.innerWidth);
const workbenchRef = ref<HTMLElement>();
const drawerRef = ref<HTMLElement>();
const drawerCloseRef = ref<HTMLButtonElement>();
const triggerRefs = reactive(new Map<WorkspaceKey, HTMLElement>());
const panelWidth = ref(DEFAULT_PANEL_WIDTH);
const resizing = ref(false);
const overlayMode = computed(() => shouldOverlayPanel(viewportWidth.value));
const mobileMode = computed(() => viewportWidth.value < 768);
const effectiveWidth = computed(() => clampPanelWidth(panelWidth.value, viewportWidth.value));
const workbenchStyle = computed<CSSProperties>(() => ({ '--preview-drawer-width': `${effectiveWidth.value}px` }) as CSSProperties);
const PANEL_WIDTH_STORAGE_KEY = 'ip-preview-panel-width';

const setTriggerRef = (key: WorkspaceKey, element: Element | ComponentPublicInstance | null) => {
  if (element instanceof HTMLElement) triggerRefs.set(key, element);
};

const loadWorkspace = (view: WorkspaceKey) => {
  if (view === 'note' && !notesModel.loaded.my) notesModel.loadMyNotes();
  if (view === 'chat' && !notesModel.loaded.public) notesModel.loadPublicNotes();
  if (view === 'info' && !notesModel.loaded.records) notesModel.loadViewRecords();
};

const focusDrawer = async () => {
  if (!mobileMode.value || !panel.value.view) return;
  await nextTick();
  drawerCloseRef.value?.focus();
};

const selectTile = (tile: StudioTile) => {
  const wasOpen = panel.value.view;
  panel.value = reducePanelState(panel.value, { type: 'clickTile', tile });
  if (panel.value.view) {
    loadWorkspace(panel.value.view);
    focusDrawer();
  } else if (wasOpen) {
    nextTick(() => triggerRefs.get(wasOpen)?.focus());
  }
};

const closeWorkspace = () => {
  const previous = panel.value.view;
  panel.value = reducePanelState(panel.value, { type: 'closeWorkspace' });
  if (previous) nextTick(() => triggerRefs.get(previous)?.focus());
};

const quoteSelection = (text: string) => {
  panel.value = reducePanelState(panel.value, { type: 'openWorkspace', view: 'note' });
  loadWorkspace('note');
  notesModel.quoteSelection(text);
  focusDrawer();
};

defineExpose({ quoteSelection });

const persistPanelWidth = () => {
  try {
    window.localStorage.setItem(PANEL_WIDTH_STORAGE_KEY, String(panelWidth.value));
  } catch {
    /* 不持久化 */
  }
};

const startResize = (event: PointerEvent) => {
  const handle = event.currentTarget as HTMLElement;
  const startX = event.clientX;
  const startWidth = effectiveWidth.value;
  resizing.value = true;
  handle.setPointerCapture(event.pointerId);
  const onMove = (moveEvent: PointerEvent) => {
    panelWidth.value = clampPanelWidth(startWidth + startX - moveEvent.clientX, viewportWidth.value);
  };
  const stop = () => {
    resizing.value = false;
    persistPanelWidth();
    handle.removeEventListener('pointermove', onMove);
    handle.removeEventListener('pointerup', stop);
    handle.removeEventListener('pointercancel', stop);
  };
  handle.addEventListener('pointermove', onMove);
  handle.addEventListener('pointerup', stop);
  handle.addEventListener('pointercancel', stop);
  event.preventDefault();
};

const resetPanelWidth = () => {
  panelWidth.value = DEFAULT_PANEL_WIDTH;
  persistPanelWidth();
};

const handleDrawerKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    event.preventDefault();
    closeWorkspace();
    return;
  }
  if (!mobileMode.value || event.key !== 'Tab' || !drawerRef.value) return;
  const focusable = [
    ...drawerRef.value.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
    )
  ];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  }
  if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
};

const handleResize = () => {
  viewportWidth.value = window.innerWidth;
};

watch(
  () => mobileMode.value && Boolean(panel.value.view),
  (locked) => {
    document.body.style.overflow = locked ? 'hidden' : '';
  }
);

watch(
  () => props.resourceId,
  () => {
    panel.value = DEFAULT_PANEL_STATE;
  }
);

onMounted(() => {
  window.addEventListener('resize', handleResize);
  try {
    panelWidth.value = clampPanelWidth(Number(window.localStorage.getItem(PANEL_WIDTH_STORAGE_KEY)) || DEFAULT_PANEL_WIDTH, window.innerWidth);
  } catch {
    panelWidth.value = DEFAULT_PANEL_WIDTH;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  document.body.style.overflow = '';
});
</script>

<style scoped src="./resource-preview-workbench.scss"></style>
