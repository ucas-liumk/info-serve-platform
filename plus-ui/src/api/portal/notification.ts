import request from '@/utils/request';
import { AxiosPromise } from 'axios';

export interface PortalNotification {
  messageId: number;
  title: string;
  content: string;
  msgType: 'version' | 'resource' | 'app' | 'forum' | 'demand' | 'system' | string;
  isRead: string;
  createTime: string;
}

export function listPortalNotifications(query: any): AxiosPromise<PortalNotification[]> {
  return request({ url: '/appcenter/portal/messages', method: 'get', params: query });
}

export function getPortalUnreadCount(): AxiosPromise<number> {
  return request({ url: '/appcenter/portal/messages/unreadCount', method: 'get' });
}

export function readPortalNotification(id: number) {
  return request({ url: `/appcenter/portal/messages/${id}/read`, method: 'post' });
}

export function deletePortalNotification(id: number) {
  return request({ url: `/appcenter/portal/messages/${id}`, method: 'delete' });
}

export function clearPortalReadNotifications() {
  return request({ url: '/appcenter/portal/messages/history/clear', method: 'delete' });
}
