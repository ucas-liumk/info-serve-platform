#!/usr/bin/env node
import crypto from 'node:crypto';
import http from 'node:http';
import https from 'node:https';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const BASE_URL = process.env.APP_CENTER_BASE_URL || 'http://127.0.0.1:19100/prod-api';
const CLIENT_ID = process.env.APP_CENTER_CLIENT_ID || 'e5cd7e4891bf95d1d19206ce24a7b32e';
const TENANT_ID = process.env.APP_CENTER_TENANT_ID || '000000';
const PG_CONTAINER = process.env.APP_CENTER_PG_CONTAINER || 'infosys-ruoyi-cloud-plus-postgres';
const ROOT_DIR = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', '..');
const REPORT_DIR = path.join(ROOT_DIR, 'output', 'appcenter-tests');
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const REPORT_FILE = path.join(REPORT_DIR, `v1-e2e-${RUN_ID}.json`);

const reqPubB64 = 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';
const aesKey = 'RuoYiCloudPlusPGMigrate012345678';

const categories = [
  { categoryName: '自研应用', categoryCode: 'self_hosted', icon: 'component', orderNum: 1 },
  { categoryName: '开源应用', categoryCode: 'open_source', icon: 'open', orderNum: 2 },
  { categoryName: '离线应用', categoryCode: 'offline', icon: 'download', orderNum: 3 }
];

const localApps = [
  { appName: '应知应会', appCode: 'required-knowledge', version: 'v1', categoryCode: 'self_hosted', appType: 'business', icon: 'education', accent: '#2563eb', description: '内部学习、题库、考试与材料导入的自研应用入口。', tags: '自研应用,题库,考试', accessUrl: '/portal/required-knowledge', accessMode: 'user', userIds: [1], orderNum: 1 },
  { appName: 'Stirling PDF', appCode: 'openapps-stirling-pdf', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'PDF', accent: 'blue', description: '开源 PDF 应用，支持拆分、合并、压缩、转换等常用文档处理。', tags: 'PDF,文档处理,转换', accessUrl: 'http://127.0.0.1:18080', orderNum: 101 },
  { appName: 'Memos', appCode: 'openapps-memos', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'M', accent: 'green', description: '轻量级开源备忘录和知识记录应用，适合个人知识沉淀。', tags: '备忘录,知识协作,记录', accessUrl: 'http://127.0.0.1:18081', orderNum: 102 },
  { appName: 'Draw.io', appCode: 'openapps-drawio', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'D', accent: 'orange', description: '在线流程图、架构图和业务图绘制应用。', tags: '流程图,绘图,架构图', accessUrl: 'http://127.0.0.1:18082', orderNum: 103 },
  { appName: 'File Browser', appCode: 'openapps-filebrowser', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'F', accent: 'cyan', description: '网页端文件管理器，支持目录浏览、上传、下载和基础管理。', tags: '文件管理,浏览器,上传下载', accessUrl: 'http://127.0.0.1:18083', orderNum: 104 },
  { appName: 'Baserow', appCode: 'openapps-baserow', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'B', accent: 'teal', description: '开源无代码数据库和在线表格系统，适合快速搭建业务数据表。', tags: '低代码,表格,数据库', accessUrl: 'http://127.0.0.1:18084', orderNum: 105 },
  { appName: 'BookStack', appCode: 'openapps-bookstack', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'BK', accent: 'amber', description: '结构化团队知识库，按书籍、章节和页面组织内容。', tags: '知识库,文档,团队协作', accessUrl: 'http://127.0.0.1:18085', orderNum: 106 },
  { appName: 'Wiki.js', appCode: 'openapps-wikijs', version: '2.x', categoryCode: 'open_source', appType: 'online', icon: 'W', accent: 'slate', description: '现代化开源 Wiki 系统，适合构建内部知识门户。', tags: 'Wiki,知识库,门户', accessUrl: 'http://127.0.0.1:18086', orderNum: 107 },
  { appName: 'HedgeDoc', appCode: 'openapps-hedgedoc', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'H', accent: 'black', description: '实时协作 Markdown 文档应用，适合会议纪要和协作写作。', tags: 'Markdown,协作文档,实时协作', accessUrl: 'http://127.0.0.1:18087', orderNum: 108 },
  { appName: 'NocoBase', appCode: 'openapps-nocobase', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'N', accent: 'violet', description: '开源无代码/低代码平台，用于快速搭建内部业务系统。', tags: '低代码,业务系统,应用搭建', accessUrl: 'http://127.0.0.1:18088', orderNum: 109 },
  { appName: 'Seafile', appCode: 'openapps-seafile', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'S', accent: 'blue', description: '开源文件同步和共享平台，适合团队文件协作。', tags: '文件协作,网盘,同步', accessUrl: 'http://127.0.0.1:18089', orderNum: 110 },
  { appName: 'Excalidraw', appCode: 'openapps-excalidraw', version: 'latest', categoryCode: 'open_source', appType: 'online', icon: 'EX', accent: 'red', description: '手绘风白板应用，适合快速画流程、草图和方案图。', tags: '白板,绘图,草图', accessUrl: 'http://127.0.0.1:18090', orderNum: 111 }
];

