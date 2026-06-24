import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { PortalApp, PortalCategory, PortalMessage } from './types';

export function listCategories(): AxiosPromise<PortalCategory[]> {
  return request({ url: '/appcenter/portal/categories', method: 'get' });
}

export function listApps(query: any): AxiosPromise<PortalApp[]> {
  return request({ url: '/appcenter/portal/apps', method: 'get', params: query });
}

export function useApp(id: number): AxiosPromise<string> {
  return request({ url: `/appcenter/portal/apps/${id}/use`, method: 'post' });
}

export function favorite(id: number) {
  return request({ url: `/appcenter/portal/apps/${id}/favorite`, method: 'post' });
}

export function unfavorite(id: number) {
  return request({ url: `/appcenter/portal/apps/${id}/favorite`, method: 'delete' });
}

export function recommend(id: number) {
  return request({ url: `/appcenter/portal/apps/${id}/recommend`, method: 'post' });
}

export function unrecommend(id: number) {
  return request({ url: `/appcenter/portal/apps/${id}/recommend`, method: 'delete' });
}

export function listFavorites(query: any): AxiosPromise<PortalApp[]> {
  return request({ url: '/appcenter/portal/favorites', method: 'get', params: query });
}

export function listMessages(query: any): AxiosPromise<PortalMessage[]> {
  return request({ url: '/appcenter/portal/messages', method: 'get', params: query });
}

export function unreadCount(): AxiosPromise<number> {
  return request({ url: '/appcenter/portal/messages/unreadCount', method: 'get' });
}

export function readMessage(id: number) {
  return request({ url: `/appcenter/portal/messages/${id}/read`, method: 'post' });
}
