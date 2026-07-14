-- ============================================================
-- 应用中心 App Center — PostgreSQL DDL + 种子数据 + 菜单权限
-- 目标库: ry-cloud
-- 幂等: 所有 DROP / CREATE / INSERT 均可安全重复执行
-- ============================================================

-- ----------------------------------------------------------------
-- 关联表必须先删(外键顺序)
-- ----------------------------------------------------------------
DROP TABLE IF EXISTS app_message;
DROP TABLE IF EXISTS app_demand;
DROP TABLE IF EXISTS app_recommend;
DROP TABLE IF EXISTS app_favorite;
DROP TABLE IF EXISTS app_access_scope;
DROP TABLE IF EXISTS app_application;
DROP TABLE IF EXISTS app_category;

-- ----------------------------------------------------------------
-- 应用分类
-- ----------------------------------------------------------------
CREATE TABLE app_category (
    category_id   int8         NOT NULL,
    category_name varchar(50)  NOT NULL,
    category_code varchar(50)  NOT NULL,
    icon          varchar(100) DEFAULT NULL,
    order_num     int4         DEFAULT 0,
    status        char(1)      DEFAULT '0',
    del_flag      char(1)      DEFAULT '0',
    create_dept   int8         DEFAULT NULL,
    create_by     int8         DEFAULT NULL,
    create_time   timestamp    DEFAULT NULL,
    update_by     int8         DEFAULT NULL,
    update_time   timestamp    DEFAULT NULL,
    remark        varchar(500) DEFAULT NULL,
    CONSTRAINT pk_app_category PRIMARY KEY (category_id)
);
CREATE UNIQUE INDEX uk_app_category_code ON app_category (category_code) WHERE del_flag = '0';
COMMENT ON TABLE app_category IS '应用分类';

-- ----------------------------------------------------------------
-- 应用
-- ----------------------------------------------------------------
CREATE TABLE app_application (
    app_id          int8         NOT NULL,
    app_name        varchar(100) NOT NULL,
    app_code        varchar(100) NOT NULL,
    version         varchar(50)  DEFAULT NULL,
    category_id     int8         NOT NULL,
    icon            varchar(255) DEFAULT NULL,
    accent          varchar(20)  DEFAULT NULL,
    description     varchar(255) DEFAULT NULL,
    tags            varchar(255) DEFAULT NULL,
    access_url      varchar(500) NOT NULL,
    app_type        varchar(20)  DEFAULT 'online' NOT NULL,
    package_oss_id  int8         DEFAULT NULL,
    package_name    varchar(255) DEFAULT NULL,
    package_size    int8         DEFAULT NULL,
    package_url     varchar(500) DEFAULT NULL,
    status          char(1)      DEFAULT '0',
    is_security     char(1)      DEFAULT '0',
    access_mode     varchar(20)  DEFAULT 'all' NOT NULL,
    use_count       int8         DEFAULT 0,
    recommend_count int8         DEFAULT 0,
    order_num       int4         DEFAULT 0,
    tenant_id       varchar(20)  DEFAULT '000000',
    del_flag        char(1)      DEFAULT '0',
    create_dept     int8         DEFAULT NULL,
    create_by       int8         DEFAULT NULL,
    create_time     timestamp    DEFAULT NULL,
    update_by       int8         DEFAULT NULL,
    update_time     timestamp    DEFAULT NULL,
    remark          varchar(500) DEFAULT NULL,
    CONSTRAINT pk_app_application PRIMARY KEY (app_id)
);
CREATE UNIQUE INDEX uk_app_application_code ON app_application (tenant_id, app_code) WHERE del_flag = '0';
CREATE INDEX idx_app_application_category ON app_application (category_id);
CREATE INDEX idx_app_application_status ON app_application (status, is_security);
COMMENT ON TABLE app_application IS '应用';

-- ----------------------------------------------------------------
-- 应用开放范围
-- ----------------------------------------------------------------
CREATE TABLE app_access_scope (
    scope_id    int8        NOT NULL,
    app_id      int8        NOT NULL,
    target_type varchar(20) NOT NULL,
    target_id   int8        NOT NULL,
    tenant_id   varchar(20) DEFAULT '000000',
    create_dept int8        DEFAULT NULL,
    create_by   int8        DEFAULT NULL,
    create_time timestamp   DEFAULT NULL,
    update_by   int8        DEFAULT NULL,
    update_time timestamp   DEFAULT NULL,
    remark      varchar(500) DEFAULT NULL,
    CONSTRAINT pk_app_access_scope PRIMARY KEY (scope_id)
);
CREATE UNIQUE INDEX uk_app_access_scope_target ON app_access_scope (tenant_id, app_id, target_type, target_id);
CREATE INDEX idx_app_access_scope_app ON app_access_scope (tenant_id, app_id);
COMMENT ON TABLE app_access_scope IS '应用开放范围';

