import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ForumBoard, ForumReply, ForumTopic, ForumTopicDetail } from '@/api/infoservice/types';

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
