#!/usr/bin/env node
import crypto from 'node:crypto';
import http from 'node:http';
import https from 'node:https';
import fs from 'node:fs';
import path from 'node:path';

const BASE_URL = process.env.PHASE1_BASE_URL || 'http://127.0.0.1:19100/prod-api';
const CLIENT_ID = process.env.PHASE1_CLIENT_ID || 'e5cd7e4891bf95d1d19206ce24a7b32e';
const TENANT_ID = process.env.PHASE1_TENANT_ID || '000000';
const USERNAME = process.env.PHASE1_USERNAME || 'admin';
const PASSWORD = process.env.PHASE1_PASSWORD || 'admin123';
const ROOT_DIR = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', '..');
const REPORT_DIR = path.join(ROOT_DIR, 'output', 'phase1-smoke');
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const REPORT_FILE = path.join(REPORT_DIR, `phase1-smoke-${RUN_ID}.json`);

const reqPubB64 = 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';
const aesKey = 'RuoYiCloudPlusPGMigrate012345678';
const results = [];

function pass(id, detail = '') {
  results.push({ id, status: 'PASS', detail });
  console.log(`PASS ${id}${detail ? ` - ${detail}` : ''}`);
}

function fail(id, detail) {
  results.push({ id, status: 'FAIL', detail });
  throw new Error(`${id}: ${detail}`);
}

function assert(condition, id, detail) {
  if (!condition) fail(id, detail);
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
  if (options.token) headers.Authorization = `Bearer ${options.token}`;
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
        resolve({ status: res.statusCode, headers: res.headers, body: parsed, raw: data });
      });
    });
    req.on('timeout', () => req.destroy(new Error(`request timeout: ${method} ${url.href}`)));
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

function requestAbsolute(method, absoluteUrl, options = {}) {
  const url = new URL(absoluteUrl);
  const lib = url.protocol === 'https:' ? https : http;
  return new Promise((resolve, reject) => {
    const req = lib.request(url, { method, timeout: options.timeout || 15000 }, (res) => {
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, raw: data }));
    });
    req.on('timeout', () => req.destroy(new Error(`request timeout: ${method} ${url.href}`)));
    req.on('error', reject);
    req.end();
  });
}

