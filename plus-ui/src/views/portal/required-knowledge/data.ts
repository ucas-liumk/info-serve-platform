export type SubjectGroupCode = string;
export type QuestionType = 'single' | 'multiple' | 'judge' | 'subjective';
export type SubjectStatus = 'enabled' | 'empty';

export interface StudySubject {
  code: string;
  name: string;
  groupCode: SubjectGroupCode;
  groupName: string;
  description: string;
  knowledgeCount: number;
  questionCount: number;
  examCount: number;
  status: SubjectStatus;
  chapters: StudyChapter[];
  exams: SubjectExam[];
}

export interface StudyChapter {
  id: string;
  title: string;
  summary: string;
  content: string[];
  relatedQuestions: RelatedQuestion[];
}

export interface RelatedQuestion {
  id: string;
  title: string;
  type: QuestionType;
  answer: string;
}

export interface SubjectExam {
  id: string;
  name: string;
  duration: number;
  questionCount: number;
  totalScore: number;
}

export const questionTypeLabels: Record<QuestionType, string> = {
  single: '单选',
  multiple: '多选',
  judge: '判断',
  subjective: '主观'
};

export const fallbackSubjectGroups: Array<{ code: SubjectGroupCode; name: string; description: string }> = [
  { code: 'soft-exam', name: '软考类', description: '项目管理、系统架构等职业资格方向' },
  { code: 'postgraduate', name: '考研类', description: '数学、408 等研究生入学考试方向' }
];

export const fallbackSubjects: StudySubject[] = [
  {
    code: 'advanced-project',
    name: '软考高项',
    groupCode: 'soft-exam',
    groupName: '软考类',
    description: '信息系统项目管理师核心知识学习与模拟考试',
    knowledgeCount: 18,
    questionCount: 86,
    examCount: 2,
    status: 'enabled',
    chapters: [
      {
        id: 'scope',
        title: '范围管理基础',
        summary: '明确项目边界，控制需求变更，形成可验收成果。',
        content: [
          '范围管理的目标是保证项目只做必要工作，并让项目团队、客户和干系人对交付边界形成一致理解。',
          '首期学习重点放在范围说明书、WBS、范围基准和范围确认四个概念。考试中常通过情境题考查“需求变更是否进入正式控制流程”。',
          '做题时先判断题干是在讨论“定义范围”还是“控制范围”，再看是否存在基准、变更请求、验收记录等关键词。'
        ],
        relatedQuestions: [
          { id: 'q1', title: '范围基准通常由哪些内容组成？', type: 'multiple', answer: '范围说明书、WBS、WBS 词典' },
          { id: 'q2', title: '客户提出新增需求时，项目经理首先应做什么？', type: 'single', answer: '进入变更控制流程' },
          { id: 'q3', title: '范围确认的核心是获得可交付成果验收。', type: 'judge', answer: '正确' }
        ]
      },
      {
        id: 'schedule',
        title: '进度管理基础',
        summary: '识别活动、排序、估算资源与持续时间，形成进度计划。',
        content: ['进度管理关注活动之间的逻辑关系与关键路径。首期只保留基础概念，后续再扩展网络图计算。'],
        relatedQuestions: [{ id: 'q4', title: '关键路径上的活动总时差通常是多少？', type: 'single', answer: '0' }]
      }
    ],
    exams: [{ id: 'mock-a', name: '高项基础模拟卷 A', duration: 90, questionCount: 45, totalScore: 75 }]
  },
  {
    code: 'architect',
    name: '系统架构设计师',
    groupCode: 'soft-exam',
    groupName: '软考类',
    description: '架构风格、质量属性、系统设计题基础训练',
    knowledgeCount: 16,
    questionCount: 72,
    examCount: 1,
    status: 'enabled',
    chapters: [
      {
        id: 'quality',
        title: '质量属性',
        summary: '性能、可用性、安全性、可维护性等架构决策目标。',
        content: ['质量属性用于评价架构方案是否满足业务目标。首期重点理解可用性、性能和安全性的常见设计策略。'],
        relatedQuestions: [{ id: 'q5', title: '熔断、限流主要服务于哪类质量属性？', type: 'single', answer: '可用性' }]
      }
    ],
    exams: [{ id: 'mock-a', name: '架构基础模拟卷 A', duration: 90, questionCount: 40, totalScore: 75 }]
  },
  {
    code: 'math',
    name: '考研数学',
    groupCode: 'postgraduate',
    groupName: '考研类',
    description: '高等数学、线性代数、概率论基础题型',
    knowledgeCount: 22,
    questionCount: 96,
    examCount: 2,
    status: 'enabled',
    chapters: [
      {
        id: 'limit',
        title: '极限与连续',
        summary: '掌握极限存在、等价无穷小、函数连续等基础概念。',
        content: [
          '极限题通常先看函数结构，再判断是否可直接代入、等价替换或洛必达。首期只覆盖基础判断与常见等价无穷小。',
          '学习时把“能否代入”“是否 0/0 型”“是否需要拆项”作为固定检查顺序。'
        ],
        relatedQuestions: [{ id: 'q6', title: '当 x -> 0 时，sin x 与 x 是否等价？', type: 'judge', answer: '正确' }]
      }
    ],
    exams: [{ id: 'mock-a', name: '数学基础模拟卷 A', duration: 120, questionCount: 23, totalScore: 150 }]
  },
  {
    code: '408',
    name: '考研 408',
    groupCode: 'postgraduate',
    groupName: '考研类',
    description: '数据结构、计组、操作系统、网络基础知识',
    knowledgeCount: 24,
    questionCount: 110,
    examCount: 2,
    status: 'enabled',
    chapters: [
      {
        id: 'process',
        title: '进程同步与互斥',
        summary: '理解临界区、信号量、PV 操作和常见同步模型。',
        content: [
          '互斥解决的是同一时刻只能有一个进程访问临界资源的问题；同步解决的是多个进程之间的执行顺序约束。',
          'PV 操作题的关键是先找共享资源，再判断信号量初值，最后按“先 P 后进入、退出后 V”的顺序检查。'
        ],
        relatedQuestions: [
          { id: 'q7', title: '互斥信号量 mutex 的初值通常是多少？', type: 'single', answer: '1' },
          { id: 'q8', title: 'P 操作可能导致进程阻塞。', type: 'judge', answer: '正确' }
        ]
      }
    ],
    exams: [{ id: 'mock-a', name: '408 基础模拟卷 A', duration: 120, questionCount: 40, totalScore: 150 }]
  }
];

export const findFallbackSubject = (subjectCode: string) => fallbackSubjects.find((subject) => subject.code === subjectCode);
