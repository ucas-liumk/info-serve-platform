<template>
  <main class="rk-admin-page">
    <header class="rk-admin-head">
      <div>
        <p class="rk-admin-kicker">应知应会管理</p>
        <h1>题目录入</h1>
        <p>维护首期题库。题目保存后可被考试配置引用。</p>
      </div>
      <AdminNav />
    </header>

    <section class="rk-summary" aria-label="题库概览">
      <div>
        <strong>{{ questionList.length }}</strong>
        <span>题目总数</span>
      </div>
      <div>
        <strong>{{ publishedCount }}</strong>
        <span>已发布</span>
      </div>
      <div>
        <strong>{{ draftCount }}</strong>
        <span>草稿</span>
      </div>
    </section>

    <section class="rk-panel">
      <div class="rk-toolbar">
        <el-form class="rk-filter" :model="query" inline>
          <el-form-item label="科目">
            <el-select v-model="query.subject" clearable placeholder="全部科目" style="width: 168px">
              <el-option v-for="subject in subjectOptions" :key="subject.subjectId" :label="subject.subjectName" :value="subject.subjectName" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="query.type" clearable placeholder="全部题型" style="width: 140px">
              <el-option v-for="type in questionTypes" :key="type" :label="type" :value="type" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="rk-actions">
          <el-button @click="openImportDialog">
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
          <el-button type="primary" @click="drawerVisible = true">新增题目</el-button>
        </div>
      </div>

      <el-table v-if="filteredQuestions.length" :data="filteredQuestions" border>
        <el-table-column label="题目摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="rk-question-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.subject }} · {{ row.type }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="150" />
        <el-table-column prop="type" label="题型" width="100" />
        <el-table-column prop="answer" label="答案" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="rk-tag" :class="{ draft: row.status === '草稿' }">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default>
            <el-button link type="primary">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无匹配题目" />
    </section>

    <el-drawer v-model="drawerVisible" title="新增题目" size="520px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="科目">
          <el-select v-model="form.subject" placeholder="请选择科目" style="width: 100%">
            <el-option v-for="subject in subjectOptions" :key="subject.subjectId" :label="subject.subjectName" :value="subject.subjectName" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="type in questionTypes" :key="type" :label="type" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.title" type="textarea" :rows="4" placeholder="请输入题干" />
        </el-form-item>
        <el-form-item label="选项">
          <el-input v-model="form.options" type="textarea" :rows="4" placeholder="每行一个选项，主观题可留空" />
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="form.answer" placeholder="如 A 或 正确" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="请输入解析" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="importDialogVisible" title="批量导入题目" width="860px" class="rk-import-dialog">
      <div class="rk-import-layout">
        <el-upload
          ref="uploadRef"
          class="rk-upload"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".json,.xlsx,.csv"
          :on-change="onImportFileChange"
          :on-remove="clearImportFile"
        >
          <el-icon class="rk-upload-icon"><UploadFilled /></el-icon>
          <div class="rk-upload-title">选择 JSON 或 Excel 文件</div>
          <div class="rk-upload-hint">支持 .json / .xlsx / .csv</div>
        </el-upload>

        <div class="rk-import-guide">
          <strong>字段</strong>
          <span>科目、题型、题干、答案为必填；选项、解析、状态可选。</span>
          <span>题型支持：单选、多选、判断、主观；状态为空时按草稿导入。</span>
          <div class="rk-template-actions">
            <el-button size="small" @click="downloadJsonTemplate">
              <el-icon><Download /></el-icon>
              JSON 模板
            </el-button>
            <el-button size="small" @click="downloadExcelTemplate">
              <el-icon><Download /></el-icon>
              Excel 模板
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="importPreview.length" class="rk-import-summary">
        <span>文件：{{ importFileName }}</span>
        <span>有效 {{ importStats.valid }} 条</span>
        <span>错误 {{ importStats.invalid }} 条</span>
      </div>

      <el-table v-if="importPreview.length" class="rk-import-table" :data="importPreview" height="300" border>
        <el-table-column label="行" prop="rowNo" width="70" />
        <el-table-column label="校验" width="90">
          <template #default="{ row }">
            <span class="rk-tag" :class="{ draft: !row.valid }">{{ row.valid ? '有效' : '错误' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="130" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="90" />
        <el-table-column prop="title" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" width="110" show-overflow-tooltip />
        <el-table-column label="问题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.errors.length ? row.errors.join('；') : '—' }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else class="rk-import-empty" description="上传文件后预览导入结果" />

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!validImportRows.length" @click="confirmImport">确认导入 {{ validImportRows.length }} 条</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { UploadFile, UploadInstance } from 'element-plus';
import { Download, Upload, UploadFilled } from '@element-plus/icons-vue';
import readXlsxFile from 'read-excel-file';
import AdminNav from '../components/AdminNav.vue';
import { downloadQuestionExcelTemplate, downloadQuestionJsonTemplate } from './importTemplates';
import { listSubjectOptions } from '@/api/required-knowledge';
import { RkSubject } from '@/api/required-knowledge/types';

type QuestionType = '单选' | '多选' | '判断' | '主观';
type QuestionStatus = '已发布' | '草稿';

interface QuestionRow {
  title: string;
  subject: string;
  type: QuestionType;
  answer: string;
  status: QuestionStatus;
  updatedAt: string;
  options?: string;
  analysis?: string;
}

interface ImportPreviewRow extends QuestionRow {
  rowNo: number;
  valid: boolean;
  errors: string[];
}

const drawerVisible = ref(false);
const importDialogVisible = ref(false);
const uploadRef = ref<UploadInstance>();
const subjectOptions = ref<RkSubject[]>([]);
const query = reactive({ subject: '', type: '' });
const questionList = ref<QuestionRow[]>([
  { title: '范围基准通常由哪些内容组成？', subject: '软考高项', type: '多选', answer: 'A/B/C', status: '已发布', updatedAt: '2026-07-05 09:30' },
  { title: '互斥信号量 mutex 的初值通常是多少？', subject: '考研 408', type: '单选', answer: '1', status: '已发布', updatedAt: '2026-07-05 09:20' },
  { title: '当 x -> 0 时，sin x 与 x 是否等价？', subject: '考研数学', type: '判断', answer: '正确', status: '草稿', updatedAt: '2026-07-05 09:10' }
]);

const form = reactive({
  subject: '',
  type: '单选',
  title: '',
  options: '',
  answer: '',
  analysis: ''
});

const questionTypes: QuestionType[] = ['单选', '多选', '判断', '主观'];
const importPreview = ref<ImportPreviewRow[]>([]);
const importFileName = ref('');

const filteredQuestions = computed(() =>
  questionList.value.filter((item) => (!query.subject || item.subject === query.subject) && (!query.type || item.type === query.type))
);
const publishedCount = computed(() => questionList.value.filter((item) => item.status === '已发布').length);
const draftCount = computed(() => questionList.value.filter((item) => item.status === '草稿').length);
const validImportRows = computed(() => importPreview.value.filter((item) => item.valid));
const importStats = computed(() => ({
  valid: validImportRows.value.length,
  invalid: importPreview.value.length - validImportRows.value.length
}));

const formatNow = () => {
  const date = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const saveQuestion = () => {
  questionList.value.unshift({
    title: form.title || '未命名题目',
    subject: form.subject || '软考高项',
    type: form.type,
    answer: form.answer || '待补充',
    status: '草稿',
    updatedAt: formatNow(),
    options: form.options,
    analysis: form.analysis
  });
  drawerVisible.value = false;
};

const normalizeKey = (value: string) => value.trim().toLowerCase().replace(/\s+/g, '');

const normalizeType = (value: string): QuestionType | '' => {
  const key = normalizeKey(value).replace(/题$/, '');
  const map: Record<string, QuestionType> = {
    单选: '单选',
    单项选择: '单选',
    single: '单选',
    radio: '单选',
    多选: '多选',
    多项选择: '多选',
    multiple: '多选',
    checkbox: '多选',
    判断: '判断',
    truefalse: '判断',
    tf: '判断',
    主观: '主观',
    简答: '主观',
    subjective: '主观'
  };
  return map[key] || '';
};

const normalizeStatus = (value: string): QuestionStatus => {
  const key = normalizeKey(value);
  if (['已发布', '发布', '启用', 'published', 'enabled'].includes(key)) return '已发布';
  return '草稿';
};

const readValue = (row: Record<string, unknown>, keys: string[]) => {
  for (const key of keys) {
    const value = row[key];
    if (Array.isArray(value)) {
      return value
        .map((item) => String(item).trim())
        .filter(Boolean)
        .join('\n');
    }
    if (value !== undefined && value !== null && String(value).trim()) return String(value).trim();
  }
  return '';
};

const toPreviewRow = (row: Record<string, unknown>, index: number): ImportPreviewRow => {
  const subject = readValue(row, ['科目', '所属科目', 'subject', 'subjectName']);
  const rawType = readValue(row, ['题型', '类型', 'type', 'questionType']);
  const type = normalizeType(rawType);
  const title = readValue(row, ['题干', '题目', '题目内容', 'title', 'question', 'stem']);
  const answer = readValue(row, ['答案', '正确答案', 'answer', 'correctAnswer']);
  const options = readValue(row, ['选项', 'options', 'choices']);
  const analysis = readValue(row, ['解析', 'analysis', 'explanation']);
  const status = normalizeStatus(readValue(row, ['状态', 'status']));
  const errors: string[] = [];
  const subjectNames = new Set(subjectOptions.value.map((item) => item.subjectName));

  if (!subject) {
    errors.push('科目为空');
  } else if (subjectNames.size && !subjectNames.has(subject)) {
    errors.push('科目不存在');
  }
  if (!rawType) errors.push('题型为空');
  if (rawType && !type) errors.push('题型不支持');
  if (!title) errors.push('题干为空');
  if (!answer) errors.push('答案为空');

  return {
    rowNo: index + 1,
    subject,
    type: type || '单选',
    title,
    answer,
    options,
    analysis,
    status,
    updatedAt: formatNow(),
    valid: errors.length === 0,
    errors
  };
};

const parseJsonFile = async (file: File): Promise<Record<string, unknown>[]> => {
  const parsed = JSON.parse(await file.text());
  if (Array.isArray(parsed)) return parsed;
  if (Array.isArray(parsed?.questions)) return parsed.questions;
  if (Array.isArray(parsed?.data)) return parsed.data;
  throw new Error('JSON 根节点需为数组，或包含 questions/data 数组');
};

const parseTableRows = (rows: unknown[][]) => {
  const [header, ...body] = rows;
  if (!header?.length) return [];
  const keys = header.map((item) => String(item ?? '').trim());
  return body
    .filter((row) => row.some((cell) => String(cell ?? '').trim()))
    .map((row) =>
      keys.reduce<Record<string, unknown>>((record, key, index) => {
        if (key) record[key] = row[index] ?? '';
        return record;
      }, {})
    );
};

const parseCsvLine = (line: string) => {
  const cells: string[] = [];
  let current = '';
  let quoted = false;
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];
    if (char === '"' && quoted && next === '"') {
      current += '"';
      index += 1;
    } else if (char === '"') {
      quoted = !quoted;
    } else if (char === ',' && !quoted) {
      cells.push(current.trim());
      current = '';
    } else {
      current += char;
    }
  }
  cells.push(current.trim());
  return cells;
};

const parseCsvFile = async (file: File): Promise<Record<string, unknown>[]> => {
  const rows = (await file.text())
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map(parseCsvLine);
  return parseTableRows(rows);
};

const parseExcelFile = async (file: File): Promise<Record<string, unknown>[]> => {
  const rows = await readXlsxFile(file);
  return parseTableRows(rows);
};

const parseImportFile = async (file: File) => {
  const name = file.name.toLowerCase();
  if (name.endsWith('.json')) return parseJsonFile(file);
  if (name.endsWith('.xlsx')) return parseExcelFile(file);
  if (name.endsWith('.csv')) return parseCsvFile(file);
  throw new Error('仅支持 JSON、XLSX 或 CSV 文件');
};

const clearImportFile = () => {
  importPreview.value = [];
  importFileName.value = '';
  uploadRef.value?.clearFiles();
};

const downloadJsonTemplate = () => {
  downloadQuestionJsonTemplate();
};

const downloadExcelTemplate = () => {
  downloadQuestionExcelTemplate();
};

const openImportDialog = () => {
  clearImportFile();
  importDialogVisible.value = true;
};

const onImportFileChange = async (uploadFile: UploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;
  try {
    const rows = await parseImportFile(file);
    if (!rows.length) {
      clearImportFile();
      ElMessage.warning('文件中没有可导入的数据');
      return;
    }
    importFileName.value = file.name;
    importPreview.value = rows.map((item, index) => toPreviewRow(item, index));
    ElMessage.success(`已解析 ${importPreview.value.length} 条题目`);
  } catch (error) {
    clearImportFile();
    ElMessage.error(error instanceof Error ? error.message : '文件解析失败');
  }
};

const confirmImport = () => {
  if (!validImportRows.value.length) {
    ElMessage.warning('没有可导入的有效题目');
    return;
  }
  questionList.value = [
    ...validImportRows.value.map(({ rowNo, valid, errors, ...question }) => ({
      ...question,
      updatedAt: formatNow()
    })),
    ...questionList.value
  ];
  ElMessage.success(`已导入 ${validImportRows.value.length} 条题目`);
  importDialogVisible.value = false;
  clearImportFile();
};

const loadSubjects = async () => {
  const res = await listSubjectOptions({ status: '0' });
  subjectOptions.value = (res as any).data ?? [];
};

onMounted(() => {
  loadSubjects();
});
</script>

<style scoped>
.rk-admin-page {
  padding: 24px;
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-900);
}

.rk-admin-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.rk-admin-kicker {
  margin: 0 0 4px;
  color: var(--ip-primary-600);
  font-size: var(--ip-font-hint);
  line-height: 1.5;
  font-weight: 700;
}

.rk-admin-head h1 {
  margin: 0;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-headline);
  line-height: 1.25;
  font-weight: 700;
}

