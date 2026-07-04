import request from '@/utils/request';
import axios, { AxiosPromise, AxiosResponse } from 'axios';
import { globalHeaders } from '@/utils/request';
import { InfoResource, ResourcePortalPayload, ResourcePortalQuery, ResourceCategory, ResourceUploadResult } from '@/api/infoservice/types';

type ResourceId = number | string;

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