function requestMultipart(method, apiPath, options = {}) {
  const url = new URL(`${BASE_URL.replace(/\/$/, '')}${apiPath.startsWith('/') ? apiPath : `/${apiPath}`}`);
  const lib = url.protocol === 'https:' ? https : http;
  const boundary = `----phase1-smoke-${crypto.randomBytes(8).toString('hex')}`;
  const parts = [];
  for (const file of options.files || []) {
    parts.push(Buffer.from(
      `--${boundary}\r\n` +
      `Content-Disposition: form-data; name="${file.name}"; filename="${file.filename}"\r\n` +
      `Content-Type: ${file.contentType || 'application/octet-stream'}\r\n\r\n`
    ));
    parts.push(Buffer.isBuffer(file.content) ? file.content : Buffer.from(String(file.content), 'utf8'));
    parts.push(Buffer.from('\r\n'));
  }
  parts.push(Buffer.from(`--${boundary}--\r\n`));
  const payload = Buffer.concat(parts);
  const headers = {
    clientid: CLIENT_ID,
    'Content-Type': `multipart/form-data; boundary=${boundary}`,
    'Content-Length': payload.length
  };
  if (options.token) headers.Authorization = `Bearer ${options.token}`;
  return new Promise((resolve, reject) => {
    const req = lib.request(url, { method, headers, timeout: options.timeout || 20000 }, (res) => {
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
        resolve({ status: res.statusCode, headers: res.headers, body: parsed, raw: data });
      });
    });
    req.on('timeout', () => req.destroy(new Error(`request timeout: ${method} ${url.href}`)));
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

function expectR(response, id) {
  assert(response.status >= 200 && response.status < 300, `${id}-HTTP`, `HTTP ${response.status}`);
  assert(response.body?.code === 200, `${id}-CODE`, JSON.stringify(response.body).slice(0, 300));
  return response.body.data;
}

function expectTable(response, id) {
  assert(response.status >= 200 && response.status < 300, `${id}-HTTP`, `HTTP ${response.status}`);
  assert(Array.isArray(response.body?.rows), `${id}-ROWS`, `total=${response.body?.total ?? 0}`);
  return response.body.rows;
}

async function login() {
  const response = await request('POST', '/auth/login', {
    encrypted: true,
    body: {
      clientId: CLIENT_ID,
      grantType: 'password',
      tenantId: TENANT_ID,
      username: USERNAME,
      password: PASSWORD
    }
  });
  const data = expectR(response, 'AUTH-LOGIN');
  assert(Boolean(data?.access_token), 'AUTH-TOKEN', 'access_token exists');
  return data.access_token;
}

async function main() {
  fs.mkdirSync(REPORT_DIR, { recursive: true });
  const token = await login();

  const stats = expectR(await request('GET', '/infoservice/portal/stats', { token }), 'INFO-STATS');
  assert(Number.isFinite(Number(stats?.toolCount)), 'INFO-STATS-SHAPE', JSON.stringify(stats));

  const resourceCategories = expectR(await request('GET', '/infoservice/portal/resources/categories', { token }), 'RESOURCE-CATEGORIES');
  assert(Array.isArray(resourceCategories), 'RESOURCE-CATEGORIES-SHAPE', `${resourceCategories?.length ?? 0} categories`);
  const resourceTitle = `phase1 smoke resource ${RUN_ID}`;
  const upload = expectR(await requestMultipart('POST', '/infoservice/resource/upload', {
    token,
    files: [{
      name: 'file',
      filename: 'phase1-smoke-resource.txt',
      contentType: 'text/plain',
      content: `Phase 1 resource smoke file generated at ${RUN_ID}.\n`
    }]
  }), 'RESOURCE-UPLOAD');
  assert(Boolean(upload?.ossId), 'RESOURCE-UPLOAD-SHAPE', `ossId=${upload?.ossId ?? ''}`);
  expectR(await request('POST', '/infoservice/resource', {
    token,
    body: {
      title: resourceTitle,
      description: 'Phase 1 smoke resource for upload, preview and download verification.',
      categoryId: resourceCategories[0].categoryId,
      ossId: upload.ossId,
      originalName: upload.originalName,
      fileSuffix: upload.fileSuffix,
      mimeType: upload.mimeType,
      fileSize: upload.fileSize,
      previewType: upload.previewType,
      status: '0'
    }
  }), 'RESOURCE-CREATE');
  const resources = expectTable(
    await request('GET', `/infoservice/portal/resources?pageNum=1&pageSize=10&keyword=${encodeURIComponent(resourceTitle)}`, { token }),
    'RESOURCE-LIST'
  );
  const resource = resources.find((item) => item.title === resourceTitle);
  assert(Boolean(resource?.resourceId), 'RESOURCE-LIST-SHAPE', `resourceId=${resource?.resourceId ?? ''}`);
  const resourceDetail = expectR(await request('GET', `/infoservice/portal/resources/${resource.resourceId}`, { token }), 'RESOURCE-DETAIL');
  assert(resourceDetail?.resourceId === resource.resourceId, 'RESOURCE-DETAIL-SHAPE', `resourceId=${resourceDetail?.resourceId ?? ''}`);
  const preview = await request('GET', `/infoservice/portal/resources/${resource.resourceId}/preview`, { token });
  assert(preview.status === 200, 'RESOURCE-PREVIEW-HTTP', `HTTP ${preview.status}`);
  assert(String(preview.headers?.['content-disposition'] || '').includes('inline'), 'RESOURCE-PREVIEW-INLINE', String(preview.headers?.['content-disposition'] || ''));
  assert(preview.raw.includes('Phase 1 resource smoke file'), 'RESOURCE-PREVIEW-CONTENT', `${preview.raw.length} bytes`);
  const download = await request('GET', `/infoservice/portal/resources/${resource.resourceId}/download`, { token });
  assert(download.status === 200, 'RESOURCE-DOWNLOAD-HTTP', `HTTP ${download.status}`);
  assert(String(download.headers?.['content-disposition'] || '').includes('attachment'), 'RESOURCE-DOWNLOAD-ATTACHMENT', String(download.headers?.['content-disposition'] || ''));
  assert(String(download.headers?.['content-disposition'] || '').includes('phase1-smoke-resource.txt'), 'RESOURCE-DOWNLOAD-FILENAME', String(download.headers?.['content-disposition'] || ''));
  assert(download.raw.includes('Phase 1 resource smoke file'), 'RESOURCE-DOWNLOAD-CONTENT', `${download.raw.length} bytes`);
  expectR(await request('DELETE', `/infoservice/resource/${resource.resourceId}`, { token }), 'RESOURCE-CLEANUP');

  const boards = expectR(await request('GET', '/infoservice/portal/forum/boards', { token }), 'FORUM-BOARDS');
  assert(Array.isArray(boards) && boards.length > 0, 'FORUM-BOARDS-SHAPE', `${boards?.length ?? 0} boards`);
  expectTable(await request('GET', '/infoservice/portal/forum/topics?pageNum=1&pageSize=10', { token }), 'FORUM-TOPICS');
  const boardId = boards[0].boardId;
  const topic = expectR(await request('POST', '/infoservice/portal/forum/topics', {
    token,
    body: {
      boardId,
      title: `phase1 smoke topic ${RUN_ID}`,
      content: `Phase 1 forum smoke topic generated at ${RUN_ID}.`
    }
  }), 'FORUM-CREATE-TOPIC');
  assert(Boolean(topic?.topicId), 'FORUM-CREATE-TOPIC-SHAPE', `topicId=${topic?.topicId ?? ''}`);
  const reply = expectR(await request('POST', `/infoservice/portal/forum/topics/${topic.topicId}/replies`, {
    token,
    body: {
      content: `Phase 1 forum smoke reply generated at ${RUN_ID}.`
    }
  }), 'FORUM-CREATE-REPLY');
  assert(reply?.topicId === topic.topicId, 'FORUM-CREATE-REPLY-SHAPE', `replyId=${reply?.replyId ?? ''}`);
  expectR(await request('POST', `/infoservice/portal/forum/topics/${topic.topicId}/like`, { token }), 'FORUM-LIKE');
  const detail = expectR(await request('GET', `/infoservice/portal/forum/topics/${topic.topicId}`, { token }), 'FORUM-DETAIL');
  assert(
    detail?.topic?.topicId === topic.topicId && Array.isArray(detail?.replies) && detail.replies.some((item) => item.replyId === reply.replyId),
    'FORUM-DETAIL-SHAPE',
    `topicId=${detail?.topic?.topicId ?? ''}, replies=${detail?.replies?.length ?? 0}`
  );
  expectR(await request('DELETE', `/infoservice/portal/forum/topics/${topic.topicId}/like`, { token }), 'FORUM-UNLIKE');
  expectR(await request('DELETE', `/infoservice/forum/topic/${topic.topicId}`, { token }), 'FORUM-CLEANUP');

  const toolCategories = expectR(await request('GET', '/appcenter/portal/categories', { token }), 'TOOL-CATEGORIES');
  assert(Array.isArray(toolCategories), 'TOOL-CATEGORIES-SHAPE', `${toolCategories?.length ?? 0} categories`);
  const tools = expectTable(await request('GET', '/appcenter/portal/apps?pageNum=1&pageSize=20', { token }), 'TOOL-LIST');
  const toolNames = tools.map((item) => item.appName).sort();
  ['Stirling PDF', 'draw.io', 'Excalidraw'].forEach((name) => {
    assert(toolNames.includes(name), `TOOL-SEED-${name}`, toolNames.join(','));
  });

  const report = {
    baseUrl: BASE_URL,
    runId: RUN_ID,
    generatedAt: new Date().toISOString(),
    results
  };
  fs.writeFileSync(REPORT_FILE, JSON.stringify(report, null, 2));
  console.log(`REPORT ${REPORT_FILE}`);
}

main().catch((error) => {
  fs.mkdirSync(REPORT_DIR, { recursive: true });
  const report = {
    baseUrl: BASE_URL,
    runId: RUN_ID,
    generatedAt: new Date().toISOString(),
    results,
    error: error.message
  };
  fs.writeFileSync(REPORT_FILE, JSON.stringify(report, null, 2));
  console.error(error.message);
  console.error(`REPORT ${REPORT_FILE}`);
  process.exit(1);
});
