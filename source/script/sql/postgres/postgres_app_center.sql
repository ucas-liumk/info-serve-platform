-- ============================================================
-- 应用中心 App Center — PostgreSQL DDL + 种子数据 + 菜单权限
-- 目标库: ry-cloud
-- 幂等: 所有 DROP / CREATE / INSERT 均可安全重复执行
-- ============================================================

-- ----------------------------------------------------------------
-- 关联表必须先删(外键顺序)
-- ----------------------------------------------------------------
DROP TABLE IF EXISTS app_message;
DROP TABLE IF EXISTS app_recommend;
DROP TABLE IF EXISTS app_favorite;
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
    status          char(1)      DEFAULT '0',
    is_security     char(1)      DEFAULT '0',
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
-- 应用中心消息
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
COMMENT ON TABLE app_message IS '应用中心消息';

-- ----------------------------------------------------------------
-- 种子数据：分类 (5条)
-- ----------------------------------------------------------------
INSERT INTO app_category (category_id, category_name, category_code, icon, order_num, create_time) VALUES
(1, '云原生',   'cloud',    'cloud',  1, now()),
(2, '开发工具', 'dev',      'code',   2, now()),
(3, '数据存储', 'data',     'db',     3, now()),
(4, '监控运维', 'ops',      'gauge',  4, now()),
(5, '安全工具', 'security', 'shield', 5, now());

-- ----------------------------------------------------------------
-- 种子数据：应用 (6条)
-- ----------------------------------------------------------------
INSERT INTO app_application
    (app_id, app_name, app_code, version, category_id, icon, accent,
     description, tags, access_url, status, is_security,
     use_count, recommend_count, order_num, create_time)
VALUES
(1, 'Dify',      'dify',      'v1.2.0',  1, 'D', 'blue',
 '开源大语言模型(LLM)应用开发平台,提供可视化编排与部署能力。',
 'AI模型,LLM,低代码',     'http://192.168.8.4:18001', '0', '0', 356, 128, 1, now()),
(2, 'Airflow',   'airflow',   'v2.7.1',  1, 'A', 'violet',
 '开源工作流调度平台,支持复杂任务编排、调度和监控。',
 '工作流,调度',            'http://192.168.8.4:18002', '0', '0', 289,  90, 2, now()),
(3, 'MinIO',     'minio',     'RELEASE', 3, 'M', 'slate',
 '高性能分布式对象存储,兼容 S3 协议,适合缓存与备份。',
 '存储,对象存储,S3',       'http://192.168.8.4:18003', '0', '0', 210,  86, 3, now()),
(4, 'Grafana',   'grafana',   'v10.2.0', 4, 'G', 'orange',
 '开源可观测性平台,支持多数据源可视化与监控告警。',
 '监控,可视化,告警',       'http://192.168.8.4:18004', '0', '0', 532, 140, 4, now()),
(5, 'Portainer', 'portainer', 'v2.19.3', 1, 'P', 'cyan',
 '轻量级容器管理 UI,支持 Docker 与 Kubernetes 管理。',
 '容器,Docker,K8s',        'http://192.168.8.4:18005', '0', '0',  98,  42, 5, now()),
(6, 'Vault',     'vault',     'v1.15.0', 5, 'V', 'black',
 '开源密钥管理与数据保护工具,支持动态密钥与访问控制。',
 '安全,密钥管理',           'http://192.168.8.4:18006', '0', '1', 133,  60, 6, now());

-- ----------------------------------------------------------------
-- sys_menu: 应用中心目录 + 菜单 + 按钮权限
-- 幂等: ON CONFLICT (menu_id) DO NOTHING
-- ----------------------------------------------------------------

-- 应用中心 目录 (menu_id=2000)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2000, '应用中心', 0, 5, 'appcenter', NULL, '', '1', '0', 'M', '0', '0', '', 'shopping', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 应用管理 菜单 (menu_id=2010)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2010, '应用管理', 2000, 1, 'application', 'appcenter/application/index', '', '1', '0', 'C', '0', '0', 'appcenter:application:list', 'list', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 分类管理 菜单 (menu_id=2020)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2020, '分类管理', 2000, 2, 'category', 'appcenter/category/index', '', '1', '0', 'C', '0', '0', 'appcenter:category:list', 'tree', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 按钮权限 (menu_id 2011-2014, 2021-2024)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(2011, '应用查询', 2010, 1, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:query',  '#', 103, 1, now()),
(2012, '应用新增', 2010, 2, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:add',    '#', 103, 1, now()),
(2013, '应用修改', 2010, 3, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:edit',   '#', 103, 1, now()),
(2014, '应用删除', 2010, 4, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:application:remove', '#', 103, 1, now()),
(2021, '分类查询', 2020, 1, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:query',     '#', 103, 1, now()),
(2022, '分类新增', 2020, 2, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:add',       '#', 103, 1, now()),
(2023, '分类修改', 2020, 3, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:edit',      '#', 103, 1, now()),
(2024, '分类删除', 2020, 4, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:category:remove',    '#', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;
