import request from '@/utils/request';
import axios, { AxiosPromise, AxiosResponse } from 'axios';
import { globalHeaders } from '@/utils/request';
import {
  CategoryTreeNode,
  InfoResource,
  ResourceCategory,
  ResourceCategoryTreeQuery,
  ResourceNote,
  ResourceNotePayload,
  ResourcePortalPayload,
  ResourcePortalQuery,
  ResourceUploadResult,
  ResourceViewRecord
} from '@/api/infoservice/types';

type ResourceId = number | string;

export function listResourceCategories(): AxiosPromise<ResourceCategory[]> {
  return request({ url: '/infoservice/portal/resources/categories', method: 'get' });
}

/** 栏目→分类两级树（含分面计数）：入参为当前关键词/工具条筛选，不含 categoryCode */
export function getResourceCategoryTree(params?: ResourceCategoryTreeQuery): AxiosPromise<CategoryTreeNode[]> {
  return request({ url: '/infoservice/portal/resources/category-tree', method: 'get', params });
}

export function listResources(query: ResourcePortalQuery): AxiosPromise<InfoResource[]> {
  return request({ url: '/infoservice/portal/resources', method: 'get', params: query });
}

export function getResource(resourceId: ResourceId): AxiosPromise<InfoResource> {
  return request({ url: `/infoservice/portal/resources/${resourceId}`, method: 'get' });
}

export function uploadPortalResourceFile(data: FormData, onUploadProgress?: (percent: number) => void): AxiosPromise<ResourceUploadResult> {
  return request({
    url: '/infoservice/portal/resources/upload',
    method: 'post',
    data,
    timeout: 5 * 60 * 1000,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
    onUploadProgress: (event: { loaded: number; total?: number }) => {
      if (onUploadProgress && event.total) {
        onUploadProgress(Math.round((event.loaded / event.total) * 100));
      }
    }
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

export function listMyResourceNotes(resourceId: ResourceId, query?: PageQuery): AxiosPromise<ResourceNote[]> {
  return request({ url: `/infoservice/portal/resources/${resourceId}/notes/my`, method: 'get', params: query });
}

export function listPublicResourceNotes(resourceId: ResourceId, query?: PageQuery): AxiosPromise<ResourceNote[]> {
  return request({ url: `/infoservice/portal/resources/${resourceId}/notes/public`, method: 'get', params: query });
}

export function createResourceNote(resourceId: ResourceId, data: ResourceNotePayload): AxiosPromise<ResourceNote> {
  return request({ url: `/infoservice/portal/resources/${resourceId}/notes`, method: 'post', data, headers: { repeatSubmit: false } });
}

export function updateResourceNote(resourceId: ResourceId, noteId: ResourceId, data: ResourceNotePayload): AxiosPromise<ResourceNote> {
  return request({ url: `/infoservice/portal/resources/${resourceId}/notes/${noteId}`, method: 'put', data, headers: { repeatSubmit: false } });
}

export function deleteResourceNote(resourceId: ResourceId, noteId: ResourceId) {
  return request({ url: `/infoservice/portal/resources/${resourceId}/notes/${noteId}`, method: 'delete' });
}

export function listResourceViewRecords(resourceId: ResourceId, query?: PageQuery): AxiosPromise<ResourceViewRecord[]> {
  return request({ url: `/infoservice/portal/resources/${resourceId}/view-records`, method: 'get', params: query });
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
