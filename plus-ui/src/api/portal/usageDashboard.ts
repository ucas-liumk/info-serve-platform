import request from '@/utils/request';
import { AxiosPromise } from 'axios';

export interface DashboardKpi {
  code: string;
  label: string;
  value: number;
  unit: string;
  hint: string;
}

export interface DashboardMetric {
  code: string;
  label: string;
  value: number;
  hint: string;
}

export interface DashboardRankItem {
  name: string;
  category: string;
  value: number;
  unit: string;
}

export interface HeatmapColumn {
  code: string;
  label: string;
}

export interface HeatmapCell {
  columnCode: string;
  value: number;
}

export interface HeatmapRow {
  deptId: number;
  deptName: string;
  total: number;
  cells: HeatmapCell[];
}

export interface DashboardHeatmap {
  ready: boolean;
  emptyReason: string;
  columns: HeatmapColumn[];
  rows: HeatmapRow[];
}

export interface DashboardFeedback {
  total: number;
  pending: number;
  processing: number;
  resolved: number;
  closed: number;
  typeStats: DashboardMetric[];
}

export interface DashboardSignal {
  level: 'success' | 'warning' | 'danger' | 'info';
  title: string;
  action: string;
}

export interface UsageDashboardOverview {
  title: string;
  subtitle: string;
  periodLabel: string;
  scopeLabel: string;
  generatedAt: string;
  kpis: DashboardKpi[];
  moduleActivity: DashboardMetric[];
  resourceRanking: DashboardRankItem[];
  appRanking: DashboardRankItem[];
  heatmap: DashboardHeatmap;
  feedback: DashboardFeedback;
  signals: DashboardSignal[];
}

export function getUsageDashboardOverview(): AxiosPromise<UsageDashboardOverview> {
  return request({ url: '/infoservice/portal/usage-dashboard/overview', method: 'get' });
}
