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
