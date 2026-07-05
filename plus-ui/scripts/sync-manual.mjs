import { copyFile, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const plusUiDir = path.resolve(scriptDir, '..');
const rootDir = path.resolve(plusUiDir, '..');
const source = path.join(rootDir, 'README.md');
const targetDir = path.join(plusUiDir, 'public');
const target = path.join(targetDir, 'readme.md');

await mkdir(targetDir, { recursive: true });
await copyFile(source, target);

console.log(`[sync:manual] ${path.relative(rootDir, source)} -> ${path.relative(rootDir, target)}`);
