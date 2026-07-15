import type { InfoResource } from '@/api/infoservice/types';

export type WorkspaceKey = 'note' | 'chat' | 'info';

export interface StudioTile {
  readonly key: WorkspaceKey;
  readonly name: string;
  readonly hint: string;
}

export const STUDIO_TILES: readonly StudioTile[] = Object.freeze([
  Object.freeze({ key: 'note', name: '我的笔记', hint: '记录摘录与想法' }),
  Object.freeze({ key: 'chat', name: '交流互动', hint: '查看公开讨论' }),
  Object.freeze({ key: 'info', name: '文件信息', hint: '查看资料与阅看记录' })
]);

export interface PanelState {
  readonly view: WorkspaceKey | null;
}

export const DEFAULT_PANEL_STATE: PanelState = Object.freeze({ view: null });

export type PanelAction =
  | { readonly type: 'clickTile'; readonly tile: StudioTile }
  | { readonly type: 'openWorkspace'; readonly view: WorkspaceKey }
  | { readonly type: 'closeWorkspace' };

/** 同项二次点击收起；切换功能时复用同一抽屉。 */
export const reducePanelState = (state: PanelState, action: PanelAction): PanelState => {
  switch (action.type) {
    case 'clickTile':
      return Object.freeze({ view: state.view === action.tile.key ? null : action.tile.key });
    case 'openWorkspace':
      return state.view === action.view ? state : Object.freeze({ view: action.view });
    case 'closeWorkspace':
      return state.view === null ? state : DEFAULT_PANEL_STATE;
    default:
      return state;
  }
};

export const DEFAULT_PANEL_WIDTH = 392;
export const PANEL_MIN_WIDTH = 320;
export const PANEL_MAX_WIDTH = 820;
export const PREVIEW_RAIL_WIDTH = 56;
export const PREVIEW_LAYOUT_GAP = 8;
export const RESERVED_READER_WIDTH = 650;

export const panelWidthLimit = (contentWidth: number): number => {
  const available = contentWidth - PREVIEW_RAIL_WIDTH - PREVIEW_LAYOUT_GAP - RESERVED_READER_WIDTH;
  return Math.min(PANEL_MAX_WIDTH, Math.max(PANEL_MIN_WIDTH, available));
};

export const shouldOverlayPanel = (contentWidth: number): boolean =>
  contentWidth < PANEL_MIN_WIDTH + PREVIEW_RAIL_WIDTH + PREVIEW_LAYOUT_GAP + RESERVED_READER_WIDTH;

export const clampPanelWidth = (width: number, contentWidth: number): number => {
  const normalized = Number.isFinite(width) ? width : width === Number.POSITIVE_INFINITY ? PANEL_MAX_WIDTH : DEFAULT_PANEL_WIDTH;
  return Math.round(Math.min(panelWidthLimit(contentWidth), Math.max(PANEL_MIN_WIDTH, normalized)));
};

export const formatFileSize = (size?: number): string => {
  if (!size) return '—';
  if (size < 1024) return `${size.toLocaleString('zh-CN')} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

export const formatDateTime = (value?: string): string => {
  if (!value) return '—';
  return value.replace('T', ' ').slice(0, 16);
};

const formatMetric = (value?: number): string => (value ? `${value.toLocaleString('zh-CN')} 次` : '—');

export interface ResourceInfoItem {
  readonly label: string;
  readonly value: string;
}

export const buildResourceInfoItems = (resource?: InfoResource): readonly ResourceInfoItem[] =>
  Object.freeze([
    { label: '资料分类', value: resource?.categoryName || '未分类' },
    { label: '原始文件', value: resource?.originalName || '—' },
    { label: '文件大小', value: formatFileSize(resource?.fileSize) },
    { label: '发布人', value: resource?.ownerName || resource?.createByName || '平台用户' },
    { label: '发布时间', value: formatDateTime(resource?.createTime) },
    { label: '阅看次数', value: formatMetric(resource?.viewCount) },
    { label: '下载次数', value: formatMetric(resource?.downloadCount) }
  ]);
