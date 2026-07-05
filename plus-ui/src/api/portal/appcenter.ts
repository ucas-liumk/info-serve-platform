import request from '@/utils/request';
import axios, { AxiosPromise, AxiosResponse } from 'axios';
import { globalHeaders } from '@/utils/request';
import type { PortalApp, PortalCategory, PortalDemandForm, PortalDemandItem } from '@/api/appcenter/types';

export function listCategories(): AxiosPromise<PortalCategory[]> {
  return request({ url: '/appcenter/portal/categories', method: 'get' });
}

export function listApps(query: any): AxiosPromise<PortalApp[]> {
  return request({ url: '/appcenter/portal/apps', method: 'get', params: query });
}

export function useApp(id: number): AxiosPromise<string> {
  return request({ url: `/appcenter/portal/apps/${id}/use`, method: 'post' });
}

export function packageDownloadUrl(id: number) {
  return `${import.meta.env.VITE_APP_BASE_API}/appcenter/portal/apps/${id}/package/download`;
}

export function downloadPackageBlob(id: number): Promise<AxiosResponse<Blob>> {
  return axios({
    method: 'get',
    url: packageDownloadUrl(id),
    responseType: 'blob',
    headers: globalHeaders()
  });
}

export function favorite(id: number) {
  return request({ url: `/appcenter/portal/apps/${id}/favorite`, method: 'post' });
}

export function unfavorite(id: number) {
  return request({ url: `/appcenter/portal/apps/${id}/favorite`, method: 'delete' });
}

export function listFavorites(query: any): AxiosPromise<PortalApp[]> {
  return request({ url: '/appcenter/portal/favorites', method: 'get', params: query });
}

export function submitDemand(data: PortalDemandForm) {
  return request({ url: '/appcenter/portal/demands', method: 'post', data });
}

export function listMyDemands(query: any): AxiosPromise<PortalDemandItem[]> {
  return request({ url: '/appcenter/portal/demands/my', method: 'get', params: query });
}

export function deleteMyDemand(id: number) {
  return request({ url: `/appcenter/portal/demands/${id}`, method: 'delete' });
}
