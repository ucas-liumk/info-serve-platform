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
-- 种子数据：分类 (3条)
-- ----------------------------------------------------------------
INSERT INTO app_category (category_id, category_name, category_code, icon, order_num, create_time) VALUES
(1, '文档处理', 'document', 'document', 1, now()),
(2, '图形绘制', 'diagram',  'edit',     2, now()),
(3, '协作白板', 'whiteboard','guide',    3, now());

-- ----------------------------------------------------------------
-- 种子数据：应用 (3条)
-- ----------------------------------------------------------------
INSERT INTO app_application
    (app_id, app_name, app_code, version, category_id, icon, accent,
     description, tags, access_url, status, is_security,
     use_count, recommend_count, order_num, create_time)
VALUES
(1, 'Stirling PDF', 'stirling-pdf', 'latest', 1, 'PDF', '#2563eb',
 'PDF 合并、拆分、压缩、转换与页面处理工具，即开即用。',
 'PDF,文档处理,转换', 'http://127.0.0.1:18080', '0', '0', 128, 36, 1, now()),
(2, 'draw.io', 'drawio', 'latest', 2, 'DIO', '#0f766e',
 '流程图、架构图、网络拓扑和业务图示绘制工具。',
 '流程图,架构图,绘图', 'http://127.0.0.1:18082', '0', '0', 96, 28, 2, now()),
(3, 'Excalidraw', 'excalidraw', 'latest', 3, 'EX', '#c2410c',
 '轻量白板和草图协作工具，适合快速表达方案和讨论。',
 '白板,草图,协作', 'http://127.0.0.1:18090', '0', '0', 84, 24, 3, now());

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
