import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AppDemandForm, AppDemandQuery, AppDemandVo } from './types';

// 查询需求反馈列表
export function listDemand(query: AppDemandQuery): AxiosPromise<AppDemandVo[]> {
  return request({
    url: '/appcenter/demand/list',
    method: 'get',
    params: query
  });
}

// 查询需求反馈详细
export function getDemand(demandId: string | number): AxiosPromise<AppDemandVo> {
  return request({
    url: '/appcenter/demand/' + demandId,
    method: 'get'
  });
}

// 处理需求反馈
export function handleDemand(data: AppDemandForm) {
  return request({
    url: '/appcenter/demand/handle',
    method: 'put',
    data
  });
}

// 删除需求反馈
export function delDemand(ids: string | number | (string | number)[]) {
  return request({
    url: '/appcenter/demand/' + ids,
    method: 'delete'
  });
}
