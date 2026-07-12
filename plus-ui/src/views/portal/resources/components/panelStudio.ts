/**
 * 预览页右栏「功能区」纯函数与磁贴配置（spec §5.6）。
 * 磁贴配置驱动：新增能力时向 STUDIO_TILES 追加条目即可（status 'soon' → 'active' 接入即亮）。
 */
import type { InfoResource } from '@/api/infoservice/types';

export type WorkspaceKey = 'note' | 'chat';
export type TileStatus = 'active' | 'soon';
export type TileTone = 'blue' | 'green' | 'amber' | 'purple' | 'cyan' | 'pink';

export interface StudioTile {
  readonly key: string;
  readonly name: string;
  readonly icon: string;
  readonly tone: TileTone;
  readonly status: TileStatus;
}

/** 六磁贴定稿：我的笔记/交流互动可用，OCR/朗读/摘要/导图为「即将上线」占位 */
export const STUDIO_TILES: readonly StudioTile[] = Object.freeze([
  Object.freeze({ key: 'note', name: '我的笔记', icon: '📝', tone: 'blue', status: 'active' } as StudioTile),
  Object.freeze({ key: 'chat', name: '交流互动', icon: '💬', tone: 'green', status: 'active' } as StudioTile),
  Object.freeze({ key: 'ocr', name: 'OCR 识别', icon: '🔍', tone: 'amber', status: 'soon' } as StudioTile),
  Object.freeze({ key: 'tts', name: '语音朗读', icon: '🔊', tone: 'purple', status: 'soon' } as StudioTile),
  Object.freeze({ key: 'summary', name: '智能摘要', icon: '✨', tone: 'cyan', status: 'soon' } as StudioTile),
  Object.freeze({ key: 'mindmap', name: '思维导图', icon: '🧠', tone: 'pink', status: 'soon' } as StudioTile)
]);

/** 默认工作区=交流互动（用户定稿） */
export const DEFAULT_WORKSPACE: WorkspaceKey = 'chat';

const WORKSPACE_KEYS: readonly WorkspaceKey[] = Object.freeze(['note', 'chat']);

/** 磁贴是否为可切换的工作区磁贴（active 且落在已实现工作区集合内） */
export const isWorkspaceTile = (tile: StudioTile): boolean => tile.status === 'active' && (WORKSPACE_KEYS as readonly string[]).includes(tile.key);

/** 工作区状态归约：点 active 磁贴切换，点 soon 磁贴维持现状（返回新值，不改入参） */
export const reduceWorkspace = (current: WorkspaceKey, tile: StudioTile): WorkspaceKey =>
  isWorkspaceTile(tile) ? (tile.key as WorkspaceKey) : current;

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
