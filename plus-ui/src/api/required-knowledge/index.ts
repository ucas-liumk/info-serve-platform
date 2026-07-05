import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  RkKnowledge,
  RkKnowledgeForm,
  RkKnowledgeQuery,
  RkSubject,
  RkSubjectForm,
  RkSubjectGroup,
  RkSubjectGroupForm,
  RkSubjectGroupQuery,
  RkSubjectQuery
} from './types';

export function listSubjectGroup(query?: RkSubjectGroupQuery): AxiosPromise<RkSubjectGroup[]> {
  return request({ url: '/infoservice/required-knowledge/group/list', method: 'get', params: query });
}

export function listSubjectGroupOptions(query?: RkSubjectGroupQuery): AxiosPromise<RkSubjectGroup[]> {
  return request({ url: '/infoservice/required-knowledge/group/options', method: 'get', params: query });
}

export function getSubjectGroup(groupId: string | number): AxiosPromise<RkSubjectGroup> {
  return request({ url: `/infoservice/required-knowledge/group/${groupId}`, method: 'get' });
}

export function addSubjectGroup(data: RkSubjectGroupForm) {
  return request({ url: '/infoservice/required-knowledge/group', method: 'post', data });
}

export function updateSubjectGroup(data: RkSubjectGroupForm) {
  return request({ url: '/infoservice/required-knowledge/group', method: 'put', data });
}

export function changeSubjectGroupStatus(groupId: string | number, status: string) {
  return request({ url: '/infoservice/required-knowledge/group/changeStatus', method: 'put', data: { groupId, status } });
}

export function delSubjectGroup(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/required-knowledge/group/${ids}`, method: 'delete' });
}

export function listSubject(query?: RkSubjectQuery): AxiosPromise<RkSubject[]> {
  return request({ url: '/infoservice/required-knowledge/subject/list', method: 'get', params: query });
}

export function listSubjectOptions(query?: RkSubjectQuery): AxiosPromise<RkSubject[]> {
  return request({ url: '/infoservice/required-knowledge/subject/options', method: 'get', params: query });
}

export function getSubject(subjectId: string | number): AxiosPromise<RkSubject> {
  return request({ url: `/infoservice/required-knowledge/subject/${subjectId}`, method: 'get' });
}

export function addSubject(data: RkSubjectForm) {
  return request({ url: '/infoservice/required-knowledge/subject', method: 'post', data });
}

export function updateSubject(data: RkSubjectForm) {
  return request({ url: '/infoservice/required-knowledge/subject', method: 'put', data });
}

export function changeSubjectStatus(subjectId: string | number, status: string) {
  return request({ url: '/infoservice/required-knowledge/subject/changeStatus', method: 'put', data: { subjectId, status } });
}

export function delSubject(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/required-knowledge/subject/${ids}`, method: 'delete' });
}

export function listKnowledge(query?: RkKnowledgeQuery): AxiosPromise<RkKnowledge[]> {
  return request({ url: '/infoservice/required-knowledge/knowledge/list', method: 'get', params: query });
}

export function getKnowledge(knowledgeId: string | number): AxiosPromise<RkKnowledge> {
  return request({ url: `/infoservice/required-knowledge/knowledge/${knowledgeId}`, method: 'get' });
}

export function addKnowledge(data: RkKnowledgeForm) {
  return request({ url: '/infoservice/required-knowledge/knowledge', method: 'post', data });
}

export function updateKnowledge(data: RkKnowledgeForm) {
  return request({ url: '/infoservice/required-knowledge/knowledge', method: 'put', data });
}

export function changeKnowledgeStatus(knowledgeId: string | number, status: string) {
  return request({ url: '/infoservice/required-knowledge/knowledge/changeStatus', method: 'put', data: { knowledgeId, status } });
}

export function delKnowledge(ids: string | number | (string | number)[]) {
  return request({ url: `/infoservice/required-knowledge/knowledge/${ids}`, method: 'delete' });
}