.rk-admin-head p {
  margin: 8px 0 0;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-body);
  line-height: 1.6;
}

.rk-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.rk-summary div {
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-summary strong {
  display: block;
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-title);
  line-height: 1.35;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rk-summary span {
  display: block;
  margin-top: 4px;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-panel {
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-0);
  box-shadow: var(--ip-shadow-sm);
}

.rk-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.rk-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rk-filter :deep(.el-form-item) {
  margin-bottom: 0;
}

.rk-question-cell {
  display: grid;
  gap: 4px;
}

.rk-question-cell strong {
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 600;
}

.rk-question-cell span {
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-tag {
  height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid var(--ip-success-border);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-success-soft);
  color: var(--ip-success);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
}

.rk-tag.draft {
  border-color: var(--ip-warning-border);
  background: var(--ip-warning-soft);
  color: var(--ip-warning);
}

.rk-import-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
  align-items: stretch;
}

.rk-upload :deep(.el-upload),
.rk-upload :deep(.el-upload-dragger) {
  width: 100%;
}

.rk-upload :deep(.el-upload-dragger) {
  border-color: var(--ip-neutral-300);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-50);
}

.rk-upload-icon {
  margin-top: 8px;
  color: var(--ip-primary-600);
  font-size: 32px;
}

.rk-upload-title {
  margin-top: 8px;
  color: var(--ip-neutral-800);
  font-size: var(--ip-font-emphasis);
  line-height: 1.6;
  font-weight: 700;
}

.rk-upload-hint {
  margin-top: 4px;
  color: var(--ip-neutral-500);
  font-size: var(--ip-font-hint);
  line-height: 1.5;
}

.rk-import-guide {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: var(--ip-radius-md);
  background: var(--ip-neutral-50);
}

.rk-import-guide strong {
  color: var(--ip-neutral-900);
  font-size: var(--ip-font-body);
  line-height: 1.6;
  font-weight: 700;
}

.rk-import-guide span {
  color: var(--ip-neutral-600);
  font-size: var(--ip-font-hint);
  line-height: 1.5;
}

.rk-import-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0 12px;
}

.rk-import-summary span {
  height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--ip-primary-200);
  border-radius: var(--ip-radius-sm);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
  font-size: var(--ip-font-caption);
  line-height: 1.5;
  font-weight: 700;
}

.rk-import-table {
  margin-top: 12px;
}

.rk-import-empty {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .rk-admin-head,
  .rk-toolbar {
    display: grid;
  }

  .rk-summary {
    grid-template-columns: 1fr;
  }

  .rk-import-layout {
    grid-template-columns: 1fr;
  }
}
</style>