const results = [];
const expectedAccessUrls = new Map();

function log(message) {
  console.log(message);
}

function pass(id, detail = '') {
  results.push({ id, status: 'PASS', detail });
  console.log(`PASS ${id}${detail ? ` - ${detail}` : ''}`);
}

function assert(condition, id, detail) {
  if (!condition) {
    const error = new Error(`${id}: ${detail}`);
    error.testId = id;
    throw error;
  }
  pass(id, detail);
}

function encryptPayload(data) {
  const body = JSON.stringify(data);
  const cipher = crypto.createCipheriv('aes-256-ecb', Buffer.from(aesKey, 'utf8'), null);
  let encBody = cipher.update(body, 'utf8', 'base64');
  encBody += cipher.final('base64');
  const aesKeyB64 = Buffer.from(aesKey, 'utf8').toString('base64');
  const pubKey = crypto.createPublicKey({ key: Buffer.from(reqPubB64, 'base64'), format: 'der', type: 'spki' });
  const encKey = crypto.publicEncrypt(
    { key: pubKey, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(aesKeyB64, 'utf8')
  ).toString('base64');
  return { payload: encBody, encKey };
}

function request(method, apiPath, options = {}) {
  const url = new URL(`${BASE_URL.replace(/\/$/, '')}${apiPath.startsWith('/') ? apiPath : `/${apiPath}`}`);
  const lib = url.protocol === 'https:' ? https : http;
  let payload = null;
  const headers = { clientid: CLIENT_ID };
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json;charset=UTF-8';
    if (options.encrypted) {
      const encrypted = encryptPayload(options.body);
      payload = Buffer.from(encrypted.payload, 'utf8');
      headers['encrypt-key'] = encrypted.encKey;
    } else {
      payload = Buffer.from(JSON.stringify(options.body), 'utf8');
    }
    headers['Content-Length'] = payload.length;
  }
  return new Promise((resolve, reject) => {
    const req = lib.request(url, { method, headers, timeout: options.timeout || 15000 }, (res) => {
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        let parsed = data;
        try {
          parsed = data ? JSON.parse(data) : {};
        } catch {
          parsed = { raw: data };
        }
        resolve({ status: res.statusCode, body: parsed, raw: data });
      });
    });
    req.on('timeout', () => {
      req.destroy(new Error(`request timeout: ${method} ${url.href}`));
    });
    req.on('error', reject);
    if (payload) {
      req.write(payload);
    }
    req.end();
  });
}

function expectR(response, id) {
  assert(response.status >= 200 && response.status < 300, id, `HTTP ${response.status}`);
  assert(response.body && response.body.code === 200, id, JSON.stringify(response.body).slice(0, 300));
  return response.body.data;
}

function legacyAbsoluteUrl(pathname) {
  const url = new URL(BASE_URL);
  url.pathname = pathname;
  url.search = '';
  url.hash = '';
  return url.toString();
}

async function writeApp(token, method, payload, id) {
  let response = await request(method, '/appcenter/application', { token, body: payload });
  if (
    response.body?.code !== 200 &&
    payload.appCode === 'required-knowledge' &&
    String(response.body?.msg || '').includes('http(s)://') &&
    String(payload.accessUrl || '').startsWith('/')
  ) {
    payload.accessUrl = legacyAbsoluteUrl(payload.accessUrl);
    response = await request(method, '/appcenter/application', { token, body: payload });
  }
  expectedAccessUrls.set(payload.appCode, payload.accessUrl);
  expectR(response, id);
}

function expectNotOk(response, id) {
  const notOk = response.status < 200 || response.status >= 300 || !response.body || response.body.code !== 200;
  assert(notOk, id, JSON.stringify(response.body).slice(0, 300));
}

