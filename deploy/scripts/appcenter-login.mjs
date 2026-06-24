import crypto from 'node:crypto';
import http from 'node:http';

// 服务端「请求解密私钥」对应的加密公钥（来自 application-common.yml 注释）
const reqPubB64 = 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';

// 32 字节 AES 密钥（AES-256），对应 Hutool SecureUtil.aes 默认 AES/ECB/PKCS5Padding
const aesKey = 'RuoYiCloudPlusPGMigrate012345678'; // 正好 32 字符
const body = JSON.stringify({
  clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e',
  grantType: 'password',
  tenantId: '000000',
  username: 'admin',
  password: 'admin123',
});

// 1) AES-256-ECB 加密报文体 -> Base64
const cipher = crypto.createCipheriv('aes-256-ecb', Buffer.from(aesKey, 'utf8'), null);
let encBody = cipher.update(body, 'utf8', 'base64');
encBody += cipher.final('base64');

// 2) 用服务端公钥 RSA/PKCS1 加密 Base64(AES密钥) -> Base64（放入 encrypt-key 头）
//    服务端流程: RSA解密头 -> 得到 Base64(key) 字符串 -> Base64解码 -> 真正的 AES 密钥
const aesKeyB64 = Buffer.from(aesKey, 'utf8').toString('base64');
const pubKey = crypto.createPublicKey({ key: Buffer.from(reqPubB64, 'base64'), format: 'der', type: 'spki' });
const encKey = crypto.publicEncrypt(
  { key: pubKey, padding: crypto.constants.RSA_PKCS1_PADDING },
  Buffer.from(aesKeyB64, 'utf8')
).toString('base64');

const payload = Buffer.from(encBody, 'utf8');
const req = http.request({
  host: '127.0.0.1', port: 19100, path: '/prod-api/auth/login', method: 'POST',
  headers: { 'Content-Type': 'application/json;charset=UTF-8', 'encrypt-key': encKey, 'Content-Length': payload.length },
}, (res) => {
  let data = '';
  res.on('data', (c) => (data += c));
  res.on('end', () => {
    console.log('HTTP', res.statusCode);
    console.log(data.slice(0, 800));
  });
});
req.on('error', (e) => console.error('ERR', e.message));
req.write(payload);
req.end();
