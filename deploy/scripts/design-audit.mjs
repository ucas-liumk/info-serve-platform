#!/usr/bin/env node
/**
 * 设计审计门禁（棘轮机制）—— 正本规范 docs/design/design-system.md §10
 *
 * 统计门户代码中硬编码色值 / font-size / border-radius 的“离散度”，
 * 与 plus-ui/design-audit-baseline.json 比较：任一指标超过基线即退出码 1（构建失败）。
 * 迁移取得进展后运行 `node design-audit.mjs --update-baseline` 下调基线；禁止手工上调。
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const SCAN_DIRS = ['plus-ui/src/views/portal', 'plus-ui/src/layout/portal', 'plus-ui/src/views/admin/portal'];
const BASELINE_FILE = path.join(ROOT, 'plus-ui', 'design-audit-baseline.json');
const TOKEN_FILE = path.join(ROOT, 'plus-ui/src/assets/styles/tokens.scss');

function* walk(dir) {
  for (const name of readdirSync(dir)) {
    const p = path.join(dir, name);
    if (statSync(p).isDirectory()) yield* walk(p);
    else if (/\.(vue|scss|css|ts)$/.test(name)) yield p;
  }
}

const tokenHex = new Set((readFileSync(TOKEN_FILE, 'utf8').match(/#[0-9a-fA-F]{3,8}\b/g) || []).map((s) => s.toLowerCase()));
const tokenVars = new Set(readFileSync(TOKEN_FILE, 'utf8').match(/--ip-(?:font|radius)-[\w-]+/g) || []);

function normalizeCssValue(value) {
  return value.replace(/\s+/g, '');
}

function isTokenVarRef(value, prefix) {
  const match = normalizeCssValue(value).match(/^var\((--ip-[\w-]+)\)$/);
  return Boolean(match && match[1].startsWith(prefix) && tokenVars.has(match[1]));
}

const hex = new Set();
const rgba = new Set();
const fontSizes = new Set();
const radii = new Set();
let files = 0;

for (const dir of SCAN_DIRS) {
  const abs = path.join(ROOT, dir);
  let entries;
  try {
    entries = [...walk(abs)];
  } catch {
    continue; // 目录可选
  }
  for (const f of entries) {
    files++;
    const text = readFileSync(f, 'utf8');
    for (const m of text.match(/#[0-9a-fA-F]{3,8}\b/g) || []) {
      const v = m.toLowerCase();
      if (!tokenHex.has(v)) hex.add(v); // 与令牌同值的硬编码同样计数？——迁移期先豁免同值，聚焦真离散
    }
    for (const m of text.match(/rgba?\([^)]+\)/g) || []) rgba.add(m.replace(/\s+/g, ''));
    for (const m of text.matchAll(/font-size:\s*([^;]+);/g)) {
      if (!isTokenVarRef(m[1], '--ip-font-')) fontSizes.add(`font-size:${normalizeCssValue(m[1])};`);
    }
    for (const m of text.matchAll(/border-radius:\s*([^;]+);/g)) {
      if (!isTokenVarRef(m[1], '--ip-radius-')) radii.add(`border-radius:${normalizeCssValue(m[1])};`);
    }
  }
}

const current = {
  hardcodedHex: hex.size,
  hardcodedRgba: rgba.size,
  fontSizeVariants: fontSizes.size,
  radiusVariants: radii.size
};

if (process.argv.includes('--update-baseline')) {
  writeFileSync(BASELINE_FILE, JSON.stringify(current, null, 2) + '\n');
  console.log('[design-audit] 基线已更新:', current);
  process.exit(0);
}

let baseline;
try {
  baseline = JSON.parse(readFileSync(BASELINE_FILE, 'utf8'));
} catch {
  console.error('[design-audit] 缺少基线文件，先运行: node deploy/scripts/design-audit.mjs --update-baseline');
  process.exit(1);
}

console.log(`[design-audit] 扫描 ${files} 个文件`);
let failed = false;
for (const [key, cur] of Object.entries(current)) {
  const base = baseline[key] ?? 0;
  const mark = cur > base ? '✗ 超基线' : cur < base ? '↓ 可下调基线' : '=';
  console.log(`  ${key.padEnd(18)} 当前 ${String(cur).padStart(4)} / 基线 ${String(base).padStart(4)}  ${mark}`);
  if (cur > base) failed = true;
}

if (failed) {
  console.error('\n[design-audit] 失败：硬编码设计值超过棘轮基线。新样式必须使用 tokens.scss 令牌（--ip-*）。');
  process.exit(1);
}
console.log('[design-audit] 通过。');
