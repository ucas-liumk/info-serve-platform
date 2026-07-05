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
