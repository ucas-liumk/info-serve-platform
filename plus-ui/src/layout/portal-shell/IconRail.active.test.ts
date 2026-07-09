import { describe, expect, it } from 'vitest';
import { pickActiveDomain } from './pickActiveDomain';
import type { NavDomain, NavShortcut } from './types';

const domains: NavDomain[] = [
  { key: 'center', label: '资料中心', icon: 'i-x', route: '/portal/resources' },
  { key: 'community', label: '交流互动', icon: 'i-x', route: '/portal/resources/community', badge: 'soon' }
];
const shortcuts: NavShortcut[] = [
  { key: 'mine', label: '我的上传', icon: 'i-x', route: '/portal/resources', query: { scope: 'mine' } },
  { key: 'favorites', label: '我的收藏', icon: 'i-x', route: '/portal/resources', query: { scope: 'favorites' } }
];

describe('pickActiveDomain（最长前缀胜出）', () => {
  it('嵌套子路由只命中最深的域（community 不双高亮 center）', () => {
    expect(pickActiveDomain('/portal/resources/community', '', domains, shortcuts)).toBe('community');
  });

  it('父路径精确命中父域（center）', () => {
    expect(pickActiveDomain('/portal/resources', '', domains, shortcuts)).toBe('center');
  });

  it('捷径 scope 激活时域不抢高亮（返回空）', () => {
    expect(pickActiveDomain('/portal/resources', 'mine', domains, shortcuts)).toBe('');
  });
});
