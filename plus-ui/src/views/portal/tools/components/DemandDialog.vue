<template>
  <el-dialog v-model="visible" class="portal-demand-dialog" width="760px" append-to-body align-center :close-on-click-modal="false">
    <template #header>
      <div class="demand-dialog-head">
        <span class="demand-dialog-icon">
          <el-icon><DocumentAdd /></el-icon>
        </span>
        <div>
          <strong>需求反馈</strong>
          <p>提交希望上架的应用，或反馈现有应用的改进建议。</p>
        </div>
      </div>
    </template>

    <el-tabs v-model="demandActiveTab" class="demand-tabs">
      <el-tab-pane label="提交反馈" name="submit">
        <el-form ref="demandFormRef" :model="demandForm" :rules="demandRules" label-width="86px" class="portal-demand-form">
          <el-form-item label="需求类型" prop="demandType">
            <el-radio-group v-model="demandForm.demandType" class="demand-type-group" @change="onDemandTypeChange">
              <el-radio-button value="new_app">
                <span class="demand-type-option">
                  <strong>希望上架应用</strong>
                  <em>补充新的内部工具</em>
                </span>
              </el-radio-button>
              <el-radio-button value="suggestion">
                <span class="demand-type-option">
                  <strong>现有应用建议</strong>
                  <em>优化已有工具体验</em>
                </span>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="demandAppNameLabel" :prop="demandAppNameProp">
            <el-input
              v-if="demandForm.demandType === 'new_app'"
              v-model="demandForm.appName"
              :placeholder="demandAppNamePlaceholder"
              maxlength="100"
              show-word-limit
            />
            <el-select
              v-else
              v-model="demandForm.appId"
              class="suggestion-app-select"
              filterable
              remote
              clearable
              remote-show-suffix
              reserve-keyword
              teleported
              popper-class="portal-app-select-popper"
              :remote-method="searchSuggestionApps"
              :loading="suggestionAppLoading"
              :placeholder="demandAppNamePlaceholder"
              no-match-text="未找到已上线应用"
              no-data-text="请输入应用名称查询"
              @change="onSuggestionAppChange"
              @visible-change="onSuggestionAppVisibleChange"
            >
              <el-option v-for="app in suggestionApps" :key="app.appId" :label="app.appName" :value="app.appId">
                <div class="suggestion-app-option">
                  <strong>{{ app.appName }}</strong>
                  <span>{{ app.categoryName || '未分类' }} · {{ app.version || 'latest' }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="需求说明" prop="content">
            <el-input
              v-model="demandForm.content"
              type="textarea"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="请描述你想解决的问题、使用场景，或希望改进的地方"
            />
          </el-form-item>
          <el-form-item label="联系方式" prop="contact">
            <el-input v-model="demandForm.contact" placeholder="选填，便于管理员进一步沟通" maxlength="100" show-word-limit />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="我的反馈" name="mine">
        <MyDemandList :active="demandActiveTab === 'mine'" />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <div v-if="demandActiveTab === 'submit'" class="dialog-footer">
        <el-button @click="visible = false">取 消</el-button>
        <el-button type="primary" :loading="demandSubmitting" @click="submitDemandForm">提 交</el-button>
      </div>
      <div v-else class="dialog-footer">
        <el-button @click="visible = false">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { DocumentAdd } from '@element-plus/icons-vue';
import MyDemandList from './MyDemandList.vue';
import { listApps, submitDemand } from '@/api/portal/appcenter';
import { PortalApp, PortalDemandForm } from '@/api/appcenter/types';

const visible = ref(false);
const demandActiveTab = ref<'submit' | 'mine'>('submit');
const demandSubmitting = ref(false);
const demandFormRef = ref<FormInstance>();
const suggestionApps = ref<PortalApp[]>([]);
const suggestionAppLoading = ref(false);
let suggestionAppSearchSeq = 0;

const createDemandForm = (): PortalDemandForm => ({
  demandType: 'new_app',
  appId: undefined,
  appName: '',
  content: '',
  contact: ''
});

const demandForm = ref<PortalDemandForm>(createDemandForm());

const validateDemandAppName = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (demandForm.value.demandType === 'suggestion') {
    callback();
    return;
  }
  if (value?.trim()) {
    callback();
    return;
  }
  callback(new Error('请输入应用名称'));
};

