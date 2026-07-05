import request from '@/utils/request';
import { AxiosPromise } from 'axios';

/** 门户模块注册表条目（portal_module） */
export interface PortalModuleItem {
  moduleId?: number;
  moduleCode: string;
  moduleName: string;
  description?: string;
  image?: string;
  entryPath?: string;
  perms?: string;
  /** 0启用 / 1敬请期待 / 2隐藏 */
  status: string;
  sortOrder?: number;
}

/** 门户端：当前用户可见模块 */
export function listPortalModules(): AxiosPromise<PortalModuleItem[]> {
  return request({ url: '/infoservice/portal/modules', method: 'get' });
}

/** 门户端：保存当前用户的首页模块排序 */
export function updatePortalModuleOrder(moduleCodes: string[]) {
  return request({ url: '/infoservice/portal/modules/order', method: 'put', data: { moduleCodes } });
}

/** 管理端：全部模块 */
export function listModules(): AxiosPromise<PortalModuleItem[]> {
  return request({ url: '/infoservice/module/list', method: 'get' });
}

export function addModule(data: PortalModuleItem) {
  return request({ url: '/infoservice/module', method: 'post', data });
}

export function updateModule(data: PortalModuleItem) {
  return request({ url: '/infoservice/module', method: 'put', data });
}

export function deleteModule(moduleId: number) {
  return request({ url: `/infoservice/module/${moduleId}`, method: 'delete' });
}