-- ----------------------------------------------------------------
-- 应用收藏
-- ----------------------------------------------------------------
CREATE TABLE app_favorite (
    id          int8 NOT NULL,
    app_id      int8 NOT NULL,
    user_id     int8 NOT NULL,
    create_time timestamp DEFAULT NULL,
    CONSTRAINT pk_app_favorite PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_fav_user_app ON app_favorite (user_id, app_id);
COMMENT ON TABLE app_favorite IS '应用收藏';

-- ----------------------------------------------------------------
-- 应用推荐
-- ----------------------------------------------------------------
CREATE TABLE app_recommend (
    id          int8 NOT NULL,
    app_id      int8 NOT NULL,
    user_id     int8 NOT NULL,
    create_time timestamp DEFAULT NULL,
    CONSTRAINT pk_app_recommend PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_rec_user_app ON app_recommend (user_id, app_id);
COMMENT ON TABLE app_recommend IS '应用推荐';

-- ----------------------------------------------------------------
-- 门户通知消息
-- ----------------------------------------------------------------
CREATE TABLE app_message (
    message_id  int8         NOT NULL,
    user_id     int8         NOT NULL,
    title       varchar(200) NOT NULL,
    content     text         DEFAULT NULL,
    msg_type    varchar(20)  DEFAULT 'system',
    is_read     char(1)      DEFAULT '0',
    create_time timestamp    DEFAULT NULL,
    CONSTRAINT pk_app_message PRIMARY KEY (message_id)
);
CREATE INDEX idx_app_message_user ON app_message (user_id, is_read);
COMMENT ON TABLE app_message IS '门户通知消息';

-- ----------------------------------------------------------------
-- 种子数据：v0.3.3 版本升级通知
-- ----------------------------------------------------------------
INSERT INTO app_message (message_id, user_id, title, content, msg_type, is_read, create_time)
SELECT
    2073003000000000000 + user_id,
    user_id,
    '版本升级：v0.3.3 门户通知上线',
    'v0.3.3 新增门户右上角统一通知，支持版本升级、资源新增、应用上架、论坛反馈等消息提醒。',
    'version',
    '0',
    now()
FROM sys_user
WHERE status = '0' AND del_flag = '0'
ON CONFLICT (message_id) DO NOTHING;

-- ----------------------------------------------------------------
-- 应用需求反馈
-- ----------------------------------------------------------------
CREATE TABLE app_demand (
    demand_id      int8          NOT NULL,
    demand_type    varchar(20)   NOT NULL,
    app_id         int8          DEFAULT NULL,
    app_name       varchar(100)  NOT NULL,
    reference_url  varchar(500)  DEFAULT NULL,
    content        text          NOT NULL,
    contact        varchar(100)  DEFAULT NULL,
    requester_id   int8          DEFAULT NULL,
    requester_name varchar(100)  DEFAULT NULL,
    status         char(1)       DEFAULT '0',
    handle_remark  varchar(1000) DEFAULT NULL,
    handled_by     int8          DEFAULT NULL,
    handled_time   timestamp     DEFAULT NULL,
    tenant_id      varchar(20)   DEFAULT '000000',
    del_flag       char(1)       DEFAULT '0',
    create_dept    int8          DEFAULT NULL,
    create_by      int8          DEFAULT NULL,
    create_time    timestamp     DEFAULT NULL,
    update_by      int8          DEFAULT NULL,
    update_time    timestamp     DEFAULT NULL,
    CONSTRAINT pk_app_demand PRIMARY KEY (demand_id)
);
CREATE INDEX idx_app_demand_status ON app_demand (tenant_id, status, create_time);
CREATE INDEX idx_app_demand_requester ON app_demand (requester_id, create_time);
COMMENT ON TABLE app_demand IS '应用需求反馈';

-- ----------------------------------------------------------------
-- 种子数据：分类
-- ----------------------------------------------------------------
INSERT INTO app_category (category_id, category_name, category_code, icon, order_num, create_time) VALUES
(1, '自研应用', 'self_hosted', 'component', 1, now()),
(2, '开源应用', 'open_source', 'open',      2, now()),
(3, '离线应用', 'offline',     'download',  3, now()),
(4, '治理工具', 'governance',  'database',  4, now());

-- ----------------------------------------------------------------
-- 种子数据：应用
-- ----------------------------------------------------------------
INSERT INTO app_application
    (app_id, app_name, app_code, version, category_id, icon, accent,
     description, tags, access_url, app_type, status, is_security,
     access_mode, use_count, recommend_count, order_num, create_time)
VALUES
(1, '应知应会', 'required-knowledge', 'v1', 1, 'education', '#2563eb',
 '面向内部学习、题库、考试与材料导入的自研应用入口。',
 '自研应用,题库,考试', '/portal/required-knowledge', 'business', '0', '0', 'user', 0, 0, 1, now()),
(2, 'Stirling PDF', 'stirling-pdf', '2.14.2', 2, 'PDF', '#2563eb',
 'PDF 合并、拆分、压缩、转换与页面处理应用，即开即用。',
 'PDF,文档处理,转换', 'http://127.0.0.1:18080', 'online', '1', '0', 'all', 128, 36, 2, now()),
(3, 'draw.io', 'drawio', '30.3.11', 2, 'DIO', '#0f766e',
 '流程图、架构图、网络拓扑和业务图示绘制应用。',
 '流程图,架构图,绘图', 'http://127.0.0.1:18082/?offline=1&https=0&lang=zh', 'online', '1', '0', 'all', 96, 28, 3, now()),
(4, 'Excalidraw', 'excalidraw', '0.18.1', 2, 'EX', '#c2410c',
 '轻量白板和草图协作应用，适合快速表达方案和讨论。',
 '白板,草图,协作', 'http://127.0.0.1:18090', 'online', '1', '0', 'all', 84, 24, 4, now()),
(5, 'Memos', 'memos', '0.29.1', 2, 'M', '#16a34a',
 '轻量级备忘录与 Markdown 知识记录应用。',
 '备忘录,Markdown,知识记录', 'http://127.0.0.1:18110', 'online', '1', '0', 'all', 0, 0, 5, now()),
(6, 'File Browser', 'filebrowser', '2.63.18', 2, 'F', '#0891b2',
 '局域网网页文件管理器，支持目录浏览、上传和下载。',
 '文件管理,上传下载,局域网', 'http://127.0.0.1:18111', 'online', '1', '0', 'all', 0, 0, 6, now()),
(7, 'PairDrop', 'pairdrop', '1.11.2', 2, 'PD', '#2563eb',
 '无需客户端的局域网跨平台文件互传工具。',
 '文件互传,局域网,跨平台', 'http://127.0.0.1:18112', 'online', '1', '0', 'all', 0, 0, 7, now()),
(8, 'IT-Tools', 'it-tools', '2024.10.22', 2, 'IT', '#7c3aed',
 '编码转换、JSON、时间戳、网络与开发常用工具箱。',
 '工具箱,编码转换,开发工具', 'http://127.0.0.1:18113', 'online', '1', '0', 'all', 0, 0, 8, now()),
(9, 'Uptime Kuma', 'uptime-kuma', '2.4.0', 2, 'UK', '#16a34a',
 '网站、端口和服务可用性监控与状态页。',
 '监控,状态页,运维', 'http://127.0.0.1:18114', 'online', '1', '1', 'all', 0, 0, 9, now()),
(10, 'FreshRSS', 'freshrss', '1.29.1', 2, 'RSS', '#f97316',
 '自托管 RSS 聚合、订阅和阅读应用。',
 'RSS,资讯聚合,订阅', 'http://127.0.0.1:18115', 'online', '1', '0', 'all', 0, 0, 10, now()),
(11, 'Gitea', 'gitea', '1.27.0', 2, 'gitea', '#111827',
 '轻量内网 Git 托管、代码评审与协作平台。',
 'Git,代码托管,开发协作', 'http://127.0.0.1:18116', 'online', '1', '1', 'all', 0, 0, 11, now()),
(12, 'Jellyfin', 'jellyfin', '10.11.11', 2, 'JF', '#7c3aed',
 '培训视频、音频和内部媒体资料库。',
 '媒体库,视频,培训资料', 'http://127.0.0.1:18118', 'online', '1', '0', 'all', 0, 0, 12, now()),
(13, 'SearXNG', 'searxng', '2026.07.15', 2, 'SX', '#0f766e',
 '保护隐私的聚合搜索入口，外部搜索源需要联网。',
 '搜索,聚合,隐私', 'http://127.0.0.1:18119', 'online', '1', '0', 'all', 0, 0, 13, now()),
(5001, 'DataEase 态势后台', 'dataease-admin', 'latest', 4, 'DE', '#2563eb',
 '态势大屏制作、数据集配置与可视化发布后台，仅面向管理员开放。',
 '态势大屏,BI,DataEase', 'http://127.0.0.1:8100', 'online', '0', '1', 'role', 0, 0, 41, now()),
(5002, 'Apache Hop 数据加工', 'apache-hop', 'latest', 4, 'HOP', '#0f766e',
 '门户态势分析层数据加工、清洗与作业验证入口，仅面向管理员开放。',
 '数据治理,ETL,Hop', 'http://127.0.0.1:18091/ui', 'online', '0', '1', 'role', 0, 0, 42, now()),
(5003, 'Budibase 低代码工厂', 'budibase', 'latest', 4, 'BB', '#c2410c',
 '快速搭建需求台账、任务推进、问题闭环等内部低代码应用，仅面向管理员开放。',
 '低代码,应用工厂,Budibase', 'http://127.0.0.1:18100', 'online', '0', '1', 'role', 0, 0, 43, now());

INSERT INTO app_access_scope
    (scope_id, app_id, target_type, target_id, tenant_id, create_time)
VALUES
    (2073005000000000901, 1, 'user', 1, '000000', now()),
    (2073005000000005001, 5001, 'role', 1, '000000', now()),
    (2073005000000005002, 5002, 'role', 1, '000000', now()),
    (2073005000000005003, 5003, 'role', 1, '000000', now());

-- ----------------------------------------------------------------
-- sys_menu: 应用中心后台菜单 + 按钮权限
-- 幂等: ON CONFLICT (menu_id) DO NOTHING
-- ----------------------------------------------------------------

-- 应用中心 目录 (menu_id=2000)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2000, '应用中心', 0, 5, 'appcenter', NULL, '', '1', '0', 'M', '0', '0', '', 'shopping', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 应知应会 目录 (menu_id=4000)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (4000, '应知应会', 0, 9, 'required-knowledge', NULL, '', '1', '0', 'M', '0', '0', '', 'education', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 应知应会管理 菜单 (menu_id=4001-4003)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(4001, '题库管理', 4000, 3, 'questions', 'admin/required-knowledge/questions/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:question:list', 'question', 103, 1, now()),
(4002, '考试配置', 4000, 4, 'exams', 'admin/required-knowledge/exams/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:exam:list', 'education', 103, 1, now()),
(4003, 'OCR 导入', 4000, 5, 'ocr', 'admin/required-knowledge/ocr/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:ocr:list', 'upload', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 应用管理 菜单 (menu_id=2010)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2010, '应用管理', 2000, 1, 'application', 'admin/appcenter/application/index', '', '1', '0', 'C', '0', '0', 'appcenter:application:list', 'list', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 应用分类 菜单 (menu_id=2020)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2020, '应用分类', 2000, 2, 'category', 'admin/appcenter/category/index', '', '1', '0', 'C', '0', '0', 'appcenter:category:list', 'tree', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 需求反馈 菜单 (menu_id=2030)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2030, '需求反馈', 2000, 3, 'demand', 'admin/appcenter/demand/index', '', '1', '0', 'C', '0', '0', 'appcenter:demand:list', 'message', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 按钮权限 (menu_id 2011-2014, 2021-2024, 2031-2033)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(2011, '应用查询', 2010, 1, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:query',  '#', 103, 1, now()),
(2012, '应用新增', 2010, 2, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:add',    '#', 103, 1, now()),
(2013, '应用修改', 2010, 3, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:edit',   '#', 103, 1, now()),
(2014, '应用删除', 2010, 4, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:remove', '#', 103, 1, now()),
(2021, '分类查询', 2020, 1, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:query',     '#', 103, 1, now()),
(2022, '分类新增', 2020, 2, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:add',       '#', 103, 1, now()),
(2023, '分类修改', 2020, 3, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:edit',      '#', 103, 1, now()),
(2024, '分类删除', 2020, 4, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:remove',    '#', 103, 1, now()),
(2031, '需求查询', 2030, 1, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:demand:query',       '#', 103, 1, now()),
(2032, '需求处理', 2030, 2, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:demand:edit',        '#', 103, 1, now()),
(2033, '需求删除', 2030, 3, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:demand:remove',      '#', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;
