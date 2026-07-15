import { describe, expect, it } from 'vitest';
import type { InfoResource } from '@/api/infoservice/types';
import {
  buildResourceInfoItems,
  clampPanelWidth,
  DEFAULT_PANEL_STATE,
  DEFAULT_PANEL_WIDTH,
  formatDateTime,
  formatFileSize,
  PANEL_MAX_WIDTH,
  PANEL_MIN_WIDTH,
  panelWidthLimit,
  reducePanelState,
  shouldOverlayPanel,
  STUDIO_TILES
} from './panelStudio';

const tileByKey = (key: 'note' | 'chat' | 'info') => {
  const tile = STUDIO_TILES.find((item) => item.key === key);
  if (!tile) throw new Error(`missing tile: ${key}`);
  return tile;
};

describe('文件预览功能轨', () => {
  it('只呈现三个已实现工作区', () => {
    expect(STUDIO_TILES.map((tile) => tile.key)).toEqual(['note', 'chat', 'info']);
    expect(Object.isFrozen(STUDIO_TILES)).toBe(true);
  });

  it('默认仅显示功能轨，同项二次点击收起', () => {
    expect(DEFAULT_PANEL_STATE).toEqual({ view: null });
    const opened = reducePanelState(DEFAULT_PANEL_STATE, { type: 'clickTile', tile: tileByKey('note') });
    expect(opened).toEqual({ view: 'note' });
    expect(reducePanelState(opened, { type: 'clickTile', tile: tileByKey('note') })).toEqual({ view: null });
  });

  it('切换工作区、引用打开笔记和显式关闭均为不可变转换', () => {
    const chat = reducePanelState(DEFAULT_PANEL_STATE, { type: 'clickTile', tile: tileByKey('chat') });
    expect(reducePanelState(chat, { type: 'clickTile', tile: tileByKey('info') })).toEqual({ view: 'info' });
    expect(reducePanelState(chat, { type: 'openWorkspace', view: 'note' })).toEqual({ view: 'note' });
    expect(reducePanelState(chat, { type: 'closeWorkspace' })).toEqual(DEFAULT_PANEL_STATE);
    expect(reducePanelState(DEFAULT_PANEL_STATE, { type: 'closeWorkspace' })).toBe(DEFAULT_PANEL_STATE);
    expect(Object.isFrozen(chat)).toBe(true);
  });
});

describe('抽屉宽度约束', () => {
  it('使用 392 默认值与 320–820 绝对范围', () => {
    expect(DEFAULT_PANEL_WIDTH).toBe(392);
    expect(PANEL_MIN_WIDTH).toBe(320);
    expect(PANEL_MAX_WIDTH).toBe(820);
  });

  it('动态上限扣除功能轨、间隙与 650px 阅读区', () => {
    expect(panelWidthLimit(1440)).toBe(726);
    expect(panelWidthLimit(1600)).toBe(820);
    expect(clampPanelWidth(900, 1440)).toBe(726);
    expect(clampPanelWidth(500.6, 1440)).toBe(501);
  });

  it('空间不足时标记覆盖式抽屉并保持绝对最小宽', () => {
    expect(shouldOverlayPanel(1024)).toBe(true);
    expect(shouldOverlayPanel(1200)).toBe(false);
    expect(panelWidthLimit(900)).toBe(PANEL_MIN_WIDTH);
    expect(clampPanelWidth(100, 1440)).toBe(PANEL_MIN_WIDTH);
  });

  it('非法输入回退默认值，无穷大钳到动态上限', () => {
    expect(clampPanelWidth(Number.NaN, 1440)).toBe(DEFAULT_PANEL_WIDTH);
    expect(clampPanelWidth(Infinity, 1440)).toBe(726);
  });
});

describe('资料信息格式', () => {
  const resource = {
    categoryName: '技术文档',
    originalName: 'GB/T 37490-2019.pdf',
    fileSize: 3.7 * 1024 * 1024,
    ownerName: '疯狂的狮子Li',
    createTime: '2026-07-11T09:00:00',
    viewCount: 1234,
    downloadCount: 1
  } as InfoResource;

  it('文件大小与普通时间使用规范格式', () => {
    expect(formatFileSize(512)).toBe('512 B');
    expect(formatFileSize(2048)).toBe('2.0 KB');
    expect(formatFileSize(0)).toBe('—');
    expect(formatDateTime('2026-07-11T10:32:00.000')).toBe('2026-07-11 10:32');
    expect(formatDateTime()).toBe('—');
  });

  it('数字使用千分位，零值显示破折号', () => {
    expect(buildResourceInfoItems(resource)).toEqual([
      { label: '资料分类', value: '技术文档' },
      { label: '原始文件', value: 'GB/T 37490-2019.pdf' },
      { label: '文件大小', value: '3.7 MB' },
      { label: '发布人', value: '疯狂的狮子Li' },
      { label: '发布时间', value: '2026-07-11 09:00' },
      { label: '阅看次数', value: '1,234 次' },
      { label: '下载次数', value: '1 次' }
    ]);
    expect(buildResourceInfoItems(undefined).slice(-2)).toEqual([
      { label: '阅看次数', value: '—' },
      { label: '下载次数', value: '—' }
    ]);
  });
});