const validateSuggestionApp = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  if (demandForm.value.demandType !== 'suggestion') {
    callback();
    return;
  }
  if (value) {
    callback();
    return;
  }
  callback(new Error('请选择已上线应用'));
};

const demandRules: FormRules<PortalDemandForm> = {
  demandType: [{ required: true, message: '请选择需求类型', trigger: 'change' }],
  appId: [{ validator: validateSuggestionApp, trigger: 'change' }],
  appName: [{ validator: validateDemandAppName, trigger: 'blur' }],
  content: [{ required: true, message: '请输入需求说明', trigger: 'blur' }]
};

const demandAppNameLabel = computed(() => (demandForm.value.demandType === 'new_app' ? '应用名称' : '相关应用'));
const demandAppNameProp = computed(() => (demandForm.value.demandType === 'suggestion' ? 'appId' : 'appName'));

const demandAppNamePlaceholder = computed(() =>
  demandForm.value.demandType === 'new_app' ? '请输入希望上架的应用名称' : '请输入应用名称搜索并选择已上线应用'
);

const searchSuggestionApps = async (query = '') => {
  const seq = ++suggestionAppSearchSeq;
  suggestionAppLoading.value = true;
  try {
    const res: any = await listApps({
      categoryCode: 'all',
      keyword: query.trim(),
      sort: 'latest',
      pageNum: 1,
      pageSize: 20
    });
    if (seq === suggestionAppSearchSeq) {
      suggestionApps.value = res.rows || res.data || [];
    }
  } finally {
    if (seq === suggestionAppSearchSeq) {
      suggestionAppLoading.value = false;
    }
  }
};

const onSuggestionAppVisibleChange = (visibleState: boolean) => {
  if (visibleState && demandForm.value.demandType === 'suggestion' && suggestionApps.value.length === 0) {
    void searchSuggestionApps('');
  }
};

const onSuggestionAppChange = (appId?: number) => {
  const selected = suggestionApps.value.find((app) => app.appId === appId);
  demandForm.value.appName = selected?.appName || '';
  if (!appId) {
    demandForm.value.appName = '';
  }
  void demandFormRef.value?.validateField('appId');
};

const onDemandTypeChange = () => {
  demandForm.value.appId = undefined;
  demandForm.value.appName = '';
  demandFormRef.value?.clearValidate(['appId', 'appName']);
  if (demandForm.value.demandType === 'suggestion') {
    void searchSuggestionApps('');
  }
};

const resetDemandForm = () => {
  demandForm.value = createDemandForm();
  demandFormRef.value?.clearValidate();
};

const open = () => {
  resetDemandForm();
  demandActiveTab.value = 'submit';
  visible.value = true;
};

const submitDemandForm = async () => {
  const formEl = demandFormRef.value;
  if (!formEl) return;
  const valid = await formEl.validate().catch(() => false);
  if (!valid) return;

  demandSubmitting.value = true;
  try {
    const selectedSuggestionApp = suggestionApps.value.find((app) => app.appId === demandForm.value.appId);
    const appName =
      demandForm.value.demandType === 'suggestion'
        ? selectedSuggestionApp?.appName || demandForm.value.appName.trim()
        : demandForm.value.appName.trim();
    const payload: PortalDemandForm = {
      demandType: demandForm.value.demandType,
      appName,
      content: demandForm.value.content.trim()
    };
    if (demandForm.value.demandType === 'suggestion') {
      payload.appId = demandForm.value.appId;
    }
    const contact = demandForm.value.contact?.trim();
    if (contact) payload.contact = contact;
    await submitDemand(payload);
    ElMessage.success('需求反馈已提交，我们会尽快处理');
    resetDemandForm();
    demandActiveTab.value = 'mine';
  } finally {
    demandSubmitting.value = false;
  }
};

defineExpose({ open });
</script>

