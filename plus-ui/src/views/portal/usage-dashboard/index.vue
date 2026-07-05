<template>
  <main class="usage-dashboard-page" v-loading="loading">
    <section class="dashboard-shell">
      <header class="dashboard-header">
        <div class="title-block">
          <span class="eyebrow">APPLICATION SITUATION</span>
          <h1>{{ dashboard.title }}</h1>
          <p>{{ dashboard.subtitle }}</p>
        </div>
        <div class="header-tools">
          <span>{{ dashboard.periodLabel }}</span>
          <span>{{ dashboard.scopeLabel }}</span>
          <span>更新 {{ dashboard.generatedAt || '-' }}</span>
          <el-button :icon="Refresh" type="primary" @click="loadDashboard">刷新</el-button>
          <el-button :icon="House" @click="router.push(PORTAL_HOME_PATH)">返回首页</el-button>
        </div>
      </header>

      <el-alert v-if="loadError" class="dashboard-alert" :title="loadError" type="warning" show-icon :closable="false" />

      <section class="kpi-grid">
        <article v-for="item in dashboard.kpis" :key="item.code" class="kpi-card">
          <span>{{ item.label }}</span>
          <strong>{{ formatNumber(item.value) }}<small>{{ item.unit }}</small></strong>
          <em>{{ item.hint }}</em>
        </article>
      </section>

      <section class="ops-grid">
        <article class="dashboard-panel">
          <PanelTitle title="模块活跃趋势" desc="按当前系统已有应用行为汇总" />
          <div ref="moduleChartRef" class="chart"></div>
        </article>

        <article class="dashboard-panel">
          <PanelTitle title="资料流动排行" desc="浏览与下载综合热度" />
          <RankList :items="dashboard.resourceRanking" empty-text="暂无资料流动数据" />
        </article>

        <article class="dashboard-panel">
          <PanelTitle title="应用热度排行" desc="按工具打开次数排序" />
          <RankList :items="dashboard.appRanking" empty-text="暂无应用使用数据" />
        </article>
      </section>

      <section class="dashboard-panel heatmap-panel">
        <PanelTitle title="部门 × 模块应用热力" desc="按可归属部门的数据展示，不伪造缺失口径" />
        <div v-if="dashboard.heatmap.ready" ref="heatmapChartRef" class="heatmap-chart"></div>
        <div v-else class="empty-state">
          <el-icon><DataAnalysis /></el-icon>
          <strong>{{ dashboard.heatmap.emptyReason }}</strong>
          <span>后续接入应用使用日志后，可展示更准确的部门热力。</span>
        </div>
      </section>

      <section class="bottom-grid">
        <article class="dashboard-panel">
          <PanelTitle title="需求反馈响应统计" desc="看反馈规模、状态和类型分布" />
          <div class="feedback-layout">
            <div ref="feedbackChartRef" class="feedback-chart"></div>
            <div class="feedback-stats">
              <div class="feedback-stat">
                <span>本期反馈</span>
                <strong>{{ formatNumber(dashboard.feedback.total) }}</strong>
              </div>
              <div class="feedback-stat pending">
                <span>待处理</span>
                <strong>{{ formatNumber(dashboard.feedback.pending) }}</strong>
              </div>
              <div class="feedback-stat">
                <span>处理中</span>
                <strong>{{ formatNumber(dashboard.feedback.processing) }}</strong>
              </div>
              <div class="feedback-stat resolved">
                <span>已处理</span>
                <strong>{{ formatNumber(dashboard.feedback.resolved) }}</strong>
              </div>
            </div>
          </div>
        </article>

        <article class="dashboard-panel">
          <PanelTitle title="低活跃与异常信号" desc="只展示当前数据能支撑的提醒" />
          <div class="signal-list">
            <div v-for="item in dashboard.signals" :key="item.title" :class="['signal-item', item.level]">
              <span>{{ item.title }}</span>
              <b>{{ item.action }}</b>
            </div>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup name="UsageDashboard" lang="ts">
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { DataAnalysis, House, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { getUsageDashboardOverview, UsageDashboardOverview, DashboardRankItem } from '@/api/portal/usageDashboard';

type ChartInstance = ReturnType<typeof echarts.init>;

const emptyDashboard: UsageDashboardOverview = {
  title: '系统应用情况洞察看板',
  subtitle: '看模块活跃、资料流动、应用热度、部门覆盖与反馈响应',
  periodLabel: '全量数据',
  scopeLabel: '全单位',
  generatedAt: '',
  kpis: [],
  moduleActivity: [],
  resourceRanking: [],
  appRanking: [],
  heatmap: { ready: false, emptyReason: '暂无足够部门归集数据', columns: [], rows: [] },
  feedback: { total: 0, pending: 0, processing: 0, resolved: 0, closed: 0, typeStats: [] },
  signals: []
};

const router = useRouter();
const loading = ref(false);
const loadError = ref('');
const overview = ref<UsageDashboardOverview>(emptyDashboard);
const moduleChartRef = ref<HTMLDivElement>();
const heatmapChartRef = ref<HTMLDivElement>();
const feedbackChartRef = ref<HTMLDivElement>();
const moduleChart = shallowRef<ChartInstance>();
const heatmapChart = shallowRef<ChartInstance>();
const feedbackChart = shallowRef<ChartInstance>();

const dashboard = computed(() => overview.value);

const PanelTitle = defineComponent({
  props: {
    title: { type: String, required: true },
    desc: { type: String, required: true }
  },
  setup(props) {
    return () =>
      h('header', { class: 'panel-title' }, [
        h('div', [h('h2', props.title), h('p', props.desc)])
      ]);
  }
});

const RankList = defineComponent({
  props: {
    items: { type: Array as () => DashboardRankItem[], required: true },
    emptyText: { type: String, required: true }
  },
  setup(props) {
    const maxValue = computed(() => Math.max(...props.items.map((item) => Number(item.value || 0)), 1));
    return () =>
      props.items.length
        ? h(
            'ol',
            { class: 'rank-list' },
            props.items.map((item, index) =>
              h('li', { class: 'rank-row', key: item.name }, [
                h('span', { class: 'rank-no' }, String(index + 1)),
                h('span', { class: 'rank-main' }, [
                  h('b', item.name),
                  h('em', item.category || '未分类'),
                  h('i', { style: { width: `${Math.max(8, (Number(item.value || 0) / maxValue.value) * 100)}%` } })
                ]),
                h('strong', [formatNumber(item.value), h('small', item.unit)])
              ])
            )
          )
        : h('div', { class: 'empty-state compact' }, [h(DataAnalysis), h('strong', props.emptyText)]);
  }
});

const formatNumber = (value: number | string | undefined) => Number(value || 0).toLocaleString('zh-CN');

const cssVar = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

const getChart = (target: HTMLDivElement | undefined, current: typeof moduleChart) => {
  if (!target) return undefined;
  if (!current.value) {
    current.value = echarts.init(target);
  }
  return current.value;
};

const renderCharts = async () => {
  await nextTick();
  renderModuleChart();
  renderHeatmapChart();
  renderFeedbackChart();
};

const renderModuleChart = () => {
  const chart = getChart(moduleChartRef.value, moduleChart);
  if (!chart) return;
  const rows = dashboard.value.moduleActivity;
  chart.setOption({
    color: [cssVar('--ip-primary-600')],
    grid: { left: 8, right: 12, top: 16, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', axisLabel: { color: cssVar('--ip-neutral-500') }, splitLine: { lineStyle: { color: cssVar('--ip-neutral-100') } } },
    yAxis: {
      type: 'category',
      data: rows.map((item) => item.label),
      axisLabel: { color: cssVar('--ip-neutral-700') },
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [
      {
        type: 'bar',
        data: rows.map((item) => item.value),
        barWidth: 16,
        itemStyle: { borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', color: cssVar('--ip-neutral-600') }
      }
    ]
  });
};

const renderHeatmapChart = () => {
  const chart = getChart(heatmapChartRef.value, heatmapChart);
  if (!chart || !dashboard.value.heatmap.ready) return;
  const { columns, rows } = dashboard.value.heatmap;
  const maxValue = Math.max(...rows.flatMap((row) => row.cells.map((cell) => Number(cell.value || 0))), 1);
  const data = rows.flatMap((row, rowIndex) =>
    columns.map((column, columnIndex) => {
      const cell = row.cells.find((item) => item.columnCode === column.code);
      return [columnIndex, rowIndex, Number(cell?.value || 0)];
    })
  );
  chart.setOption({
    tooltip: {
      position: 'top',
      formatter: (params: { value: [number, number, number] }) =>
        `${rows[params.value[1]].deptName}<br />${columns[params.value[0]].label}: ${formatNumber(params.value[2])}`
    },
    grid: { left: 92, right: 24, top: 34, bottom: 20 },
    xAxis: { type: 'category', data: columns.map((item) => item.label), splitArea: { show: true }, axisLabel: { color: cssVar('--ip-neutral-600') } },
    yAxis: { type: 'category', data: rows.map((item) => item.deptName), splitArea: { show: true }, axisLabel: { color: cssVar('--ip-neutral-600') } },
    visualMap: {
      min: 0,
      max: maxValue,
      show: false,
      inRange: { color: [cssVar('--ip-neutral-100'), cssVar('--ip-primary-300'), cssVar('--ip-primary-700')] }
    },
    series: [{ type: 'heatmap', data, label: { show: true, color: cssVar('--ip-neutral-900') }, emphasis: { itemStyle: { shadowBlur: 8 } } }]
  });
};

const renderFeedbackChart = () => {
  const chart = getChart(feedbackChartRef.value, feedbackChart);
  if (!chart) return;
  const feedback = dashboard.value.feedback;
  chart.setOption({
    color: [cssVar('--ip-warning'), cssVar('--ip-primary-500'), cssVar('--ip-success'), cssVar('--ip-neutral-400')],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: cssVar('--ip-neutral-600') } },
    series: [
      {
        type: 'pie',
        radius: ['48%', '72%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{c}', color: cssVar('--ip-neutral-700') },
        data: [
          { name: '待处理', value: feedback.pending },
          { name: '处理中', value: feedback.processing },
          { name: '已处理', value: feedback.resolved },
          { name: '已关闭', value: feedback.closed }
        ]
      }
    ]
  });
};

const resizeCharts = () => {
  moduleChart.value?.resize();
  heatmapChart.value?.resize();
  feedbackChart.value?.resize();
};

const loadDashboard = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const { data } = await getUsageDashboardOverview();
    overview.value = data || emptyDashboard;
    await renderCharts();
  } catch (error) {
    loadError.value = '应用态势数据加载失败，请稍后重试';
    ElMessage.warning(loadError.value);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  void loadDashboard();
  window.addEventListener('resize', resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  moduleChart.value?.dispose();
  heatmapChart.value?.dispose();
  feedbackChart.value?.dispose();
});
</script>

<style scoped lang="scss">
.usage-dashboard-page {
  --usage-dashboard-font-family: var(--el-font-family);

  min-height: 100vh;
  padding: 24px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--ip-primary-100) 62%, transparent), transparent 34%),
    linear-gradient(180deg, var(--ip-neutral-50) 0%, var(--ip-neutral-100) 100%);
  color: var(--ip-neutral-900);
}

