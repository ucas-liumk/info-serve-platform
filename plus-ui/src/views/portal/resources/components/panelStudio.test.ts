import { describe, expect, it } from 'vitest';
import type { InfoResource } from '@/api/infoservice/types';
import {
  buildResourceInfoItems,
  DEFAULT_WORKSPACE,
  formatDateTime,
  formatFileSize,
  isWorkspaceTile,
  reduceWorkspace,
  STUDIO_TILES
} from './panelStudio';

const tileByKey = (key: string) => {
  const tile = STUDIO_TILES.find((item) => item.key === key);
  if (!tile) throw new Error(`missing tile: ${key}`);
  return tile;
};

describe('STUDIO_TILES 配置', () => {
  it('按原型顺序给出六个磁贴', () => {
    expect(STUDIO_TILES.map((tile) => tile.key)).toEqual(['note', 'chat', 'ocr', 'tts', 'summary', 'mindmap']);
  });

  it('我的笔记/交流互动为 active，其余四个为 soon', () => {
    expect(STUDIO_TILES.filter((tile) => tile.status === 'active').map((tile) => tile.key)).toEqual(['note', 'chat']);
    expect(STUDIO_TILES.filter((tile) => tile.status === 'soon').map((tile) => tile.key)).toEqual(['ocr', 'tts', 'summary', 'mindmap']);
  });

  it('色调与图标按定稿逐一对应', () => {
    expect(STUDIO_TILES.map((tile) => tile.tone)).toEqual(['blue', 'green', 'amber', 'purple', 'cyan', 'pink']);
    expect(STUDIO_TILES.map((tile) => tile.icon)).toEqual(['📝', '💬', '🔍', '🔊', '✨', '🧠']);
  });

  it('配置为冻结只读数组（不可变）', () => {
    expect(Object.isFrozen(STUDIO_TILES)).toBe(true);
    expect(Object.isFrozen(STUDIO_TILES[0])).toBe(true);
  });
});

describe('isWorkspaceTile / reduceWorkspace', () => {
  it('默认工作区是交流互动', () => {
    expect(DEFAULT_WORKSPACE).toBe('chat');
  });

  it('active 磁贴是工作区磁贴，soon 磁贴不是', () => {
    expect(isWorkspaceTile(tileByKey('note'))).toBe(true);
    expect(isWorkspaceTile(tileByKey('chat'))).toBe(true);
    expect(isWorkspaceTile(tileByKey('ocr'))).toBe(false);
    expect(isWorkspaceTile(tileByKey('mindmap'))).toBe(false);
  });

  it('点 active 磁贴切换工作区', () => {
    expect(reduceWorkspace('chat', tileByKey('note'))).toBe('note');
    expect(reduceWorkspace('note', tileByKey('chat'))).toBe('chat');
  });

  it('点 soon 磁贴保持当前工作区不变', () => {
    expect(reduceWorkspace('chat', tileByKey('ocr'))).toBe('chat');
    expect(reduceWorkspace('note', tileByKey('summary'))).toBe('note');
  });
});

describe('formatFileSize', () => {
  it('空值与 0 显示占位符', () => {
    expect(formatFileSize(undefined)).toBe('-');
    expect(formatFileSize(0)).toBe('-');
  });

  it('按 B/KB/MB 分档', () => {
    expect(formatFileSize(512)).toBe('512 B');
    expect(formatFileSize(2048)).toBe('2.0 KB');
    expect(formatFileSize(3.7 * 1024 * 1024)).toBe('3.7 MB');
  });
});

describe('formatDateTime', () => {
  it('空值显示占位符', () => {
    expect(formatDateTime(undefined)).toBe('-');
    expect(formatDateTime('')).toBe('-');
  });

  it('去掉 ISO T 分隔并截到秒', () => {
    expect(formatDateTime('2026-07-11T10:32:00.000')).toBe('2026-07-11 10:32:00');
    expect(formatDateTime('2026-07-11 10:32:00')).toBe('2026-07-11 10:32:00');
  });
});

describe('buildResourceInfoItems', () => {
  const resource = {
    resourceId: 1,
    title: 'GB/T 37490-2019',
    description: '',
    categoryId: 300002,
    categoryName: '技术文档',
    ossId: 1,
    originalName: 'GB/T 37490-2019.pdf',
    fileSuffix: 'pdf',
    mimeType: 'application/pdf',
    fileSize: 3.7 * 1024 * 1024,
    previewType: 'pdf',
    downloadCount: 1,
    viewCount: 3,
    status: '0',
    ownerName: '疯狂的狮子Li',
    createTime: '2026-07-11T09:00:00'
  } as InfoResource;

  it('产出六项资料信息（分类/原始文件/大小/发布人/发布时间/阅看下载合并）', () => {
    expect(buildResourceInfoItems(resource)).toEqual([
      { label: '资料分类', value: '技术文档' },
      { label: '原始文件', value: 'GB/T 37490-2019.pdf' },
      { label: '大小', value: '3.7 MB' },
      { label: '发布人', value: '疯狂的狮子Li' },
      { label: '发布时间', value: '2026-07-11 09:00:00' },
      { label: '阅看/下载', value: '3 次 / 1 次' }
    ]);
  });

  it('缺字段时逐项回退占位', () => {
    expect(buildResourceInfoItems(undefined)).toEqual([
      { label: '资料分类', value: '未分类' },
      { label: '原始文件', value: '-' },
      { label: '大小', value: '-' },
      { label: '发布人', value: '平台用户' },
      { label: '发布时间', value: '-' },
      { label: '阅看/下载', value: '0 次 / 0 次' }
    ]);
  });

  it('发布人回退顺序 ownerName → createByName → 平台用户', () => {
    const noOwner = { ...resource, ownerName: undefined, createByName: '张工' } as InfoResource;
    expect(buildResourceInfoItems(noOwner)[3]).toEqual({ label: '发布人', value: '张工' });
  });
});
