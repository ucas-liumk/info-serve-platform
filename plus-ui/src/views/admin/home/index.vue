<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="dashboard-header">
      <div>
        <h1>后台监控</h1>
      </div>
      <div class="header-actions">
        <span>更新时间：{{ lastUpdated || '-' }}</span>
        <el-button :icon="Refresh" type="primary" @click="loadDashboard">刷新</el-button>
      </div>
    </section>

    <el-alert v-if="loadError" class="dashboard-alert" :title="loadError" type="warning" show-icon :closable="false" />

    <section class="metric-grid">
      <article v-for="item in metricCards" :key="item.label" class="metric-card">
        <div class="metric-icon" :style="{ color: item.color, backgroundColor: item.background }">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="metric-body">
          <span>{{ item.label }}</span>
          <strong>{{ formatNumber(item.value) }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </article>
    </section>

    <section class="panel-grid">
      <div class="dashboard-panel">
        <div class="panel-header">
          <div>
            <h2>各单位用户数分布</h2>
            <p>按用户所属单位统计，最多展示前 8 个单位</p>
          </div>
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div ref="deptChartRef" class="chart"></div>
      </div>

      <div class="dashboard-panel">
        <div class="panel-header">
          <div>
            <h2>近期登录状态</h2>
            <p>基于最近登录记录统计成功与失败情况</p>
          </div>
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div ref="loginChartRef" class="chart"></div>
      </div>
    </section>

    <section class="table-grid">
      <div class="dashboard-panel">
        <div class="panel-header compact">
          <div>
            <h2>最近登录</h2>
            <p>最近 {{ recentLoginRows.length }} 条登录记录</p>
          </div>
          <el-icon><List /></el-icon>
        </div>
        <el-table :data="recentLoginRows" height="286" empty-text="暂无登录记录">
          <el-table-column prop="userName" label="用户" min-width="110" show-overflow-tooltip />
          <el-table-column prop="loginLocation" label="登录地点" min-width="140" show-overflow-tooltip />
          <el-table-column prop="browser" label="浏览器" min-width="110" show-overflow-tooltip />
          <el-table-column prop="loginTime" label="时间" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="88" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? 'success' : 'danger'" effect="light">
                {{ row.status === '0' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="dashboard-panel">
        <div class="panel-header compact">
          <div>
            <h2>最近操作</h2>
            <p>最近 {{ operationRecords.length }} 条操作记录</p>
          </div>
          <el-icon><Monitor /></el-icon>
        </div>
        <el-table :data="operationRecords" height="286" empty-text="暂无操作记录">
          <el-table-column prop="title" label="模块" min-width="120" show-overflow-tooltip />
          <el-table-column prop="operName" label="操作人" min-width="100" show-overflow-tooltip />
          <el-table-column prop="requestMethod" label="方式" width="82" align="center" />
          <el-table-column prop="operTime" label="时间" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="88" align="center">
            <template #default="{ row }">
              <el-tag :type="Number(row.status) === 0 ? 'success' : 'danger'" effect="light">
                {{ Number(row.status) === 0 ? '正常' : '异常' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup name="Index" lang="ts">
import { getCurrentInstance, markRaw, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, computed } from 'vue';
import type { ComponentInternalInstance } from 'vue';
import * as echarts from 'echarts';
import { Connection, List, Monitor, OfficeBuilding, Refresh, TrendCharts, User, UserFilled } from '@element-plus/icons-vue';
import { listUser } from '@/api/system/user';
import { listDept } from '@/api/system/dept';
import { list as listOnline } from '@/api/monitor/online';
import { list as listLoginInfo } from '@/api/monitor/loginInfo';
import { list as listOperLog } from '@/api/monitor/operlog';
import type { UserVO } from '@/api/system/user/types';
import type { DeptVO } from '@/api/system/dept/types';
import type { OnlineVO } from '@/api/monitor/online/types';
import type { LoginInfoVO } from '@/api/monitor/loginInfo/types';
import type { OperLogVO } from '@/api/monitor/operlog/types';

type ChartInstance = ReturnType<typeof echarts.init>;

interface ListResult<T> {
  rows?: T[];
  data?: T[];
  total?: number;
}

const MAX_STAT_PAGE_SIZE = 10000;
const LOGIN_STAT_PAGE_SIZE = 100;
const RECENT_TABLE_SIZE = 8;

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const loadError = ref('');
const lastUpdated = ref('');
const userRows = ref<UserVO[]>([]);
const deptRows = ref<DeptVO[]>([]);
const loginRows = ref<LoginInfoVO[]>([]);
const operationRecords = ref<OperLogVO[]>([]);
const totalUsers = ref(0);
const onlineTotal = ref(0);
const todayLoginTotal = ref(0);

const deptChartRef = ref<HTMLDivElement>();
const loginChartRef = ref<HTMLDivElement>();
const deptChart = shallowRef<ChartInstance>();
const loginChart = shallowRef<ChartInstance>();

const metricIcons = {
  users: markRaw(User),
  active: markRaw(UserFilled),
  online: markRaw(Connection),
  login: markRaw(TrendCharts)
};

const enabledUsers = computed(() => userRows.value.filter((item) => item.status === '0').length);
const disabledUsers = computed(() => userRows.value.filter((item) => item.status !== '0').length);
const deptTotal = computed(() => flattenDepartments(deptRows.value).length);
const recentLoginRows = computed(() => loginRows.value.slice(0, RECENT_TABLE_SIZE));

const metricCards = computed(() => [
  {
    label: '系统用户数',
    value: totalUsers.value,
    hint: `覆盖 ${formatNumber(deptTotal.value)} 个单位`,
    icon: metricIcons.users,
    color: '#2563eb',
    background: '#eff6ff'
  },
  {
    label: '正常用户数',
    value: enabledUsers.value,
    hint: `停用 ${formatNumber(disabledUsers.value)} 人`,
    icon: metricIcons.active,
    color: '#16a34a',
    background: '#ecfdf3'
  },
  {
    label: '在线用户数',
    value: onlineTotal.value,
    hint: '当前会话在线',
    icon: metricIcons.online,
    color: '#7c3aed',
    background: '#f5f3ff'
  },
  {
    label: '今日登录次数',
    value: todayLoginTotal.value,
    hint: '按登录日志统计',
    icon: metricIcons.login,
    color: '#ea580c',
    background: '#fff7ed'
  }
]);

const deptUserDistribution = computed(() => {
  const countMap = new Map<string, number>();

  userRows.value.forEach((user) => {
    const deptName = user.deptName || '未分配单位';
    countMap.set(deptName, (countMap.get(deptName) || 0) + 1);
  });

  return [...countMap.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((left, right) => right.value - left.value)
    .slice(0, 8);
});

const loginStatusDistribution = computed(() => {
  const success = loginRows.value.filter((item) => item.status === '0').length;
  const failed = loginRows.value.filter((item) => item.status !== '0').length;
  return [
    { name: '成功', value: success },
    { name: '失败', value: failed }
  ].filter((item) => item.value > 0);
});

const formatNumber = (value: number | string | undefined) => {
  return Number(value || 0).toLocaleString();
};

const formatDateTime = (value: Date | string | number = new Date()) => {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const getRows = <T,>(result: unknown): T[] => {
  const response = result as ListResult<T>;
  if (Array.isArray(response.rows)) {
    return response.rows;
  }
  if (Array.isArray(response.data)) {
    return response.data;
  }
  return [];
};

const getTotal = <T,>(result: unknown, fallbackRows: T[]) => {
  const response = result as ListResult<T>;
  return Number(response.total ?? fallbackRows.length);
};

const flattenDepartments = (departments: DeptVO[]) => {
  const result: DeptVO[] = [];
  const walk = (items: DeptVO[]) => {
    items.forEach((item) => {
      result.push(item);
      if (item.children?.length) {
        walk(item.children);
      }
    });
  };

  walk(departments);
  return result;
};

const getTodayRange = () => {
  const start = new Date();
  start.setHours(0, 0, 0, 0);

  const end = new Date();
  end.setHours(23, 59, 59, 999);

  return [formatDateTime(start), formatDateTime(end)];
};

const buildDateRangeQuery = (query: Record<string, any>, range: string[]) => {
  return proxy?.addDateRange ? proxy.addDateRange(query, range) : { ...query, params: { beginTime: range[0], endTime: range[1] } };
};

const safeLoad = async <T,>(label: string, loader: () => Promise<T>, failedLabels: string[]) => {
  try {
    return await loader();
  } catch (error) {
    console.warn(`${label}加载失败`, error);
    failedLabels.push(label);
    return undefined;
  }
};

const loadDashboard = async () => {
  loading.value = true;
  loadError.value = '';
  const failedLabels: string[] = [];
  const todayRange = getTodayRange();

  const [userResult, deptResult, onlineResult, loginResult, todayLoginResult, operResult] = await Promise.all([
    safeLoad('用户数据', () => listUser({ pageNum: 1, pageSize: MAX_STAT_PAGE_SIZE } as any), failedLabels),
    safeLoad('单位数据', () => listDept({} as any), failedLabels),
    safeLoad('在线用户', () => listOnline({ pageNum: 1, pageSize: MAX_STAT_PAGE_SIZE } as any), failedLabels),
    safeLoad(
      '登录日志',
      () => listLoginInfo({ pageNum: 1, pageSize: LOGIN_STAT_PAGE_SIZE, orderByColumn: 'login_time', isAsc: 'descending' } as any),
      failedLabels
    ),
    safeLoad(
      '今日登录',
      () => listLoginInfo(buildDateRangeQuery({ pageNum: 1, pageSize: 1, orderByColumn: 'login_time', isAsc: 'descending' }, todayRange) as any),
      failedLabels
    ),
    safeLoad(
      '操作日志',
      () => listOperLog({ pageNum: 1, pageSize: RECENT_TABLE_SIZE, orderByColumn: 'oper_time', isAsc: 'descending' } as any),
      failedLabels
    )
  ]);

  const users = getRows<UserVO>(userResult);
  const depts = getRows<DeptVO>(deptResult);
  const online = getRows<OnlineVO>(onlineResult);
  const logins = getRows<LoginInfoVO>(loginResult);
  const todayLogins = getRows<LoginInfoVO>(todayLoginResult);
  const operations = getRows<OperLogVO>(operResult);

  userRows.value = users;
  deptRows.value = depts;
  loginRows.value = logins;
  operationRecords.value = operations;
  totalUsers.value = getTotal<UserVO>(userResult, users);
  onlineTotal.value = getTotal<OnlineVO>(onlineResult, online);
  todayLoginTotal.value = getTotal<LoginInfoVO>(todayLoginResult, todayLogins);
  lastUpdated.value = formatDateTime();
  loadError.value = failedLabels.length ? `${failedLabels.join('、')}加载失败，其余数据已显示` : '';

  loading.value = false;
  await renderCharts();
};

const renderCharts = async () => {
  await nextTick();
  renderDeptChart();
  renderLoginChart();
};

const renderDeptChart = () => {
  if (!deptChartRef.value) {
    return;
  }
  if (!deptChart.value) {
    deptChart.value = echarts.init(deptChartRef.value);
  }

  const chartData = deptUserDistribution.value;
  if (!chartData.length) {
    setEmptyChart(deptChart.value);
    return;
  }

  deptChart.value.setOption({
    color: ['#2563eb'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: 36,
      right: 20,
      top: 28,
      bottom: 54
    },
    xAxis: {
      type: 'category',
      data: chartData.map((item) => item.name),
      axisTick: { show: false },
      axisLabel: {
        color: '#64748b',
        interval: 0,
        rotate: chartData.length > 4 ? 24 : 0
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#edf2f7' } }
    },
    series: [
      {
        name: '用户数',
        type: 'bar',
        barMaxWidth: 34,
        data: chartData.map((item) => item.value),
        itemStyle: {
          borderRadius: [6, 6, 0, 0]
        }
      }
    ]
  });
};

const renderLoginChart = () => {
  if (!loginChartRef.value) {
    return;
  }
  if (!loginChart.value) {
    loginChart.value = echarts.init(loginChartRef.value);
  }

  const chartData = loginStatusDistribution.value;
  if (!chartData.length) {
    setEmptyChart(loginChart.value);
    return;
  }

  loginChart.value.setOption({
    color: ['#16a34a', '#dc2626'],
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br />{c} 次 ({d}%)'
    },
    legend: {
      bottom: 8,
      icon: 'circle',
      textStyle: { color: '#475569' }
    },
    series: [
      {
        name: '登录状态',
        type: 'pie',
        radius: ['48%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: {
          formatter: '{b}: {c}',
          color: '#334155'
        },
        data: chartData
      }
    ]
  });
};

const setEmptyChart = (chart: ChartInstance) => {
  chart.clear();
  chart.setOption({
    title: {
      text: '暂无数据',
      left: 'center',
      top: 'middle',
      textStyle: {
        color: '#94a3b8',
        fontSize: 14,
        fontWeight: 400
      }
    }
  });
};

const resizeCharts = () => {
  deptChart.value?.resize();
  loginChart.value?.resize();
};

onMounted(() => {
  loadDashboard();
  window.addEventListener('resize', resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  deptChart.value?.dispose();
  loginChart.value?.dispose();
});
</script>

<style lang="scss" scoped>
.dashboard-page {
  min-height: calc(100vh - 84px);
  padding: 18px;
  color: #0f172a;
  background: #f5f7fb;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    line-height: 1.35;
  }

  p {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 13px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.dashboard-alert {
  margin-bottom: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 112px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgb(15 23 42 / 5%);
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex: none;
  font-size: 24px;
}

.metric-body {
  min-width: 0;

  span,
  em {
    display: block;
    color: #64748b;
    font-size: 13px;
    font-style: normal;
  }

  strong {
    display: block;
    margin: 5px 0;
    font-size: 28px;
    font-weight: 700;
    line-height: 1.15;
    color: #0f172a;
  }
}

.panel-grid,
.table-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.dashboard-panel {
  min-width: 0;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgb(15 23 42 / 5%);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    line-height: 1.35;
  }

  p {
    margin: 4px 0 0;
    color: #64748b;
    font-size: 12px;
  }

  .el-icon {
    color: #2563eb;
    font-size: 20px;
  }
}

.panel-header.compact {
  margin-bottom: 10px;
}

.chart {
  width: 100%;
  height: 320px;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .dashboard-header,
  .header-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-grid,
  .table-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .dashboard-page {
    padding: 12px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