.dashboard-shell {
  max-width: 1680px;
  min-height: calc(100vh - 48px);
  margin: 0 auto;
}

.dashboard-header,
.dashboard-panel,
.kpi-card {
  border: 1px solid var(--ip-neutral-200);
  border-start-start-radius: var(--ip-radius-sm);
  border-start-end-radius: var(--ip-radius-sm);
  border-end-start-radius: var(--ip-radius-sm);
  border-end-end-radius: var(--ip-radius-sm);
  background: color-mix(in srgb, var(--ip-neutral-0) 92%, transparent);
  box-shadow: var(--ip-shadow-md);
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
}

.title-block {
  min-width: 0;
}

.eyebrow {
  display: inline-block;
  color: var(--ip-primary-600);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
  font-weight: 800;
}

.title-block h1 {
  margin: 8px 0 0;
  color: var(--ip-neutral-900);
  font: 850 var(--ip-font-headline)/1.18 var(--usage-dashboard-font-family);
  line-height: 1.18;
  font-weight: 850;
}

.title-block p {
  margin: 8px 0 0;
  color: var(--ip-neutral-500);
  font: 400 var(--ip-font-body)/1.5 var(--usage-dashboard-font-family);
}

.header-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.header-tools span {
  height: 32px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--ip-neutral-200);
  border-start-start-radius: var(--ip-radius-full);
  border-start-end-radius: var(--ip-radius-full);
  border-end-start-radius: var(--ip-radius-full);
  border-end-end-radius: var(--ip-radius-full);
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-600);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
}

