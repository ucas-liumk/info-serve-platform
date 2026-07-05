-- ============================================================
-- 应知应会 Required Knowledge — PostgreSQL DDL + 种子数据 + 菜单权限
-- 目标库: ry-cloud
-- ============================================================

DROP TABLE IF EXISTS rk_knowledge;
DROP TABLE IF EXISTS rk_subject;
DROP TABLE IF EXISTS rk_subject_group;

CREATE TABLE rk_subject_group (
    group_id     int8          NOT NULL,
    group_name   varchar(80)   NOT NULL,
    group_code   varchar(80)   NOT NULL,
    description  varchar(255)  DEFAULT NULL,
    order_num    int4          DEFAULT 0,
    status       char(1)       DEFAULT '0',
    tenant_id    varchar(20)   DEFAULT '000000',
    del_flag     char(1)       DEFAULT '0',
    create_dept  int8          DEFAULT NULL,
    create_by    int8          DEFAULT NULL,
    create_time  timestamp     DEFAULT NULL,
    update_by    int8          DEFAULT NULL,
    update_time  timestamp     DEFAULT NULL,
    remark       varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_rk_subject_group PRIMARY KEY (group_id)
);
CREATE UNIQUE INDEX uk_rk_subject_group_code ON rk_subject_group (tenant_id, group_code) WHERE del_flag = '0';
COMMENT ON TABLE rk_subject_group IS '应知应会学习栏目';

CREATE TABLE rk_subject (
    subject_id      int8          NOT NULL,
    group_id        int8          NOT NULL,
    subject_name    varchar(80)   NOT NULL,
    subject_code    varchar(80)   NOT NULL,
    description     varchar(255)  DEFAULT NULL,
    icon            varchar(80)   DEFAULT NULL,
    knowledge_count int4          DEFAULT 0,
    question_count  int4          DEFAULT 0,
    exam_count      int4          DEFAULT 0,
    order_num       int4          DEFAULT 0,
    status          char(1)       DEFAULT '0',
    tenant_id       varchar(20)   DEFAULT '000000',
    del_flag        char(1)       DEFAULT '0',
    create_dept     int8          DEFAULT NULL,
    create_by       int8          DEFAULT NULL,
    create_time     timestamp     DEFAULT NULL,
    update_by       int8          DEFAULT NULL,
    update_time     timestamp     DEFAULT NULL,
    remark          varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_rk_subject PRIMARY KEY (subject_id)
);
CREATE UNIQUE INDEX uk_rk_subject_code ON rk_subject (tenant_id, subject_code) WHERE del_flag = '0';
CREATE INDEX idx_rk_subject_group ON rk_subject (tenant_id, group_id, status);
COMMENT ON TABLE rk_subject IS '应知应会学习科目';

CREATE TABLE rk_knowledge (
    knowledge_id int8          NOT NULL,
    subject_id   int8          NOT NULL,
    title        varchar(160)  NOT NULL,
    summary      varchar(500)  DEFAULT NULL,
    content      text          DEFAULT NULL,
    order_num    int4          DEFAULT 0,
    status       char(1)       DEFAULT '0',
    tenant_id    varchar(20)   DEFAULT '000000',
    del_flag     char(1)       DEFAULT '0',
    create_dept  int8          DEFAULT NULL,
    create_by    int8          DEFAULT NULL,
    create_time  timestamp     DEFAULT NULL,
    update_by    int8          DEFAULT NULL,
    update_time  timestamp     DEFAULT NULL,
    remark       varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_rk_knowledge PRIMARY KEY (knowledge_id)
);
CREATE INDEX idx_rk_knowledge_subject ON rk_knowledge (tenant_id, subject_id, status, order_num);
COMMENT ON TABLE rk_knowledge IS '应知应会知识点';

INSERT INTO rk_subject_group
    (group_id, group_name, group_code, description, order_num, status, tenant_id, del_flag, create_time)
VALUES
    (2073005100000000001, '软考类', 'soft-exam', '项目管理、系统架构等职业资格方向', 10, '0', '000000', '0', now()),
    (2073005100000000002, '考研类', 'postgraduate', '数学、408 等研究生入学考试方向', 20, '0', '000000', '0', now());

INSERT INTO rk_subject
    (subject_id, group_id, subject_name, subject_code, description, icon,
     knowledge_count, question_count, exam_count, order_num, status, tenant_id, del_flag, create_time)
