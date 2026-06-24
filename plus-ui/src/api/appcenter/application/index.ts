import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AppApplicationVo, AppApplicationForm, AppApplicationQuery } from './types';

// 查询应用列表
export function listApplication(query: AppApplicationQuery): AxiosPromise<AppApplicationVo[]> {
  return request({
    url: '/appcenter/application/list',
    method: 'get',
    params: query
  });
}

// 查询应用详细
export function getApplication(appId: string | number): AxiosPromise<AppApplicationVo> {
  return request({
    url: '/appcenter/application/' + appId,
    method: 'get'
  });
}

// 新增应用
export function addApplication(data: AppApplicationForm) {
  return request({
    url: '/appcenter/application',
    method: 'post',
    data: data
  });
}

// 修改应用
export function updateApplication(data: AppApplicationForm) {
  return request({
    url: '/appcenter/application',
    method: 'put',
    data: data
  });
}

// 修改应用状态
export function changeApplicationStatus(appId: string | number, status: string) {
  return request({
    url: '/appcenter/application/changeStatus',
    method: 'put',
    data: { appId, status }
  });
}

// 删除应用
export function delApplication(ids: string | number | (string | number)[]) {
  return request({
    url: '/appcenter/application/' + ids,
    method: 'delete'
  });
}
