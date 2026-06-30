import request from '@/utils/request';
import { AxiosPromise } from 'axios';
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

export function getResourceKkPreviewUrl(resourceId: ResourceId): AxiosPromise<string> {
  return request({ url: `/infoservice/portal/resources/${resourceId}/kk-preview-url`, method: 'get' });
}

export function uploadPortalResourceFile(data: FormData): AxiosPromise<ResourceUploadResult> {
  return request({
    url: '/infoservice/portal/resources/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  });
}

export function createPortalResource(data: ResourcePortalPayload) {
  return request({ url: '/infoservice/portal/resources', method: 'post', data });
}

export function updatePortalResource(resourceId: ResourceId, data: ResourcePortalPayload) {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'put', data });
}

export function changePortalResourceStatus(resourceId: ResourceId, status: string) {
  return request({ url: `/infoservice/portal/resources/${resourceId}/status`, method: 'put', data: { status } });
}

export function deletePortalResource(resourceId: ResourceId) {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'delete' });
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

export function resourceDownloadUrl(resourceId: ResourceId) {
  return `${import.meta.env.VITE_APP_BASE_API}/infoservice/portal/resources/${resourceId}/download`;
}