.dashboard-alert {
  margin-top: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.kpi-card {
  min-height: 118px;
  padding: 16px;
}

.kpi-card span,
.kpi-card em {
  display: block;
  color: var(--ip-neutral-500);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
  font-style: normal;
}

.kpi-card strong {
  display: block;
  margin-top: 12px;
  color: var(--ip-primary-700);
  font: 860 var(--ip-font-display)/1 var(--usage-dashboard-font-family);
  line-height: 1;
  font-weight: 860;
}

.kpi-card small {
  margin-left: 5px;
  color: var(--ip-neutral-500);
  font: 400 var(--ip-font-body)/1.5 var(--usage-dashboard-font-family);
}

.kpi-card em {
  margin-top: 12px;
}

.ops-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.bottom-grid {
  grid-template-columns: 1fr 1fr;
}

.dashboard-panel {
  min-height: 280px;
  padding: 18px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.panel-title h2 {
  margin: 0;
  color: var(--ip-neutral-800);
  font: 700 var(--ip-font-emphasis)/1.2 var(--usage-dashboard-font-family);
  line-height: 1.2;
}

.panel-title p {
  margin: 6px 0 0;
  color: var(--ip-neutral-500);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
}

.chart {
  width: 100%;
  height: 218px;
}

.heatmap-panel {
  min-height: 360px;
  margin-top: 14px;
}

.heatmap-chart {
  width: 100%;
  height: 292px;
}

.feedback-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.95fr) 1fr;
  gap: 14px;
}