<style scoped>
:global(.portal-demand-dialog) {
  width: min(760px, calc(100vw - 32px)) !important;
  overflow: hidden;
  border: 1px solid var(--tool-input-border);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 28px 70px rgba(31, 54, 76, 0.2);
}

:global(.portal-demand-dialog .el-dialog__header) {
  margin: 0;
  padding: 0;
}

:global(.portal-demand-dialog .el-dialog__headerbtn) {
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 8px;
}

:global(.portal-demand-dialog .el-dialog__headerbtn:hover) {
  background: var(--tool-primary-soft);
}

:global(.portal-demand-dialog .el-dialog__body) {
  padding: 0 26px;
}

:global(.portal-demand-dialog .el-dialog__footer) {
  padding: 18px 26px 22px;
  border-top: 1px solid var(--tool-border);
  background: #f8fafc;
}

.demand-dialog-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 28px 20px;
  border-bottom: 1px solid var(--tool-border);
  background: linear-gradient(135deg, var(--tool-accent-soft) 0%, rgba(255, 255, 255, 0.96) 58%);
}

.demand-dialog-icon {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(180deg, #d19a37 0%, var(--tool-accent) 100%);
  color: #fff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.46),
    0 14px 26px rgba(183, 121, 31, 0.22);
}

.demand-dialog-icon .el-icon {
  font-size: 24px;
}

.demand-dialog-head strong {
  display: block;
  color: var(--tool-title);
  font-size: 23px;
  line-height: 1.2;
  font-weight: 850;
}

.demand-dialog-head p {
  margin: 7px 0 0;
  color: var(--tool-muted);
  font-size: 14px;
  line-height: 1.4;
  font-weight: 650;
}

.demand-tabs {
  min-height: 492px;
  padding-top: 18px;
}

:deep(.demand-tabs .el-tabs__header) {
  margin: 0 0 18px;
}

:deep(.demand-tabs .el-tabs__nav-wrap::after),
:deep(.demand-tabs .el-tabs__active-bar) {
  display: none;
}

:deep(.demand-tabs .el-tabs__nav) {
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--tool-input-border);
  border-radius: 8px;
  background: #f5f7fa;
}

:deep(.demand-tabs .el-tabs__item) {
  height: 36px;
  border-radius: 7px;
  padding: 0 18px;
  color: var(--tool-muted);
  font-size: 14px;
  font-weight: 800;
}

:deep(.demand-tabs .el-tabs__item.is-active) {
  background: var(--tool-accent);
  color: #fff;
  box-shadow: 0 8px 18px rgba(183, 121, 31, 0.2);
}

.demand-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #dce5ed;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff9ed 0%, #fff 100%);
}

.demand-intro strong {
  display: block;
  color: var(--tool-title);
  font-size: 15px;
  line-height: 1.2;
  font-weight: 850;
}

.demand-intro p {
  margin: 6px 0 0;
  color: var(--tool-muted);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 650;
}

.demand-intro span {
  height: 30px;
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 0 12px;
  background: var(--tool-accent-soft);
  color: var(--tool-accent);
  font-size: 12px;
  line-height: 1;
  font-weight: 800;
}

.portal-demand-form {
  padding: 2px 2px 0 0;
}

:deep(.portal-demand-form .el-form-item) {
  margin-bottom: 18px;
}

:deep(.portal-demand-form .el-form-item__label) {
  color: var(--tool-text);
  font-size: 14px;
  font-weight: 800;
}

:deep(.portal-demand-form .el-input__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

:deep(.portal-demand-form .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--tool-primary) inset,
    0 8px 18px rgba(36, 95, 143, 0.08);
}

.suggestion-app-select {
  width: 100%;
}

:deep(.suggestion-app-select .el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
}

:deep(.suggestion-app-select .el-select__wrapper.is-focused) {
  box-shadow:
    0 0 0 1px var(--tool-primary) inset,
    0 8px 18px rgba(36, 95, 143, 0.08);
}

:deep(.portal-demand-form .el-input__inner),
:deep(.portal-demand-form .el-textarea__inner) {
  color: var(--tool-text);
  font-size: 14px;
  font-weight: 650;
}

