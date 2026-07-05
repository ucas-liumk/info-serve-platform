<template>
  <el-dialog v-model="visible" width="min(960px, calc(100vw - 32px))" align-center destroy-on-close class="system-manual-dialog" @open="loadManual">
    <template #header>
      <div class="manual-head">
        <span class="manual-head-icon">
          <el-icon><Memo /></el-icon>
        </span>
        <div>
          <strong>系统使用手册</strong>
          <span>来自项目 README.md</span>
        </div>
      </div>
    </template>

    <div class="manual-body">
      <el-alert
        v-if="errorText"
        :title="errorText"
        description="请确认前端静态目录存在 readme.md，或重新执行前端构建同步 README.md。"
        type="error"
        show-icon
        :closable="false"
      />
      <el-skeleton v-else-if="loading" :rows="12" animated />
      <article v-else class="manual-content" v-html="manualHtml"></article>
    </div>

    <template #footer>
      <div class="manual-footer">
        <span>部署后可直接修改前端静态目录中的 readme.md</span>
        <div class="manual-actions">
          <el-button :loading="loading" @click="reloadManual">重新加载</el-button>
          <el-button type="primary" @click="visible = false">关闭</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import MarkdownIt from 'markdown-it';
import { computed, ref } from 'vue';
import { Memo } from '@element-plus/icons-vue';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const manualText = ref('');
const loading = ref(false);
const errorText = ref('');
let requestId = 0;

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true
});

const manualUrl = computed(() => {
  const base = import.meta.env.BASE_URL || '/';
  return `${base.endsWith('/') ? base : `${base}/`}readme.md`;
});

const manualHtml = computed(() => (manualText.value ? sanitizeHtml(markdown.render(manualText.value)) : ''));

const safeHrefPattern = /^(https?:|mailto:|tel:|#|\/|\.\.?\/)/i;
const allowedTags = new Set([
  'a',
  'blockquote',
  'br',
  'code',
  'del',
  'em',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'hr',
  'li',
  'ol',
  'p',
  'pre',
  'strong',
  'table',
  'tbody',
  'td',
  'th',
  'thead',
  'tr',
  'ul'
]);

function sanitizeHtml(html: string) {
  const template = document.createElement('template');
  template.innerHTML = html;

  const sanitizeNode = (node: Node) => {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const element = node as HTMLElement;
      const tag = element.tagName.toLowerCase();

      if (!allowedTags.has(tag)) {
        element.replaceWith(document.createTextNode(element.textContent || ''));
        return;
      }

      for (const attr of [...element.attributes]) {
        const name = attr.name.toLowerCase();
        const value = attr.value.trim();
        const keepHref = tag === 'a' && name === 'href' && safeHrefPattern.test(value);
        const keepTitle = tag === 'a' && name === 'title';
        const keepCodeClass = tag === 'code' && name === 'class';

        if (!keepHref && !keepTitle && !keepCodeClass) {
          element.removeAttribute(attr.name);
        }
      }

      if (tag === 'a') {
        element.setAttribute('target', '_blank');
        element.setAttribute('rel', 'noopener noreferrer');
      }
    }

    for (const child of [...node.childNodes]) {
      sanitizeNode(child);
    }
  };

  sanitizeNode(template.content);
  return template.innerHTML;
}

const reloadManual = async () => {
  const currentRequest = ++requestId;
  loading.value = true;
  errorText.value = '';

  try {
    const response = await fetch(manualUrl.value, { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const text = await response.text();
    if (currentRequest === requestId) {
      manualText.value = text;
    }
  } catch (error) {
    if (currentRequest === requestId) {
      manualText.value = '';
      errorText.value = `手册加载失败：${error instanceof Error ? error.message : '未知错误'}`;
    }
  } finally {
    if (currentRequest === requestId) {
      loading.value = false;
    }
  }
};

const loadManual = () => {
  if (!manualText.value || errorText.value) {
    void reloadManual();
  }
};
</script>

<style scoped>
:global(.system-manual-dialog) {
  border: 1px solid var(--ip-neutral-200);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-lg);
}

:global(.system-manual-dialog .el-dialog__header),
:global(.system-manual-dialog .el-dialog__body),
:global(.system-manual-dialog .el-dialog__footer) {
  background: var(--ip-neutral-0);
}

.manual-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.manual-head-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--ip-primary-50);
  color: var(--ip-primary-600);
}

.manual-head-icon .el-icon {
  font-size: 22px;
}

.manual-head strong {
  display: block;
  color: var(--ip-neutral-900);
  font-size: 18px;
  line-height: 1.35;
}

.manual-head span {
  display: block;
  color: var(--ip-neutral-500);
  font-size: 13px;
  line-height: 1.4;
}

.manual-body {
  max-height: min(68vh, 720px);
  overflow: auto;
  padding-right: 4px;
}

.manual-content {
  color: var(--ip-neutral-800);
  font-size: 15px;
  line-height: 1.78;
}

.manual-content :deep(h1),
.manual-content :deep(h2),
.manual-content :deep(h3),
.manual-content :deep(h4) {
  margin: 24px 0 12px;
  color: var(--ip-primary-900);
  font-weight: 700;
  line-height: 1.35;
}

.manual-content :deep(h1) {
  margin-top: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--ip-neutral-200);
  font-size: 24px;
}

.manual-content :deep(h2) {
  font-size: 20px;
}

.manual-content :deep(h3) {
  font-size: 18px;
}

.manual-content :deep(h4) {
  font-size: 16px;
}

.manual-content :deep(p),
.manual-content :deep(ul),
.manual-content :deep(ol),
.manual-content :deep(blockquote),
.manual-content :deep(table),
.manual-content :deep(pre) {
  margin: 0 0 14px;
}

.manual-content :deep(ul),
.manual-content :deep(ol) {
  padding-left: 22px;
}

.manual-content :deep(a) {
  color: var(--ip-primary-600);
  font-weight: 600;
  text-decoration: none;
}

.manual-content :deep(a:hover) {
  color: var(--ip-primary-700);
  text-decoration: underline;
}

.manual-content :deep(code) {
  padding: 2px 6px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 6px;
  background: var(--ip-neutral-50);
  color: var(--ip-primary-800);
  font-size: 14px;
}

.manual-content :deep(pre) {
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 10px;
  background: var(--ip-neutral-900);
}

.manual-content :deep(pre code) {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ip-neutral-0);
  font-size: 14px;
}

.manual-content :deep(blockquote) {
  padding: 10px 14px;
  border-left: 4px solid var(--ip-primary-300);
  border-radius: 6px;
  background: var(--ip-primary-50);
  color: var(--ip-neutral-700);
}

.manual-content :deep(table) {
  width: 100%;
  display: block;
  overflow-x: auto;
  border-collapse: collapse;
}

.manual-content :deep(th),
.manual-content :deep(td) {
  padding: 10px 12px;
  border: 1px solid var(--ip-neutral-200);
  text-align: left;
}

.manual-content :deep(th) {
  background: var(--ip-primary-50);
  color: var(--ip-primary-900);
}

.manual-content :deep(hr) {
  height: 1px;
  margin: 24px 0;
  border: 0;
  background: var(--ip-neutral-200);
}

.manual-footer {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.manual-footer span {
  color: var(--ip-neutral-500);
  font-size: 13px;
}

.manual-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 640px) {
  .manual-body {
    max-height: 62vh;
  }

  .manual-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .manual-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
