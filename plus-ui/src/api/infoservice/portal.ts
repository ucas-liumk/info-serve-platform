import request from '@/utils/request';
import axios, { AxiosPromise, AxiosResponse } from 'axios';
import { globalHeaders } from '@/utils/request';
import {
  ForumBoard,
  ForumReply,
  ForumTopic,
  ForumTopicDetail,
  InfoResource,
  PortalStats,
  ResourcePortalPayload,
  ResourcePortalQuery,
  ResourceCategory,
  ResourceUploadResult
} from './types';

type ResourceId = number | string;

export function getPortalStats(): AxiosPromise<PortalStats> {
  return request({ url: '/infoservice/portal/stats', method: 'get' });
}

export function listResourceCategories(): AxiosPromise<ResourceCategory[]> {
  return request({ url: '/infoservice/portal/resources/categories', method: 'get' });
}

export function listResources(query: ResourcePortalQuery): AxiosPromise<InfoResource[]> {
  return request({ url: '/infoservice/portal/resources', method: 'get', params: query });
}

export function getResource(resourceId: ResourceId): AxiosPromise<InfoResource> {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'get' });
}

export function uploadPortalResourceFile(data: FormData): AxiosPromise<ResourceUploadResult> {
  return request({
    url: '/infoservice/portal/resources/upload',
    method: 'post',
    data,
    timeout: 5 * 60 * 1000,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  });
}

export function createPortalResource(data: ResourcePortalPayload) {
  return request({ url: '/infoservice/portal/resources', method: 'post', data, headers: { repeatSubmit: false } });
}

export function updatePortalResource(resourceId: ResourceId, data: ResourcePortalPayload) {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'put', data, headers: { repeatSubmit: false } });
}

export function changePortalResourceStatus(resourceId: ResourceId, status: string) {
  return request({ url: `/infoservice/portal/resources/${resourceId}/status`, method: 'put', data: { status } });
}

export function deletePortalResource(resourceId: ResourceId) {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'delete' });
}

export function favoritePortalResource(resourceId: ResourceId) {
  return request({ url: `/infoservice/portal/resources/${resourceId}/favorite`, method: 'post' });
}

export function unfavoritePortalResource(resourceId: ResourceId) {
  return request({ url: `/infoservice/portal/resources/${resourceId}/favorite`, method: 'delete' });
}

export function listForumBoards(): AxiosPromise<ForumBoard[]> {
  return request({ url: '/infoservice/portal/forum/boards', method: 'get' });
}

export function listForumTopics(query: any): AxiosPromise<ForumTopic[]> {
  return request({ url: '/infoservice/portal/forum/topics', method: 'get', params: query });
}

export function getForumTopic(topicId: number): AxiosPromise<ForumTopicDetail> {
  return request({ url: `/infoservice/portal/forum/topics/${topicId}`, method: 'get' });
}

export function createForumTopic(data: any): AxiosPromise<ForumTopic> {
  return request({ url: '/infoservice/portal/forum/topics', method: 'post', data });
}

export function replyForumTopic(topicId: number, data: any): AxiosPromise<ForumReply> {
  return request({ url: `/infoservice/portal/forum/topics/${topicId}/replies`, method: 'post', data });
}

export function likeForumTopic(topicId: number) {
  return request({ url: `/infoservice/portal/forum/topics/${topicId}/like`, method: 'post' });
}

export function unlikeForumTopic(topicId: number) {
  return request({ url: `/infoservice/portal/forum/topics/${topicId}/like`, method: 'delete' });
}

export function resourcePreviewUrl(resourceId: ResourceId) {
  return `${import.meta.env.VITE_APP_BASE_API}/infoservice/portal/resources/${resourceId}/preview`;
}

export function resourcePdfPreviewUrl(resourceId: ResourceId) {
  return `${import.meta.env.VITE_APP_BASE_API}/infoservice/portal/resources/${resourceId}/pdf-preview`;
}

export function resourceDownloadUrl(resourceId: ResourceId) {
  return `${import.meta.env.VITE_APP_BASE_API}/infoservice/portal/resources/${resourceId}/download`;
}

export function downloadResourceBlob(resourceId: ResourceId): Promise<AxiosResponse<Blob>> {
  return axios({
    method: 'get',
    url: resourceDownloadUrl(resourceId),
    responseType: 'blob',
    headers: globalHeaders()
  });
}
