import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  ForumBoard,
  ForumReply,
  ForumTopic,
  ForumTopicDetail,
  InfoResource,
  PortalStats,
  ResourceCategory,
  ResourceUploadResult
} from './types';

export function getPortalStats(): AxiosPromise<PortalStats> {
  return request({ url: '/infoservice/portal/stats', method: 'get' });
}

export function listResourceCategories(): AxiosPromise<ResourceCategory[]> {
  return request({ url: '/infoservice/portal/resources/categories', method: 'get' });
}

export function listResources(query: any): AxiosPromise<InfoResource[]> {
  return request({ url: '/infoservice/portal/resources', method: 'get', params: query });
}

export function getResource(resourceId: number): AxiosPromise<InfoResource> {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'get' });
}

export function uploadResourceFile(data: FormData): AxiosPromise<ResourceUploadResult> {
  return request({
    url: '/infoservice/resource/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  });
}

export function createResource(data: any) {
  return request({ url: '/infoservice/resource', method: 'post', data });
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

export function resourcePreviewUrl(resourceId: number) {
  return `${import.meta.env.VITE_APP_BASE_API}/infoservice/portal/resources/${resourceId}/preview`;
}

export function resourceDownloadUrl(resourceId: number) {
  return `${import.meta.env.VITE_APP_BASE_API}/infoservice/portal/resources/${resourceId}/download`;
}
