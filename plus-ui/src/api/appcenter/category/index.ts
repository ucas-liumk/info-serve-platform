import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AppCategoryVo, AppCategoryForm, AppCategoryQuery } from './types';

// 查询分类列表
export function listCategory(query?: AppCategoryQuery): AxiosPromise<AppCategoryVo[]> {
  return request({
    url: '/appcenter/category/list',
    method: 'get',
    params: query
  });
}

// 查询分类详细
export function getCategory(categoryId: string | number): AxiosPromise<AppCategoryVo> {
  return request({
    url: '/appcenter/category/' + categoryId,
    method: 'get'
  });
}

// 新增分类
export function addCategory(data: AppCategoryForm) {
  return request({
    url: '/appcenter/category',
    method: 'post',
    data: data
  });
}

// 修改分类
export function updateCategory(data: AppCategoryForm) {
  return request({
    url: '/appcenter/category',
    method: 'put',
    data: data
  });
}

// 删除分类
export function delCategory(ids: string | number | (string | number)[]) {
  return request({
    url: '/appcenter/category/' + ids,
    method: 'delete'
  });
}
