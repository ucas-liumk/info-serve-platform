import {
  defineConfig,
  presetAttributify,
  presetIcons,
  presetTypography,
  presetUno,
  presetWebFonts,
  transformerDirectives,
  transformerVariantGroup
} from 'unocss';

export default defineConfig({
  safelist: [
    // 模块导航配置中的动态图标类必须登记于此（UnoCSS 静态扫描不可见）——
    // IconRail 通过 :class="d.icon" 运行时字符串绑定，来源 src/layout/portal-shell/configs/resources.ts
    'i-material-symbols:library-books-outline-rounded',
    'i-material-symbols:forum-outline-rounded',
    'i-material-symbols:upload-file-outline-rounded',
    'i-material-symbols:star-outline-rounded',
    // CategoryRail 折叠/展开切换图标（当前为静态 class，一并登记以防未来动态化）
    'i-material-symbols:left-panel-close-outline-rounded',
    'i-material-symbols:left-panel-open-outline-rounded'
  ],
  shortcuts: {
    'panel-title':
      'pb-[5px] font-sans leading-[1.1] font-medium text-base text-[#6379bb] border-b border-b-solid border-[var(--el-border-color-light)] mb-5 mt-0'
  },
  theme: {
    colors: {
      // 设计令牌映射（正本：docs/design/design-system.md；原子类颜色只允许用这些键）
      primary: 'var(--ip-primary-600)',
      primary_dark: 'var(--ip-primary-300)',
      success: 'var(--ip-success)',
      warning: 'var(--ip-warning)',
      danger: 'var(--ip-danger)',
      'neutral-1': 'var(--ip-neutral-900)',
      'neutral-2': 'var(--ip-neutral-700)',
      'neutral-3': 'var(--ip-neutral-500)',
      'neutral-4': 'var(--ip-neutral-400)',
      'surface-page': 'var(--ip-neutral-50)',
      'surface-card': 'var(--ip-neutral-0)',
      line: 'var(--ip-neutral-200)'
    }
  },
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons(),
    presetTypography(),
    presetWebFonts({
      fonts: {}
    })
  ],
  transformers: [transformerDirectives(), transformerVariantGroup()]
});
