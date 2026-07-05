export interface RequiredKnowledgeSubject {
  code: string;
  name: string;
}

export const subjects: RequiredKnowledgeSubject[] = [
  { code: 'advanced-project', name: '软考高项' },
  { code: 'architect', name: '系统架构设计师' },
  { code: 'math', name: '考研数学' },
  { code: '408', name: '考研 408' }
];
