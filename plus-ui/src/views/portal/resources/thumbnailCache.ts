import type { InfoResource } from '@/api/infoservice/types';

const CACHE_PREFIX = 'info-resource-thumb:';
const MAX_CACHE_ITEMS = 80;

const cacheKey = (resource: InfoResource) => {
  const version = resource.updateTime || resource.createTime || '';
  return `${CACHE_PREFIX}${resource.resourceId}:${resource.ossId || ''}:${resource.fileSize || 0}:${version}`;
};

const listKeys = () => {
  const keys: string[] = [];
  for (let i = 0; i < window.localStorage.length; i += 1) {
    const key = window.localStorage.key(i);
    if (key?.startsWith(CACHE_PREFIX) && !key.endsWith(':ts')) {
      keys.push(key);
    }
  }
  return keys;
};

const pruneCache = () => {
  const keys = listKeys();
  if (keys.length <= MAX_CACHE_ITEMS) return;
  keys
    .sort((a, b) => {
      const at = Number(window.localStorage.getItem(`${a}:ts`) || 0);
      const bt = Number(window.localStorage.getItem(`${b}:ts`) || 0);
      return at - bt;
    })
    .slice(0, keys.length - MAX_CACHE_ITEMS)
    .forEach((key) => {
      window.localStorage.removeItem(key);
      window.localStorage.removeItem(`${key}:ts`);
    });
};

export const getCachedThumbnail = (resource: InfoResource) => {
  try {
    const key = cacheKey(resource);
    const value = window.localStorage.getItem(key);
    if (value) {
      window.localStorage.setItem(`${key}:ts`, String(Date.now()));
    }
    return value || '';
  } catch {
    return '';
  }
};

export const setCachedThumbnail = (resource: InfoResource, dataUrl: string) => {
  if (!dataUrl) return;
  try {
    const key = cacheKey(resource);
    window.localStorage.setItem(key, dataUrl);
    window.localStorage.setItem(`${key}:ts`, String(Date.now()));
    pruneCache();
  } catch {
    // localStorage may be full or disabled; thumbnail caching is an enhancement.
  }
};

/**
 * 缩略图取数并发闸 + 失败负缓存。
 * 预览服务不可用时（如 OSS 端点不可达导致 pdf-preview 长挂），无限制并发取图会占满
 * 浏览器同域连接池（HTTP/1.1 上限 6），饿死列表/筛选等关键请求——闸门保证至少留出
 * 通道给业务请求，负缓存避免同会话内对已失败资源反复重试。
 */
export const THUMBNAIL_FETCH_TIMEOUT_MS = 8000;
const MAX_CONCURRENT_FETCHES = 2;
const FAILURE_TTL_MS = 5 * 60 * 1000;

let activeFetches = 0;
const slotWaiters: Array<() => void> = [];
const failedAt = new Map<string | number, number>();

export const acquireThumbnailSlot = async (): Promise<() => void> => {
  if (activeFetches >= MAX_CONCURRENT_FETCHES) {
    await new Promise<void>((resolve) => slotWaiters.push(resolve));
  }
  activeFetches += 1;
  let released = false;
  return () => {
    if (released) return;
    released = true;
    activeFetches -= 1;
    slotWaiters.shift()?.();
  };
};

export const isThumbnailBlocked = (resource: InfoResource): boolean => {
  const at = failedAt.get(resource.resourceId);
  return at != null && Date.now() - at < FAILURE_TTL_MS;
};

export const markThumbnailFailed = (resource: InfoResource) => {
  failedAt.set(resource.resourceId, Date.now());
};