.feedback-chart {
  width: 100%;
  height: 220px;
}

.feedback-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.feedback-stat {
  min-height: 86px;
  padding: 13px;
  border: 1px solid var(--ip-neutral-200);
  border-start-start-radius: var(--ip-radius-sm);
  border-start-end-radius: var(--ip-radius-sm);
  border-end-start-radius: var(--ip-radius-sm);
  border-end-end-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
}

.feedback-stat span {
  display: block;
  color: var(--ip-neutral-500);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
}

.feedback-stat strong {
  display: block;
  margin-top: 10px;
  color: var(--ip-primary-700);
  font: 800 var(--ip-font-title)/1.1 var(--usage-dashboard-font-family);
}

.feedback-stat.pending strong {
  color: var(--ip-warning);
}

.feedback-stat.resolved strong {
  color: var(--ip-success);
}

.rank-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rank-row {
  display: grid;
  grid-template-columns: 30px 1fr auto;
  gap: 12px;
  align-items: center;
}

.rank-no {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-start-start-radius: var(--ip-radius-sm);
  border-start-end-radius: var(--ip-radius-sm);
  border-end-start-radius: var(--ip-radius-sm);
  border-end-end-radius: var(--ip-radius-sm);
  background: var(--ip-primary-50);
  color: var(--ip-primary-700);
  font-weight: 800;
}

.rank-main {
  min-width: 0;
}

.rank-main b,
.rank-main em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-main b {
  color: var(--ip-neutral-800);
  font: 400 var(--ip-font-body)/1.5 var(--usage-dashboard-font-family);
}

.rank-main em {
  margin-top: 4px;
  color: var(--ip-neutral-500);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
  font-style: normal;
}

.rank-main i {
  display: block;
  height: 6px;
  margin-top: 8px;
  border-start-start-radius: var(--ip-radius-full);
  border-start-end-radius: var(--ip-radius-full);
  border-end-start-radius: var(--ip-radius-full);
  border-end-end-radius: var(--ip-radius-full);
  background: linear-gradient(90deg, var(--ip-primary-600), var(--ip-mod-resources));
}

.rank-row strong {
  color: var(--ip-neutral-800);
  font: 800 var(--ip-font-title-sm)/1.1 var(--usage-dashboard-font-family);
  white-space: nowrap;
}

.rank-row small {
  margin-left: 3px;
  color: var(--ip-neutral-500);
  font: 800 var(--ip-font-caption)/1.2 var(--usage-dashboard-font-family);
}

.signal-list {
  display: grid;
  gap: 10px;
}

.signal-item {
  min-height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--ip-neutral-200);
  border-start-start-radius: var(--ip-radius-sm);
  border-start-end-radius: var(--ip-radius-sm);
  border-end-start-radius: var(--ip-radius-sm);
  border-end-end-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-700);
}

.signal-item.warning {
  border-color: var(--ip-warning-border);
  background: var(--ip-warning-soft);
}

.signal-item.success {
  border-color: var(--ip-success-border);
  background: var(--ip-success-soft);
}

.signal-item.danger {
  border-color: var(--ip-danger-border);
  background: var(--ip-danger-soft);
}

.signal-item span {
  min-width: 0;
  font: 400 var(--ip-font-body)/1.5 var(--usage-dashboard-font-family);
}

.signal-item b {
  color: var(--ip-neutral-800);
  white-space: nowrap;
}

.empty-state {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px dashed var(--ip-neutral-300);
  border-start-start-radius: var(--ip-radius-sm);
  border-start-end-radius: var(--ip-radius-sm);
  border-end-start-radius: var(--ip-radius-sm);
  border-end-end-radius: var(--ip-radius-sm);
  background: var(--ip-neutral-50);
  color: var(--ip-neutral-500);
  text-align: center;
}

.empty-state .el-icon {
  color: var(--ip-primary-500);
  font: 400 var(--ip-font-headline)/1 var(--usage-dashboard-font-family);
}

.empty-state strong {
  color: var(--ip-neutral-700);
}

.empty-state.compact {
  min-height: 190px;
}

@media (max-width: 1180px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .ops-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .usage-dashboard-page {
    padding: 14px;
  }

  .dashboard-header,
  .feedback-layout {
    grid-template-columns: 1fr;
  }

  .dashboard-header {
    display: grid;
  }

  .header-tools {
    justify-content: flex-start;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .kpi-grid,
  .feedback-stats {
    grid-template-columns: 1fr;
  }
}
</style>
