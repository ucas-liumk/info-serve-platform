import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { PortalStats } from '@/api/infoservice/types';

/** 门户内核：首页统计聚合 */
export function getPortalStats(): AxiosPromise<PortalStats> {
  return request({ url: '/infoservice/portal/stats', method: 'get' });
}
