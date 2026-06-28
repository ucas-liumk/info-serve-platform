import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  ForumBoard,
  ForumBoardForm,
  ForumBoardQuery,
  ForumTopic,
  ForumTopicForm,
  ForumTopicQuery,
  InfoResource,
  InfoResourceForm,
  InfoResourceQuery,
  ResourceCategory,
  ResourceCategoryForm,
  ResourceCategoryQuery,
  ResourceUploadResult
} from './types';

export function listResource(query: InfoResourceQuery): AxiosPromise<InfoResource[]> {
  return request({ url: '/infoservice/resource/list', method: 'get', params: query });
}

export function getResourceAdmin(resourceId: string | number): AxiosPromise<InfoResource> {
  return request({ url: `/infoservice/resource/${resourceId}`, method: 'get' });
}

export function uploadAdminResourceFile(data: FormData): AxiosPromise<ResourceUploadResult> {
  return request({
    url: '/infoservice/resource/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  });
}

export function addResource(data: InfoResourceForm) {
  return request({ url: '/infoservice/resource', method: 'post', data });
}

export function updateResource(data: InfoResourceForm) {
  return request({ url: '/infoservice/resource', method: 'put', data });
}

export function changeResourceStatus(resourceId: string | number, status: string) {
  return request({ url: '/infoservice/resource/changeStatus', method: 'put', data: { resourceId, status } });
}

export function delResource(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/resource/${ids}`, method: 'delete' });
}

export function listResourceCategory(query?: ResourceCategoryQuery): AxiosPromise<ResourceCategory[]> {
  return request({ url: '/infoservice/resource/category/list', method: 'get', params: query });
}

export function listResourceCategoryOptions(query?: ResourceCategoryQuery): AxiosPromise<ResourceCategory[]> {
  return request({ url: '/infoservice/resource/category/options', method: 'get', params: query });
}

export function getResourceCategory(categoryId: string | number): AxiosPromise<ResourceCategory> {
  return request({ url: `/infoservice/resource/category/${categoryId}`, method: 'get' });
}

export function addResourceCategory(data: ResourceCategoryForm) {
  return request({ url: '/infoservice/resource/category', method: 'post', data });
}

export function updateResourceCategory(data: ResourceCategoryForm) {
  return request({ url: '/infoservice/resource/category', method: 'put', data });
}

export function changeResourceCategoryStatus(categoryId: string | number, status: string) {
  return request({ url: '/infoservice/resource/category/changeStatus', method: 'put', data: { categoryId, status } });
}

export function delResourceCategory(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/resource/category/${ids}`, method: 'delete' });
}

export function listForumBoard(query?: ForumBoardQuery): AxiosPromise<ForumBoard[]> {
  return request({ url: '/infoservice/forum/board/list', method: 'get', params: query });
}

export function listForumBoardOptions(query?: ForumBoardQuery): AxiosPromise<ForumBoard[]> {
  return request({ url: '/infoservice/forum/board/options', method: 'get', params: query });
}

export function getForumBoard(boardId: string | number): AxiosPromise<ForumBoard> {
  return request({ url: `/infoservice/forum/board/${boardId}`, method: 'get' });
}

export function addForumBoard(data: ForumBoardForm) {
  return request({ url: '/infoservice/forum/board', method: 'post', data });
}

export function updateForumBoard(data: ForumBoardForm) {
  return request({ url: '/infoservice/forum/board', method: 'put', data });
}

export function changeForumBoardStatus(boardId: string | number, status: string) {
  return request({ url: '/infoservice/forum/board/changeStatus', method: 'put', data: { boardId, status } });
}

export function delForumBoard(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/forum/board/${ids}`, method: 'delete' });
}

export function listForumTopic(query: ForumTopicQuery): AxiosPromise<ForumTopic[]> {
  return request({ url: '/infoservice/forum/topic/list', method: 'get', params: query });
}

export function getForumTopicAdmin(topicId: string | number): AxiosPromise<ForumTopic> {
  return request({ url: `/infoservice/forum/topic/${topicId}`, method: 'get' });
}

export function addForumTopic(data: ForumTopicForm) {
  return request({ url: '/infoservice/forum/topic', method: 'post', data });
}

export function updateForumTopic(data: ForumTopicForm) {
  return request({ url: '/infoservice/forum/topic', method: 'put', data });
}

export function changeForumTopicStatus(topicId: string | number, status: string) {
  return request({ url: '/infoservice/forum/topic/changeStatus', method: 'put', data: { topicId, status } });
}

export function delForumTopic(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/forum/topic/${ids}`, method: 'delete' });
}