async function login(username, password) {
  const response = await request('POST', '/auth/login', {
    encrypted: true,
    body: {
      clientId: CLIENT_ID,
      grantType: 'password',
      tenantId: TENANT_ID,
      username,
      password
    }
  });
  const data = expectR(response, `AUTH-LOGIN-${username}`);
  assert(Boolean(data?.access_token), `AUTH-TOKEN-${username}`, 'access_token exists');
  return data.access_token;
}

async function registerUser(username, password, phonenumber, expectSuccess) {
  const response = await request('POST', '/auth/register', {
    encrypted: true,
    body: {
      clientId: CLIENT_ID,
      grantType: 'password',
      tenantId: TENANT_ID,
      username,
      password,
      phonenumber,
      userType: 'sys_user'
    }
  });
  if (expectSuccess) {
    expectR(response, `AUTH-REGISTER-${username}`);
  } else {
    expectNotOk(response, `AUTH-REGISTER-MISSING-PHONE-${username}`);
  }
}

async function getAdminApps(token) {
  const response = await request('GET', '/appcenter/application/list?pageNum=1&pageSize=500', { token });
  assert(Array.isArray(response.body?.rows), 'ADMIN-APP-LIST', 'admin application list returns rows');
  return response.body.rows;
}

async function getPortalApps(token, query = '') {
  const response = await request('GET', `/appcenter/portal/apps?pageNum=1&pageSize=500${query}`, { token });
  assert(Array.isArray(response.body?.rows), 'PORTAL-APP-LIST', `portal list total=${response.body?.total ?? 0}`);
  return response.body;
}

async function getFavorites(token) {
  const response = await request('GET', '/appcenter/portal/favorites?pageNum=1&pageSize=500', { token });
  assert(Array.isArray(response.body?.rows), 'FAV-LIST', `favorites total=${response.body?.total ?? 0}`);
  return response.body.rows;
}

async function ensureCategories(token) {
  const response = await request('GET', '/appcenter/category/list', { token });
  const current = expectR(response, 'ADMIN-CAT-LIST') || [];
  const map = new Map(current.map((item) => [item.categoryCode, item]));
  for (const category of categories) {
    if (!map.has(category.categoryCode)) {
      const add = await request('POST', '/appcenter/category', {
        token,
        body: { ...category, status: '0', remark: 'App Center V1 自动化测试分类' }
      });
      expectR(add, `ADMIN-CAT-ADD-${category.categoryCode}`);
    }
  }
  const latest = expectR(await request('GET', '/appcenter/category/list', { token }), 'ADMIN-CAT-LIST-AFTER') || [];
  const latestMap = new Map(latest.map((item) => [item.categoryCode, item]));
  for (const category of categories) {
    assert(latestMap.has(category.categoryCode), `DATA-CAT-${category.categoryCode}`, 'category exists');
  }
  return latestMap;
}

async function ensureLocalApps(token, categoryMap) {
  let apps = await getAdminApps(token);
  const appMap = new Map(apps.map((item) => [item.appCode, item]));
  for (const local of localApps) {
    const categoryId = categoryMap.get(local.categoryCode)?.categoryId;
    assert(Boolean(categoryId), `DATA-CATEGORY-FOR-${local.appCode}`, local.categoryCode);
    const payload = {
      appName: local.appName,
      appCode: local.appCode,
      version: local.version,
      categoryId,
      icon: local.icon,
      accent: local.accent,
      description: local.description,
      tags: local.tags,
      accessUrl: local.accessUrl,
      appType: local.appType || 'online',
      accessMode: local.accessMode || 'all',
      roleIds: local.roleIds || [],
      userIds: local.userIds || [],
      isSecurity: '0',
      status: '0',
      orderNum: local.orderNum,
      remark: 'App Center V1 本地开源应用测试数据'
    };
    if (appMap.has(local.appCode)) {
      const existing = appMap.get(local.appCode);
      await writeApp(token, 'PUT', { ...payload, appId: existing.appId }, `ADMIN-APP-UPDATE-${local.appCode}`);
    } else {
      await writeApp(token, 'POST', payload, `ADMIN-APP-ADD-${local.appCode}`);
    }
  }
  apps = await getAdminApps(token);
  const latestMap = new Map(apps.map((item) => [item.appCode, item]));
  for (const local of localApps) {
    const app = latestMap.get(local.appCode);
    assert(Boolean(app), `DATA-APP-${local.appCode}`, 'app exists');
    assert(app.status === '0', `DATA-APP-STATUS-${local.appCode}`, 'app status online');
    assert(app.accessUrl === expectedAccessUrls.get(local.appCode), `DATA-APP-URL-${local.appCode}`, app.accessUrl);
    assert(app.appType === local.appType, `DATA-APP-TYPE-${local.appCode}`, app.appType);
  }
  return latestMap;
}

