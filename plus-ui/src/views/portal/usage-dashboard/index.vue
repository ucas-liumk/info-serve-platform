<template>
  <main class="usage-dashboard-page" v-loading="loading">
    <section class="dashboard-shell">
      <header class="command-topbar">
        <div class="brand-block">
          <img :src="homeLogo" alt="信息中心数智服务平台" />
          <div>
            <strong>信息中心数智服务平台</strong>
            <span>数字化转型态势中心</span>
          </div>
        </div>

        <nav class="situation-tabs" aria-label="态势分类">
          <button
            v-for="tab in situationTabs"
            :key="tab.code"
            type="button"
            :class="['situation-tab', { active: activeTab === tab.code, pending: !tab.enabled }]"
            :aria-current="activeTab === tab.code ? 'page' : undefined"
            @click="handleTabClick(tab)"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
            <span>{{ tab.label }}</span>
            <em v-if="!tab.enabled">预留</em>
          </button>
        </nav>

        <div class="topbar-actions">
          <span>{{ dashboard.periodLabel || '全量数据' }}</span>
          <span>{{ dashboard.scopeLabel || '全单位' }}</span>
          <span>更新 {{ dashboard.generatedAt || '-' }}</span>
          <el-button :icon="Refresh" type="primary" @click="loadDashboard">刷新</el-button>
          <el-button :icon="House" @click="router.push(PORTAL_HOME_PATH)">首页</el-button>
        </div>
      </header>

      <section class="hero-strip">
        <div class="hero-copy">
          <span>APPLICATION SITUATION</span>
          <h1>{{ dashboard.title }}</h1>
          <p>{{ dashboard.subtitle }}</p>
        </div>
        <div class="hero-summary">
          <b>{{ formatNumber(kpiValue('usage')) }}</b>
          <span>累计使用次数</span>
          <em>{{ feedbackRate }}% 反馈办结率</em>
        </div>
      </section>

      <el-alert v-if="loadError" class="dashboard-alert" :title="loadError" type="warning" show-icon :closable="false" />

      <section class="kpi-strip" aria-label="核心指标">
        <article v-for="item in dashboard.kpis" :key="item.code" class="kpi-tile">
          <el-icon><component :is="kpiIcon(item.code)" /></el-icon>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ formatNumber(item.value) }}<small>{{ item.unit }}</small></strong>
            <em>{{ item.hint }}</em>
          </div>
        </article>
      </section>

      <section class="command-grid">
        <aside class="side-stack">
          <article class="dashboard-panel summary-panel">
            <PanelTitle title="运行总览" desc="当前系统应用强度" />
            <div ref="summaryChartRef" class="summary-chart"></div>
            <div class="summary-points">
              <span>工具 {{ formatNumber(kpiValue('tools')) }}{{ kpiUnit('tools') }}</span>
              <span>资料 {{ formatNumber(kpiValue('resources')) }}{{ kpiUnit('resources') }}</span>
              <span>活跃 {{ formatNumber(kpiValue('activeUsers')) }}{{ kpiUnit('activeUsers') }}</span>
            </div>
          </article>

          <article class="dashboard-panel trend-panel">
            <PanelTitle title="模块活跃趋势" desc="按已有应用行为汇总" />
            <div ref="moduleChartRef" class="chart"></div>
          </article>
        </aside>

        <article class="dashboard-panel command-map-panel">
          <PanelTitle title="部门 × 模块应用热力" desc="让数字化转型工作战场透明" tag="核心视图" />
          <div class="command-map-body">
            <img class="command-map-art" :src="commandMapBg" alt="" aria-hidden="true" />
            <div v-if="dashboard.heatmap.ready" ref="heatmapChartRef" class="heatmap-chart"></div>
            <div v-else class="empty-state map-empty">
              <img :src="moduleTools" alt="应用态势" />
              <strong>{{ dashboard.heatmap.emptyReason }}</strong>
              <span>接入部门归集数据后展示热力分布。</span>
            </div>
          </div>
          <div class="module-metrics">
            <div v-for="item in moduleStats" :key="item.code" class="module-metric">
              <b>{{ item.label }}</b>
              <strong>{{ formatNumber(item.value) }}</strong>
              <span>{{ item.hint }}</span>
            </div>
          </div>
        </article>

        <aside class="side-stack">
          <article class="dashboard-panel rank-panel">
            <PanelTitle title="应用热度排行" desc="按工具打开次数排序" />
            <RankList :items="dashboard.appRanking" empty-text="暂无应用使用数据" />
          </article>

          <article class="dashboard-panel feedback-panel">
            <PanelTitle title="需求反馈响应统计" desc="规模、状态与类型分布" />
            <div class="feedback-body">
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
                <div class="feedback-stat resolved">
                  <span>已处理</span>
                  <strong>{{ formatNumber(dashboard.feedback.resolved) }}</strong>
                </div>
              </div>
            </div>
          </article>
        </aside>
      </section>

      <section class="detail-grid">
        <article class="dashboard-panel resource-panel">
          <PanelTitle title="资料流动排行" desc="浏览与下载综合热度" />
          <RankList :items="dashboard.resourceRanking" empty-text="暂无资料流动数据" />
        </article>

        <article class="dashboard-panel signal-panel">
          <PanelTitle title="低活跃与异常信号" desc="仅展示当前数据能支撑的提醒" />
          <div class="signal-list">
            <div v-for="item in dashboard.signals" :key="item.title" :class="['signal-item', item.level]">
              <span>{{ item.title }}</span>
              <b>{{ item.action }}</b>
            </div>
          </div>
        </article>

        <article class="dashboard-panel expansion-panel">
          <PanelTitle title="后续态势扩展" desc="同一顶部 tab 承载更多业务态势" />
          <div class="expansion-list">
            <div v-for="item in futureCards" :key="item.title" class="expansion-item">
              <el-icon><component :is="item.icon" /></el-icon>
              <div>
                <b>{{ item.title }}</b>
                <span>{{ item.desc }}</span>
              </div>
            </div>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup name="UsageDashboard" lang="ts">
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, type Component } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import {
  ChatLineRound,
  Collection,
  Connection,
  DataAnalysis,
  Document,
  FolderOpened,
  Grid,
  House,
  OfficeBuilding,
  Operation,
  Refresh,
  Tools,
  TrendCharts,
  UserFilled,
  Warning
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import homeLogo from '@/assets/portal/home-logo.png';
import moduleTools from '@/assets/portal/module-tools.png';
import commandMapBg from '@/assets/portal/usage-dashboard-command-map.png';
import { PORTAL_HOME_PATH } from '@/constants/router';
import { DashboardRankItem, UsageDashboardOverview, getUsageDashboardOverview } from '@/api/portal/usageDashboard';

type ChartInstance = ReturnType<typeof echarts.init>;
type SituationTab = { code: string; label: string; icon: Component; enabled: boolean };

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

const situationTabs: SituationTab[] = [
  { code: 'application', label: '应用态势', icon: DataAnalysis, enabled: true },
  { code: 'demand', label: '需求态势', icon: Document, enabled: false },
  { code: 'task', label: '任务办理', icon: Operation, enabled: false },
  { code: 'issue', label: '问题闭环', icon: Warning, enabled: false },
  { code: 'resource', label: '资源态势', icon: Collection, enabled: false }
];

const futureCards = [
  { title: '需求态势', desc: '需求来源、办理时长、部门响应', icon: Document },
  { title: '任务办理', desc: '任务分解、节点进度、逾期预警', icon: Operation },
  { title: '问题闭环', desc: '问题发现、整改跟踪、闭环率', icon: Connection }
];

const kpiIconMap: Record<string, Component> = {
  tools: Tools,
  resources: FolderOpened,
  activeUsers: UserFilled,
  deptCoverage: OfficeBuilding,
  usage: TrendCharts,
  feedback: ChatLineRound
};

const router = useRouter();
const activeTab = ref('application');
const loading = ref(false);
const loadError = ref('');
const overview = ref<UsageDashboardOverview>(emptyDashboard);
const summaryChartRef = ref<HTMLDivElement>();
const moduleChartRef = ref<HTMLDivElement>();
const heatmapChartRef = ref<HTMLDivElement>();
const feedbackChartRef = ref<HTMLDivElement>();
const summaryChart = shallowRef<ChartInstance>();
const moduleChart = shallowRef<ChartInstance>();
const heatmapChart = shallowRef<ChartInstance>();
const feedbackChart = shallowRef<ChartInstance>();

const dashboard = computed(() => overview.value);
const moduleStats = computed(() => dashboard.value.moduleActivity.slice(0, 4));
const feedbackRate = computed(() => {
  const total = Number(dashboard.value.feedback.total || 0);
  if (!total) return 0;
  return Math.round((Number(dashboard.value.feedback.resolved || 0) / total) * 100);
});

const PanelTitle = defineComponent({
  props: {
    title: { type: String, required: true },
    desc: { type: String, required: true },
    tag: { type: String, default: '' }
  },
  setup(props) {
    return () =>
      h('header', { class: 'panel-title' }, [
        h('div', [h('h2', props.title), h('p', props.desc)]),
        props.tag ? h('span', { class: 'panel-tag' }, props.tag) : null
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
            props.items.slice(0, 5).map((item, index) =>
              h('li', { class: 'rank-row', key: `${item.name}-${index}` }, [
                h('span', { class: 'rank-no' }, String(index + 1).padStart(2, '0')),
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

const normalizeDashboard = (data?: UsageDashboardOverview): UsageDashboardOverview => ({
  ...emptyDashboard,
  ...data,
  kpis: data?.kpis || [],
  moduleActivity: data?.moduleActivity || [],
  resourceRanking: data?.resourceRanking || [],
  appRanking: data?.appRanking || [],
  heatmap: {
    ...emptyDashboard.heatmap,
    ...data?.heatmap,
    columns: data?.heatmap?.columns || [],
    rows: data?.heatmap?.rows || []
  },
  feedback: {
    ...emptyDashboard.feedback,
    ...data?.feedback,
    typeStats: data?.feedback?.typeStats || []
  },
  signals: data?.signals || []
});

const formatNumber = (value: number | string | undefined) => Number(value || 0).toLocaleString('zh-CN');
const kpiIcon = (code: string) => kpiIconMap[code] || Grid;
const kpiItem = (code: string) => dashboard.value.kpis.find((item) => item.code === code);
const kpiValue = (code: string) => Number(kpiItem(code)?.value || 0);
const kpiUnit = (code: string) => kpiItem(code)?.unit || '';
const cssVar = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
const cssNumber = (name: string, fallback: number) => Number.parseInt(cssVar(name), 10) || fallback;

const handleTabClick = (tab: SituationTab) => {
  if (!tab.enabled) {
    ElMessage.info(`${tab.label}将在相关业务上线后接入`);
    return;
  }
  activeTab.value = tab.code;
};

const getChart = (target: HTMLDivElement | undefined, current: typeof summaryChart) => {
  if (!target) return undefined;
  if (!current.value) current.value = echarts.init(target);
  return current.value;
};

const renderCharts = async () => {
  await nextTick();
  renderSummaryChart();
  renderModuleChart();
  renderHeatmapChart();
  renderFeedbackChart();
  window.requestAnimationFrame(resizeCharts);
};

const renderSummaryChart = () => {
  const chart = getChart(summaryChartRef.value, summaryChart);
  if (!chart) return;
  const value = Math.min(100, kpiValue('deptCoverage'));
  chart.setOption({
    color: [cssVar('--ip-primary-600')],
    series: [
      {
        type: 'gauge',
        radius: '92%',
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        progress: { show: true, roundCap: true, width: 12 },
        axisLine: { roundCap: true, lineStyle: { width: 12, color: [[1, cssVar('--ip-primary-100')]] } },
        pointer: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        anchor: { show: true, size: 10, itemStyle: { color: cssVar('--ip-primary-600') } },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: cssVar('--ip-primary-700'),
          fontSize: cssNumber('--ip-font-title', 24),
          fontWeight: 700,
          offsetCenter: [0, '4%']
        },
        title: { color: cssVar('--ip-neutral-500'), fontSize: cssNumber('--ip-font-caption', 12), offsetCenter: [0, '32%'] },
        data: [{ value, name: kpiItem('deptCoverage')?.hint || '部门覆盖' }]
      }
    ]
  });
};

const renderModuleChart = () => {
  const chart = getChart(moduleChartRef.value, moduleChart);
  if (!chart) return;
  const rows = dashboard.value.moduleActivity;
  const radius = cssNumber('--ip-radius-sm', 6);
  chart.setOption({
    color: [cssVar('--ip-primary-600'), cssVar('--ip-mod-resources')],
    grid: { left: 12, right: 12, top: 28, bottom: 20, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: rows.map((item) => item.label),
      axisLabel: { color: cssVar('--ip-neutral-500') },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: cssVar('--ip-neutral-200') } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: cssVar('--ip-neutral-500') },
      splitLine: { lineStyle: { color: cssVar('--ip-neutral-100') } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: rows.map((item) => item.value),
        lineStyle: { width: 3 },
        areaStyle: { color: cssVar('--ip-primary-100') },
        itemStyle: { borderWidth: 2, borderRadius: radius },
        label: { show: true, position: 'top', color: cssVar('--ip-neutral-700') }
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
    grid: { left: 96, right: 24, top: 34, bottom: 22 },
    xAxis: { type: 'category', data: columns.map((item) => item.label), splitArea: { show: true }, axisLabel: { color: cssVar('--ip-neutral-600') } },
    yAxis: { type: 'category', data: rows.map((item) => item.deptName), splitArea: { show: true }, axisLabel: { color: cssVar('--ip-neutral-600') } },
    visualMap: { min: 0, max: maxValue, show: false, inRange: { color: [cssVar('--ip-neutral-100'), cssVar('--ip-primary-300'), cssVar('--ip-primary-700')] } },
    series: [
      {
        type: 'heatmap',
        data,
        itemStyle: { opacity: 0.72, borderWidth: 2, borderColor: cssVar('--ip-neutral-0') },
        label: { show: true, color: cssVar('--ip-neutral-900') },
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: cssVar('--ip-primary-200') } }
      }
    ]
  });
};

const renderFeedbackChart = () => {
  const chart = getChart(feedbackChartRef.value, feedbackChart);
  if (!chart) return;
  const feedback = dashboard.value.feedback;
  chart.setOption({
    color: [cssVar('--ip-warning'), cssVar('--ip-primary-500'), cssVar('--ip-success'), cssVar('--ip-neutral-400')],
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['58%', '78%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
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
  summaryChart.value?.resize();
  moduleChart.value?.resize();
  heatmapChart.value?.resize();
  feedbackChart.value?.resize();
};

const loadDashboard = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const { data } = await getUsageDashboardOverview();
    overview.value = normalizeDashboard(data);
    await renderCharts();
  } catch {
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
  summaryChart.value?.dispose();
  moduleChart.value?.dispose();
  heatmapChart.value?.dispose();
  feedbackChart.value?.dispose();
});
</script>

<style scoped lang="scss">
@use './style.scss';
</style>
