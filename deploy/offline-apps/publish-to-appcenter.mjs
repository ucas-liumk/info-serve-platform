#!/usr/bin/env node
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT_DIR = path.dirname(fileURLToPath(import.meta.url));
const BASE_URL = (process.env.APP_CENTER_BASE_URL || 'http://127.0.0.1:7010/prod-api').replace(/\/$/, '');
const CLIENT_ID = process.env.APP_CENTER_CLIENT_ID || 'e5cd7e4891bf95d1d19206ce24a7b32e';
const TENANT_ID = process.env.APP_CENTER_TENANT_ID || '000000';
const USERNAME = process.env.APP_CENTER_ADMIN_USER || 'admin';
const PASSWORD = process.env.APP_CENTER_ADMIN_PASSWORD;
const PACKAGE_ROOT = process.env.APP_CENTER_PACKAGE_ROOT;
const PUBLISH_STATUS = process.env.APP_CENTER_PUBLISH_STATUS || '1';
const reqPubB64 = 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';
const aesKey = 'RuoYiCloudPlusPGMigrate012345678';

if (!PASSWORD) {
  throw new Error('必须通过 APP_CENTER_ADMIN_PASSWORD 提供管理员密码');
}
if (!PACKAGE_ROOT || !fs.statSync(PACKAGE_ROOT, { throwIfNoEntry: false })?.isDirectory()) {
  throw new Error('APP_CENTER_PACKAGE_ROOT 必须指向离线包根目录');
}
if (!['0', '1'].includes(PUBLISH_STATUS)) {
  throw new Error('APP_CENTER_PUBLISH_STATUS 只能是 0（上架）或 1（下架）');
}

const catalog = JSON.parse(fs.readFileSync(path.join(ROOT_DIR, 'catalog.json'), 'utf8'));
const definitions = [...catalog.packages, ...(catalog.reserve || [])]
  .filter((item) => item.source === 'kylin-apt');

const appMetadata = {
  'xournalpp-arm64': ['Xournal++', '手写笔记、PDF 批注与教学板书工具。', '手写笔记,PDF批注,教学', 'X', '0'],
  'ocr-toolkit-zh-arm64': ['简体中文 OCR 工具包', '包含 OCRmyPDF、gImageReader 与简体中文识别数据。', 'OCR,简体中文,PDF', 'OCR', '0'],
  'vlc-arm64': ['VLC 媒体播放器', '支持常见音视频格式和网络媒体流的播放器。', '视频,音频,播放器', 'VLC', '0'],
  'gimp-arm64': ['GIMP 图像处理', '开源位图编辑、图片修复与设计工具。', '图像处理,设计,图片编辑', 'G', '0'],
  'inkscape-arm64': ['Inkscape 矢量绘图', '开源 SVG 矢量图、图标和插图制作工具。', 'SVG,矢量图,设计', 'INK', '0'],
  'keepassxc-arm64': ['KeePassXC 密码库', '本地离线密码管理与加密数据库工具。', '密码管理,安全,离线', 'KP', '1'],
  'flameshot-arm64': ['Flameshot 截图标注', '截图、箭头、文字和区域标注工具。', '截图,标注,辅助办公', 'FS', '0'],
  'remmina-arm64': ['Remmina 远程桌面', '支持 RDP、VNC 等协议的远程桌面客户端。', '远程桌面,RDP,VNC', 'R', '1'],
  'filezilla-arm64': ['FileZilla', 'FTP、FTPS 与 SFTP 文件传输客户端。', 'FTP,SFTP,文件传输', 'FZ', '1']
};

function encryptedBody(data) {
  const cipher = crypto.createCipheriv('aes-256-ecb', Buffer.from(aesKey, 'utf8'), null);
  let payload = cipher.update(JSON.stringify(data), 'utf8', 'base64');
  payload += cipher.final('base64');
  const publicKey = crypto.createPublicKey({
    key: Buffer.from(reqPubB64, 'base64'),
    format: 'der',
    type: 'spki'
  });
  const encKey = crypto.publicEncrypt(
    { key: publicKey, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(Buffer.from(aesKey, 'utf8').toString('base64'), 'utf8')
  ).toString('base64');
  return { payload, encKey };
}

async function api(apiPath, { method = 'GET', token, body, encrypted = false } = {}) {
  const headers = { clientid: CLIENT_ID };
  if (token) headers.Authorization = `Bearer ${token}`;
  let requestBody;
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json;charset=UTF-8';
    if (encrypted) {
      const value = encryptedBody(body);
      headers['encrypt-key'] = value.encKey;
      requestBody = value.payload;
    } else {
      requestBody = JSON.stringify(body);
    }
  }
  const response = await fetch(`${BASE_URL}${apiPath}`, { method, headers, body: requestBody });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || result.code !== 200) {
    throw new Error(`${method} ${apiPath} 失败：HTTP ${response.status} ${result.msg || JSON.stringify(result)}`);
  }
  return result.data;
}

async function login() {
  const data = await api('/auth/login', {
    method: 'POST',
    encrypted: true,
    body: {
      clientId: CLIENT_ID,
      grantType: 'password',
      tenantId: TENANT_ID,
      username: USERNAME,
      password: PASSWORD
    }
  });
  if (!data?.access_token) throw new Error('登录成功但未返回 access_token');
  return data.access_token;
}