async function getPortalAppById(token, appId) {
  const data = await getPortalApps(token);
  return data.rows.find((item) => Number(item.appId) === Number(appId));
}

function dbScalar(sql) {
  return execFileSync('docker', ['exec', PG_CONTAINER, 'psql', '-U', 'ruoyi', '-d', 'ry-cloud', '-At', '-c', sql], {
    encoding: 'utf8'
  }).trim();
}

async function main() {
  fs.mkdirSync(REPORT_DIR, { recursive: true });
  log('=== App Center V1 E2E ===');
  log(`baseUrl=${BASE_URL}`);

  const adminToken = await login('admin', 'admin123');
  const categoryMap = await ensureCategories(adminToken);
  const appMap = await ensureLocalApps(adminToken, categoryMap);

  const timestamp = Date.now().toString().slice(-10);
  const username = `appuser${timestamp}`;
  const password = 'App@123456';
  const phonenumber = `139${timestamp.slice(-8)}`;
  await registerUser(`nop${timestamp}`, password, '', false);
  await registerUser(username, password, phonenumber, true);
  const userToken = await login(username, password);

  const savedPhone = dbScalar(`SELECT COALESCE(phonenumber, '') FROM sys_user WHERE user_name='${username}' ORDER BY user_id DESC LIMIT 1;`);
  assert(savedPhone === phonenumber, 'AUTH-PHONE-SAVED', savedPhone);

  const portalData = await getPortalApps(userToken);
  assert(portalData.total >= 12, 'DATA-APP-TOTAL-12', `total=${portalData.total}`);
  for (const local of localApps) {
    assert(portalData.rows.some((item) => item.appName === local.appName), `PORTAL-HAS-${local.appCode}`, local.appName);
  }

  const searchData = await getPortalApps(userToken, '&keyword=NocoBase');
  assert(searchData.rows.some((item) => item.appName === 'NocoBase'), 'PORTAL-SEARCH-NOCOBASE', 'keyword=NocoBase');

  const target = appMap.get('openapps-stirling-pdf');
  const beforeUse = await getPortalAppById(userToken, target.appId);
  const useResponse = await request('POST', `/appcenter/portal/apps/${target.appId}/use`, { token: userToken });
  const useUrl = expectR(useResponse, 'USE-APP');
  assert(useUrl === 'http://127.0.0.1:18080', 'USE-APP-URL', useUrl);
  const afterUse = await getPortalAppById(userToken, target.appId);
  assert(Number(afterUse.useCount) >= Number(beforeUse.useCount) + 1, 'USE-COUNT-INCREMENT', `${beforeUse.useCount} -> ${afterUse.useCount}`);

  expectR(await request('POST', `/appcenter/portal/apps/${target.appId}/favorite`, { token: userToken }), 'FAV-ADD-1');
  expectR(await request('POST', `/appcenter/portal/apps/${target.appId}/favorite`, { token: userToken }), 'FAV-ADD-IDEMPOTENT');
  let favorites = await getFavorites(userToken);
  assert(favorites.some((item) => Number(item.appId) === Number(target.appId)), 'FAV-MY-FAVORITES-HAS-APP', 'favorite visible');
  expectR(await request('DELETE', `/appcenter/portal/apps/${target.appId}/favorite`, { token: userToken }), 'FAV-REMOVE');
  favorites = await getFavorites(userToken);
  assert(!favorites.some((item) => Number(item.appId) === Number(target.appId)), 'FAV-REMOVE-HIDDEN', 'favorite removed');

  const recBefore = await getPortalAppById(userToken, target.appId);
  expectR(await request('POST', `/appcenter/portal/apps/${target.appId}/recommend`, { token: userToken }), 'REC-ADD-1');
  expectR(await request('POST', `/appcenter/portal/apps/${target.appId}/recommend`, { token: userToken }), 'REC-ADD-IDEMPOTENT');
  const recAfter = await getPortalAppById(userToken, target.appId);
  assert(Number(recAfter.recommendCount) === Number(recBefore.recommendCount) + 1, 'REC-COUNT-INCREMENT-ONCE', `${recBefore.recommendCount} -> ${recAfter.recommendCount}`);
  expectR(await request('DELETE', `/appcenter/portal/apps/${target.appId}/recommend`, { token: userToken }), 'REC-REMOVE');

  expectR(await request('POST', `/appcenter/portal/apps/${target.appId}/favorite`, { token: userToken }), 'FAV-RESTORE-FOR-STATUS');
  expectR(await request('PUT', '/appcenter/application/changeStatus', { token: adminToken, body: { appId: target.appId, status: '1' } }), 'ADMIN-APP-DOWN');
  favorites = await getFavorites(userToken);
  assert(!favorites.some((item) => Number(item.appId) === Number(target.appId)), 'FAV-HIDDEN-WHEN-DOWN', 'down app hidden');
  expectNotOk(await request('POST', `/appcenter/portal/apps/${target.appId}/use`, { token: userToken }), 'USE-DOWN-APP-BLOCKED');
  expectR(await request('PUT', '/appcenter/application/changeStatus', { token: adminToken, body: { appId: target.appId, status: '0' } }), 'ADMIN-APP-UP');
  favorites = await getFavorites(userToken);
  assert(favorites.some((item) => Number(item.appId) === Number(target.appId)), 'FAV-RESTORED-WHEN-UP', 'favorite relation preserved');

  const tempCode = `v1-e2e-temp-${timestamp}`;
  const tempPayload = {
    appName: `V1 E2E 临时应用 ${timestamp}`,
    appCode: tempCode,
    version: '1.0.0',
    categoryId: categoryMap.get('open_source').categoryId,
    icon: 'T',
    accent: 'gray',
    description: '自动化测试临时应用',
    tags: '自动化,测试',
    accessUrl: 'http://127.0.0.1:18080',
    appType: 'online',
    isSecurity: '0',
    status: '0',
    orderNum: 999,
    remark: 'V1 E2E 临时数据'
  };
  expectR(await request('POST', '/appcenter/application', { token: adminToken, body: tempPayload }), 'ADMIN-APP-ADD-TEMP');
  let adminApps = await getAdminApps(adminToken);
  let tempApp = adminApps.find((item) => item.appCode === tempCode);
  assert(Boolean(tempApp), 'ADMIN-APP-TEMP-LISTED', tempCode);
  expectR(await request('PUT', '/appcenter/application', {
    token: adminToken,
    body: { ...tempPayload, appId: tempApp.appId, appName: `${tempPayload.appName}-已编辑` }
  }), 'ADMIN-APP-EDIT-TEMP');
  expectR(await request('DELETE', `/appcenter/application/${tempApp.appId}`, { token: adminToken }), 'ADMIN-APP-LOGIC-DELETE-TEMP');
  adminApps = await getAdminApps(adminToken);
  assert(!adminApps.some((item) => item.appCode === tempCode), 'ADMIN-APP-TEMP-NOT-LISTED-AFTER-DELETE', tempCode);
  const delFlag = dbScalar(`SELECT del_flag FROM app_application WHERE app_code='${tempCode}' ORDER BY create_time DESC LIMIT 1;`);
  assert(delFlag === '1', 'ADMIN-APP-LOGIC-DELETE-FLAG', `del_flag=${delFlag}`);

  const forbidden = await request('POST', '/appcenter/application', { token: userToken, body: tempPayload });
  expectNotOk(forbidden, 'PERM-USER-CANNOT-ADD-APP');

  fs.writeFileSync(REPORT_FILE, JSON.stringify({ baseUrl: BASE_URL, runId: RUN_ID, results }, null, 2));
  log(`REPORT ${REPORT_FILE}`);
  log('=== APP CENTER V1 E2E PASSED ===');
}

main().catch((error) => {
  results.push({ id: error.testId || 'UNCAUGHT', status: 'FAIL', detail: error.message });
  fs.mkdirSync(REPORT_DIR, { recursive: true });
  fs.writeFileSync(REPORT_FILE, JSON.stringify({ baseUrl: BASE_URL, runId: RUN_ID, results }, null, 2));
  console.error(`FAIL ${error.message}`);
  console.error(`REPORT ${REPORT_FILE}`);
  process.exit(1);
});
