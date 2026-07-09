import type { ModuleNavConfig } from './types';
import { resourcesNav } from './configs/resources';

const registry: Record<string, ModuleNavConfig> = {
  resources: resourcesNav
};

export function resolveModuleNav(moduleKey?: string): ModuleNavConfig | null {
  if (!moduleKey) return null;
  return registry[moduleKey] ?? null;
}
