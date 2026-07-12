/**
 * 预览页右栏「功能区」纯函数与磁贴配置（spec §5.6）。
 * 磁贴配置驱动：新增能力时向 STUDIO_TILES 追加条目即可（status 'soon' → 'active' 接入即亮）。
 */
import type { InfoResource } from '@/api/infoservice/types';

export type WorkspaceKey = 'note' | 'chat' | 'info';
export type TileStatus = 'active' | 'soon';
export type TileTone = 'blue' | 'green' | 'amber' | 'purple' | 'cyan' | 'pink';

export interface StudioTile {
  readonly key: string;
  readonly name: string;
  readonly icon: string;
  readonly tone: TileTone;
  readonly status: TileStatus;
}

/**
 * 七磁贴定稿：我的笔记/交流互动/文件信息可用，OCR/朗读/摘要/导图为「即将上线」占位。
 * icon 为 @element-plus/icons-vue 组件名（政务风线性图标，组件侧经映射表渲染）。
 */
export const STUDIO_TILES: readonly StudioTile[] = Object.freeze([
  Object.freeze({ key: 'note', name: '我的笔记', icon: 'Notebook', tone: 'blue', status: 'active' } as StudioTile),
  Object.freeze({ key: 'chat', name: '交流互动', icon: 'ChatDotRound', tone: 'green', status: 'active' } as StudioTile),
  Object.freeze({ key: 'info', name: '文件信息', icon: 'Document', tone: 'cyan', status: 'active' } as StudioTile),
  Object.freeze({ key: 'ocr', name: 'OCR 识别', icon: 'Aim', tone: 'amber', status: 'soon' } as StudioTile),
  Object.freeze({ key: 'tts', name: '语音朗读', icon: 'Headset', tone: 'purple', status: 'soon' } as StudioTile),
  Object.freeze({ key: 'summary', name: '智能摘要', icon: 'MagicStick', tone: 'pink', status: 'soon' } as StudioTile),
  Object.freeze({ key: 'mindmap', name: '思维导图', icon: 'Share', tone: 'blue', status: 'soon' } as StudioTile)
]);

const WORKSPACE_KEYS: readonly WorkspaceKey[] = Object.freeze(['note', 'chat', 'info']);

/** 磁贴是否为可切换的工作区磁贴（active 且落在已实现工作区集合内） */
export const isWorkspaceTile = (tile: StudioTile): boolean => tile.status === 'active' && (WORKSPACE_KEYS as readonly string[]).includes(tile.key);

/** 面板视图：功能区总览（只有磁贴）或某个独占展开的工作区 */
export type PanelView = 'overview' | WorkspaceKey;

export interface PanelState {
  readonly view: PanelView;
  readonly collapsed: boolean;
}

/** 默认态=功能区总览、展开（点磁贴才进工作区，关闭工作区回总览） */
export const DEFAULT_PANEL_STATE: PanelState = Object.freeze({ view: 'overview', collapsed: false });

export type PanelAction =
  | { readonly type: 'clickTile'; readonly tile: StudioTile }
  | { readonly type: 'closeWorkspace' }
  | { readonly type: 'toggleCollapse' };

/**
 * 面板状态归约（纯函数，返回新对象不改入参；空操作返回原对象便于响应式短路）：
 * - clickTile：active 磁贴 → 独占展开该工作区并顺带展开面板（收起轨点击即恢复）；soon 磁贴 → 维持现状
 * - closeWorkspace：回到功能区总览
 * - toggleCollapse：整栏收起/展开，保留当前工作区
 */
export const reducePanelState = (state: PanelState, action: PanelAction): PanelState => {
  switch (action.type) {
    case 'clickTile': {
      if (!isWorkspaceTile(action.tile)) return state;
      const view = action.tile.key as WorkspaceKey;
      if (state.view === view && !state.collapsed) return state;
      return Object.freeze({ view, collapsed: false });
    }
    case 'closeWorkspace':
      if (state.view === 'overview') return state;
      return Object.freeze({ view: 'overview', collapsed: state.collapsed });
    case 'toggleCollapse':
      return Object.freeze({ view: state.view, collapsed: !state.collapsed });
    default:
      return state;
  }
};

/** 文件大小分档展示；空值/0 显示占位符 */
export const formatFileSize = (size?: number): string => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

/** ISO 时间去 T 截到秒；空值显示占位符 */
export const formatDateTime = (value?: string): string => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
};

export interface ResourceInfoItem {
  readonly label: string;
  readonly value: string;
}

/** ⓘ 弹层「资料信息」条目（分类/原始文件/大小/发布人/发布时间/阅看下载合并），逐项回退占位 */
export const buildResourceInfoItems = (resource?: InfoResource): readonly ResourceInfoItem[] =>
  Object.freeze([
    { label: '资料分类', value: resource?.categoryName || '未分类' },
    { label: '原始文件', value: resource?.originalName || '-' },
    { label: '大小', value: formatFileSize(resource?.fileSize) },
    { label: '发布人', value: resource?.ownerName || resource?.createByName || '平台用户' },
    { label: '发布时间', value: formatDateTime(resource?.createTime) },
    { label: '阅看/下载', value: `${resource?.viewCount || 0} 次 / ${resource?.downloadCount || 0} 次` }
  ]);
