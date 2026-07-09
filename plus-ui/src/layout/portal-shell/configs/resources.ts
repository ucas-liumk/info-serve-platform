import type { CategoryItem, ModuleNavConfig } from '../types';

async function fetchCategories(): Promise<CategoryItem[]> {
  const { listResourceCategories } = await import('@/api/portal/resources');
  const res = await listResourceCategories();
  const rows = (res as any).data ?? [];
  // 字段名以 api/portal/resources.ts 实际类型为准（categoryCode/categoryName/资源计数），
  // 若类型命名不同，按实际字段映射，禁止改 API 层
  return rows.map((c: any) => ({ key: c.categoryCode, label: c.categoryName, count: c.resourceCount ?? c.count }));
}

export const resourcesNav: ModuleNavConfig = {
  moduleKey: 'resources',
  domains: [
    { key: 'center', label: '资料中心', icon: 'i-material-symbols:library-books-outline-rounded', route: '/portal/resources' },
    { key: 'community', label: '交流互动', icon: 'i-material-symbols:forum-outline-rounded', route: '/portal/resources/community', badge: 'soon' }
  ],
  shortcuts: [
    { key: 'mine', label: '我的上传', icon: 'i-material-symbols:upload-file-outline-rounded', route: '/portal/resources', query: { scope: 'mine' } },
    { key: 'favorites', label: '我的收藏', icon: 'i-material-symbols:star-outline-rounded', route: '/portal/resources', query: { scope: 'favorites' } }
  ],
  category: { title: '分类', queryKey: 'category', fetch: fetchCategories }
};