VALUES
    (2073005100000000101, 2073005100000000001, '软考高项', 'advanced-project', '信息系统项目管理师核心知识学习与模拟考试', '高', 2, 86, 2, 10, '0', '000000', '0', now()),
    (2073005100000000102, 2073005100000000001, '系统架构设计师', 'architect', '架构风格、质量属性、系统设计题基础训练', '架', 1, 72, 1, 20, '0', '000000', '0', now()),
    (2073005100000000103, 2073005100000000002, '考研数学', 'math', '高等数学、线性代数、概率论基础题型', '数', 1, 96, 2, 10, '0', '000000', '0', now()),
    (2073005100000000104, 2073005100000000002, '考研 408', '408', '数据结构、计组、操作系统、网络基础知识', '408', 1, 110, 2, 20, '0', '000000', '0', now());

INSERT INTO rk_knowledge
    (knowledge_id, subject_id, title, summary, content, order_num, status, tenant_id, del_flag, create_time)
VALUES
    (2073005100000000201, 2073005100000000101, '范围管理基础', '明确项目边界，控制需求变更，形成可验收成果。', '范围管理的目标是保证项目只做必要工作，并让项目团队、客户和干系人对交付边界形成一致理解。首期学习重点放在范围说明书、WBS、范围基准和范围确认四个概念。做题时先判断题干是在讨论定义范围还是控制范围，再看是否存在基准、变更请求、验收记录等关键词。', 10, '0', '000000', '0', now()),
    (2073005100000000202, 2073005100000000101, '进度管理基础', '识别活动、排序、估算资源与持续时间，形成进度计划。', '进度管理关注活动之间的逻辑关系与关键路径。首期只保留基础概念，后续再扩展网络图计算。学习时重点区分活动定义、活动排序、资源估算和持续时间估算。', 20, '0', '000000', '0', now()),
    (2073005100000000203, 2073005100000000102, '质量属性', '性能、可用性、安全性、可维护性等架构决策目标。', '质量属性用于评价架构方案是否满足业务目标。首期重点理解可用性、性能和安全性的常见设计策略，并把质量属性和具体架构策略对应起来。', 10, '0', '000000', '0', now()),
    (2073005100000000204, 2073005100000000103, '极限与连续', '掌握极限存在、等价无穷小、函数连续等基础概念。', '极限题通常先看函数结构，再判断是否可直接代入、等价替换或洛必达。学习时把能否代入、是否 0/0 型、是否需要拆项作为固定检查顺序。', 10, '0', '000000', '0', now()),
    (2073005100000000205, 2073005100000000104, '进程同步与互斥', '理解临界区、信号量、PV 操作和常见同步模型。', '互斥解决的是同一时刻只能有一个进程访问临界资源的问题；同步解决的是多个进程之间的执行顺序约束。PV 操作题的关键是先找共享资源，再判断信号量初值，最后按先 P 后进入、退出后 V 的顺序检查。', 10, '0', '000000', '0', now());

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
    (4004, '栏目科目', 4000, 1, 'subjects', 'admin/required-knowledge/subjects/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:subject:list', 'tree', 103, 1, now(), '应知应会栏目与科目配置'),
    (4005, '知识点管理', 4000, 2, 'knowledge', 'admin/required-knowledge/knowledge/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:knowledge:list', 'education', 103, 1, now(), '应知应会知识点配置'),
    (4011, '栏目查询', 4004, 1, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:group:query', '#', 103, 1, now(), ''),
    (4012, '栏目新增', 4004, 2, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:group:add', '#', 103, 1, now(), ''),
    (4013, '栏目修改', 4004, 3, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:group:edit', '#', 103, 1, now(), ''),
    (4014, '栏目删除', 4004, 4, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:group:remove', '#', 103, 1, now(), ''),
    (4015, '栏目列表', 4004, 5, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:group:list', '#', 103, 1, now(), ''),
    (4021, '科目查询', 4004, 6, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:subject:query', '#', 103, 1, now(), ''),
    (4022, '科目新增', 4004, 7, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:subject:add', '#', 103, 1, now(), ''),
    (4023, '科目修改', 4004, 8, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:subject:edit', '#', 103, 1, now(), ''),
    (4024, '科目删除', 4004, 9, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:subject:remove', '#', 103, 1, now(), ''),
    (4031, '知识点查询', 4005, 1, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:knowledge:query', '#', 103, 1, now(), ''),
    (4032, '知识点新增', 4005, 2, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:knowledge:add', '#', 103, 1, now(), ''),
    (4033, '知识点修改', 4005, 3, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:knowledge:edit', '#', 103, 1, now(), ''),
    (4034, '知识点删除', 4005, 4, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:knowledge:remove', '#', 103, 1, now(), ''),
    (4035, '知识点列表', 4005, 5, '', '', '', '1', '0', 'F', '0', '0', 'requiredKnowledge:knowledge:list', '#', 103, 1, now(), '')
ON CONFLICT (menu_id) DO NOTHING;
