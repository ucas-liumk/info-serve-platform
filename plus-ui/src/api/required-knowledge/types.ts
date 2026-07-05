export interface RkSubjectGroup {
  groupId: number | string;
  groupName: string;
  groupCode: string;
  description?: string;
  orderNum?: number;
  status?: string;
  remark?: string;
  subjectCount?: number;
  subjects?: RkSubject[];
  createTime?: string;
}

export interface RkSubject {
  subjectId: number | string;
  groupId: number | string;
  groupName?: string;
  groupCode?: string;
  subjectName: string;
  subjectCode: string;
  description?: string;
  icon?: string;
  knowledgeCount?: number;
  questionCount?: number;
  examCount?: number;
  orderNum?: number;
  status?: string;
  remark?: string;
  createTime?: string;
}

export interface RkKnowledge {
  knowledgeId: number | string;
  subjectId: number | string;
  subjectName?: string;
  subjectCode?: string;
  title: string;
  summary?: string;
  content?: string;
  orderNum?: number;
  status?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface RkSubjectGroupForm {
  groupId?: number | string;
  groupName: string;
  groupCode: string;
  description?: string;
  orderNum: number;
  status: string;
  remark?: string;
}

export interface RkSubjectForm {
  subjectId?: number | string;
  groupId: number | string | undefined;
  subjectName: string;
  subjectCode: string;
  description?: string;
  icon?: string;
  knowledgeCount: number;
  questionCount: number;
  examCount: number;
  orderNum: number;
  status: string;
  remark?: string;
}

export interface RkKnowledgeForm {
  knowledgeId?: number | string;
  subjectId: number | string | undefined;
  title: string;
  summary?: string;
  content: string;
  orderNum: number;
  status: string;
  remark?: string;
}

export interface RkSubjectGroupQuery extends PageQuery {
  keyword?: string;
  status?: string;
}

export interface RkSubjectQuery extends PageQuery {
  keyword?: string;
  groupId?: number | string;
  status?: string;
}

export interface RkKnowledgeQuery extends PageQuery {
  keyword?: string;
  subjectId?: number | string;
  subjectCode?: string;
  status?: string;
}