function findArchive(code) {
  const directory = path.join(PACKAGE_ROOT, code);
  const files = fs.readdirSync(directory, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith('.tar.gz'))
    .map((entry) => entry.name)
    .sort();
  if (files.length !== 1) {
    throw new Error(`${code} 必须且只能有一个 .tar.gz，实际为 ${files.length}`);
  }
  return path.join(directory, files[0]);
}

function archiveVersion(code, archive) {
  return path.basename(archive)
    .replace(`${code}-`, '')
    .replace('-kylin-v10sp1-arm64.tar.gz', '');
}

async function uploadArchive(token, archive) {
  const form = new FormData();
  form.append('file', await fs.openAsBlob(archive), path.basename(archive));
  const response = await fetch(`${BASE_URL}/file/oss/upload`, {
    method: 'POST',
    headers: {
      clientid: CLIENT_ID,
      Authorization: `Bearer ${token}`
    },
    body: form
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || result.code !== 200) {
    throw new Error(`上传 ${path.basename(archive)} 失败：HTTP ${response.status} ${result.msg || JSON.stringify(result)}`);
  }
  return result.data;
}

async function ensureOfflineCategory(token) {
  let categories = await api('/appcenter/category/list', { token });
  let category = categories.find((item) => item.categoryCode === 'offline');
  if (!category) {
    await api('/appcenter/category', {
      method: 'POST',
      token,
      body: {
        categoryName: '离线应用',
        categoryCode: 'offline',
        icon: 'download',
        status: '0',
        orderNum: 3,
        remark: '银河麒麟 ARM64 离线安装包'
      }
    });
    categories = await api('/appcenter/category/list', { token });
    category = categories.find((item) => item.categoryCode === 'offline');
  }
  if (!category) throw new Error('无法创建或读取离线应用分类');
  return category.categoryId;
}

async function main() {
  const token = await login();
  const categoryId = await ensureOfflineCategory(token);
  const listResponse = await fetch(`${BASE_URL}/appcenter/application/list?pageNum=1&pageSize=500`, {
    headers: { clientid: CLIENT_ID, Authorization: `Bearer ${token}` }
  });
  const listResult = await listResponse.json();
  if (!listResponse.ok || !Array.isArray(listResult.rows)) throw new Error('无法读取应用列表');
  const existingByCode = new Map(listResult.rows.map((item) => [item.appCode, item]));

  for (const [index, definition] of definitions.entries()) {
    const archive = findArchive(definition.code);
    const stat = fs.statSync(archive);
    const existing = existingByCode.get(definition.code);
    let packageInfo;
    if (existing?.packageName === path.basename(archive) && Number(existing.packageSize) === stat.size) {
      packageInfo = {
        ossId: existing.packageOssId,
        fileName: existing.packageName,
        url: existing.packageUrl
      };
      console.log(`复用已上传包：${definition.code}`);
    } else {
      packageInfo = await uploadArchive(token, archive);
      console.log(`上传完成：${definition.code}`);
    }
    const [name, description, tags, icon, isSecurity] = appMetadata[definition.code];
    const payload = {
      appName: name,
      appCode: definition.code,
      version: archiveVersion(definition.code, archive),
      categoryId,
      icon,
      accent: '#0f766e',
      description,
      tags,
      accessUrl: '',
      appType: 'offline',
      packageOssId: packageInfo.ossId,
      packageName: packageInfo.fileName,
      packageSize: stat.size,
      packageUrl: packageInfo.url,
      status: PUBLISH_STATUS,
      isSecurity,
      accessMode: 'all',
      roleIds: [],
      userIds: [],
      orderNum: 201 + index,
      remark: '银河麒麟 V10 SP1 / 飞腾 ARM64 离线包'
    };
    if (existing) {
      await api('/appcenter/application', { method: 'PUT', token, body: { ...payload, appId: existing.appId } });
    } else {
      await api('/appcenter/application', { method: 'POST', token, body: payload });
    }
    console.log(`${existing ? '更新' : '新增'}应用：${definition.code}，状态=${PUBLISH_STATUS}`);
  }
  return token;
}

async function verifyPublished(token) {
  const response = await fetch(`${BASE_URL}/appcenter/portal/apps?pageNum=1&pageSize=500&appType=offline`, {
    headers: { clientid: CLIENT_ID, Authorization: `Bearer ${token}` }
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !Array.isArray(result.rows)) {
    throw new Error(`离线应用门户列表验证失败：HTTP ${response.status}`);
  }
  const expectedPackages = new Set(definitions.map((item) => path.basename(findArchive(item.code))));
  const published = result.rows.filter((item) => expectedPackages.has(item.packageName));
  if (published.length !== expectedPackages.size) {
    throw new Error(`离线应用门户列表不完整：期望 ${expectedPackages.size}，实际 ${published.length}`);
  }

  const sample = published[0];
  const download = await fetch(`${BASE_URL}/appcenter/portal/apps/${sample.appId}/package/download`, {
    headers: { clientid: CLIENT_ID, Authorization: `Bearer ${token}` }
  });
  if (!download.ok || !download.body) {
    throw new Error(`离线包下载验证失败：HTTP ${download.status}`);
  }
  const reader = download.body.getReader();
  const firstChunk = await reader.read();
  await reader.cancel();
  if (firstChunk.done || !firstChunk.value?.length) {
    throw new Error('离线包下载验证失败：响应体为空');
  }
  console.log(`门户验证通过：${published.length} 个离线应用，${sample.packageName} 下载首块 ${firstChunk.value.length} 字节`);
}

const token = await main();
await verifyPublished(token);