:deep(.portal-demand-form .el-textarea__inner) {
  min-height: 142px !important;
  border-radius: 8px;
  padding: 12px 14px;
  box-shadow: 0 0 0 1px var(--tool-input-border) inset;
  resize: none;
}

:deep(.portal-demand-form .el-textarea__inner:focus) {
  box-shadow:
    0 0 0 1px var(--tool-primary) inset,
    0 8px 18px rgba(36, 95, 143, 0.08);
}

.demand-type-group {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

:deep(.demand-type-group .el-radio-button) {
  min-width: 0;
}

:deep(.demand-type-group .el-radio-button__inner) {
  width: 100%;
  height: auto;
  display: block;
  border: 1px solid var(--tool-input-border);
  border-radius: 8px;
  padding: 13px 15px;
  background: #fff;
  color: var(--tool-text);
  text-align: left;
  box-shadow: none;
}

:deep(.demand-type-group .el-radio-button:first-child .el-radio-button__inner) {
  border-left: 1px solid var(--tool-input-border);
}

:deep(.demand-type-group .el-radio-button.is-active .el-radio-button__inner) {
  border-color: var(--tool-accent);
  background: var(--tool-accent-soft);
  color: var(--tool-accent);
  box-shadow: 0 0 0 1px var(--tool-accent);
}

.demand-type-option {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.demand-type-option strong {
  font-size: 14px;
  line-height: 1.2;
  font-weight: 850;
}

.demand-type-option em {
  color: #6b7a99;
  font-size: 12px;
  line-height: 1.2;
  font-style: normal;
  font-weight: 650;
}

:deep(.demand-type-group .el-radio-button.is-active .demand-type-option em) {
  color: var(--tool-accent);
}

:global(.portal-app-select-popper) {
  border: 1px solid var(--tool-input-border) !important;
  border-radius: 8px !important;
  box-shadow: 0 18px 42px rgba(31, 54, 76, 0.16) !important;
}

:global(.portal-app-select-popper .el-select-dropdown__item) {
  height: auto;
  padding: 9px 12px;
}

:global(.portal-app-select-popper .el-select-dropdown__item.is-selected) {
  color: var(--tool-primary);
}

.suggestion-app-option {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 2px 0;
}

.suggestion-app-option strong {
  color: var(--tool-title);
  font-size: 14px;
  line-height: 1.2;
  font-weight: 850;
}

.suggestion-app-option span {
  color: #6b7a99;
  font-size: 12px;
  line-height: 1.2;
  font-weight: 650;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.dialog-footer .el-button) {
  height: 38px;
  min-width: 86px;
  border-radius: 8px;
  font-weight: 800;
}

:deep(.dialog-footer .el-button--primary) {
  border-color: var(--tool-primary);
  background: var(--tool-primary);
  box-shadow: 0 10px 22px rgba(36, 95, 143, 0.2);
}

@media (max-width: 520px) {
  :global(.portal-demand-dialog .el-dialog__body) {
    padding: 0 16px;
  }

  :global(.portal-demand-dialog .el-dialog__footer) {
    padding: 14px 16px 18px;
  }

  .demand-dialog-head {
    align-items: flex-start;
    padding: 24px 32px 20px;
  }

  .demand-dialog-head strong {
    font-size: 22px;
  }

  .demand-dialog-head p {
    font-size: 13px;
    line-height: 1.45;
  }

  .demand-intro {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .portal-demand-form {
    padding-right: 0;
  }

  :deep(.portal-demand-form .el-form-item) {
    display: block;
    margin-bottom: 16px;
  }

  :deep(.portal-demand-form .el-form-item__label) {
    width: auto !important;
    height: auto;
    display: block;
    margin-bottom: 8px;
    padding: 0;
    line-height: 1.2;
    text-align: left;
  }

  :deep(.portal-demand-form .el-form-item__content) {
    margin-left: 0 !important;
  }

  .demand-type-group {
    grid-template-columns: 1fr;
  }
}
</style>
