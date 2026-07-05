import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { RkKnowledge, RkSubjectGroup } from '@/api/required-knowledge/types';

export function listRequiredKnowledgeCatalog(): AxiosPromise<RkSubjectGroup[]> {
  return request({ url: '/infoservice/portal/required-knowledge/catalog', method: 'get' });
}

export function listRequiredKnowledgeBySubject(subjectCode: string): AxiosPromise<RkKnowledge[]> {
  return request({ url: `/infoservice/portal/required-knowledge/subject/${subjectCode}/knowledge`, method: 'get' });
}
