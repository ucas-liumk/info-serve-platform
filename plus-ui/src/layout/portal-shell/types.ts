export interface NavDomain { key: string; label: string; icon: string; route: string; badge?: 'soon' }
export interface NavShortcut { key: string; label: string; icon: string; route: string; query: Record<string, string> }
// icon = UnoCSS presetIcons 类名（Iconify 数据源），如 'i-material-symbols:library-books-outline-rounded'
// ——纯字符串保证配置/解析器可在 node 环境单测，渲染层 <span :class="icon"> 即出图标
export interface CategoryItem { key: string; label: string; count?: number }
export interface ModuleNavConfig {
  moduleKey: string;                    // 'resources'，storage 键与高亮匹配用
  domains: NavDomain[];
  shortcuts: NavShortcut[];
  category?: { title: string; queryKey: string; fetch: () => Promise<CategoryItem[]> };
}
