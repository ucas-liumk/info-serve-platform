-- ============================================================
-- 信息中心数智服务平台一期 — PostgreSQL DDL + 种子数据 + 菜单权限
-- 目标库: ry-cloud
-- ============================================================

CREATE TABLE IF NOT EXISTS info_resource_category (
    category_id   int8         NOT NULL,
    category_name varchar(80)  NOT NULL,
    category_code varchar(80)  NOT NULL,
    description   varchar(500) DEFAULT NULL,
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
    CONSTRAINT pk_info_resource_category PRIMARY KEY (category_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_info_resource_category_code ON info_resource_category (category_code) WHERE del_flag = '0';
COMMENT ON TABLE info_resource_category IS '信息中心资料分类';

CREATE TABLE IF NOT EXISTS info_resource (
    resource_id    int8          NOT NULL,
    title          varchar(160)  NOT NULL,
    description    varchar(1000) DEFAULT NULL,
    category_id    int8          NOT NULL,
    oss_id         int8          NOT NULL,
    original_name  varchar(255)  DEFAULT NULL,
    file_suffix    varchar(50)   DEFAULT NULL,
    mime_type      varchar(120)  DEFAULT NULL,
    file_size      int8          DEFAULT 0,
    preview_type   varchar(30)   DEFAULT 'file',
    download_count int8          DEFAULT 0,
    view_count     int8          DEFAULT 0,
    status         char(1)       DEFAULT '0',
    tenant_id      varchar(20)   DEFAULT '000000',
    del_flag       char(1)       DEFAULT '0',
    create_dept    int8          DEFAULT NULL,
    create_by      int8          DEFAULT NULL,
    create_time    timestamp     DEFAULT NULL,
    update_by      int8          DEFAULT NULL,
    update_time    timestamp     DEFAULT NULL,
    remark         varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_info_resource PRIMARY KEY (resource_id)
);
CREATE INDEX IF NOT EXISTS idx_info_resource_category ON info_resource (category_id);
CREATE INDEX IF NOT EXISTS idx_info_resource_status ON info_resource (tenant_id, status, del_flag);
COMMENT ON TABLE info_resource IS '信息中心共享资料';

CREATE TABLE IF NOT EXISTS info_forum_board (
    board_id    int8         NOT NULL,
    board_name  varchar(80)  NOT NULL,
    board_code  varchar(80)  NOT NULL,
    description varchar(500) DEFAULT NULL,
    order_num   int4         DEFAULT 0,
    status      char(1)      DEFAULT '0',
    del_flag    char(1)      DEFAULT '0',
    create_dept int8         DEFAULT NULL,
    create_by   int8         DEFAULT NULL,
    create_time timestamp    DEFAULT NULL,
    update_by   int8         DEFAULT NULL,
    update_time timestamp    DEFAULT NULL,
    remark      varchar(500) DEFAULT NULL,
    CONSTRAINT pk_info_forum_board PRIMARY KEY (board_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_info_forum_board_code ON info_forum_board (board_code) WHERE del_flag = '0';
COMMENT ON TABLE info_forum_board IS '信息中心论坛版块';

CREATE TABLE IF NOT EXISTS info_forum_topic (
    topic_id    int8          NOT NULL,
    board_id    int8          NOT NULL,
    title       varchar(160)  NOT NULL,
    content     text          NOT NULL,
    author_id   int8          DEFAULT NULL,
    author_name varchar(80)   DEFAULT NULL,
    view_count  int8          DEFAULT 0,
    reply_count int8          DEFAULT 0,
    like_count  int8          DEFAULT 0,
    is_top      char(1)       DEFAULT '0',
    is_closed   char(1)       DEFAULT '0',
    status      char(1)       DEFAULT '0',
    tenant_id   varchar(20)   DEFAULT '000000',
    del_flag    char(1)       DEFAULT '0',
    create_dept int8          DEFAULT NULL,
    create_by   int8          DEFAULT NULL,
    create_time timestamp     DEFAULT NULL,
    update_by   int8          DEFAULT NULL,
    update_time timestamp     DEFAULT NULL,
    remark      varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_info_forum_topic PRIMARY KEY (topic_id)
);
CREATE INDEX IF NOT EXISTS idx_info_forum_topic_board ON info_forum_topic (board_id, status, del_flag);
CREATE INDEX IF NOT EXISTS idx_info_forum_topic_tenant ON info_forum_topic (tenant_id, status, del_flag);
COMMENT ON TABLE info_forum_topic IS '信息中心论坛主题';

CREATE TABLE IF NOT EXISTS info_forum_reply (
    reply_id    int8          NOT NULL,
    topic_id    int8          NOT NULL,
    content     text          NOT NULL,
    author_id   int8          DEFAULT NULL,
    author_name varchar(80)   DEFAULT NULL,
    status      char(1)       DEFAULT '0',
    tenant_id   varchar(20)   DEFAULT '000000',
    del_flag    char(1)       DEFAULT '0',
    create_dept int8          DEFAULT NULL,
    create_by   int8          DEFAULT NULL,
    create_time timestamp     DEFAULT NULL,
    update_by   int8          DEFAULT NULL,
    update_time timestamp     DEFAULT NULL,
    remark      varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_info_forum_reply PRIMARY KEY (reply_id)
);
CREATE INDEX IF NOT EXISTS idx_info_forum_reply_topic ON info_forum_reply (topic_id, status, del_flag);
COMMENT ON TABLE info_forum_reply IS '信息中心论坛回复';

CREATE TABLE IF NOT EXISTS info_forum_like (
    like_id     int8        NOT NULL,
    target_type varchar(30) NOT NULL,
    target_id   int8        NOT NULL,
    user_id     int8        NOT NULL,
    create_time timestamp   DEFAULT NULL,
    CONSTRAINT pk_info_forum_like PRIMARY KEY (like_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_info_forum_like_user_target ON info_forum_like (user_id, target_type, target_id);
COMMENT ON TABLE info_forum_like IS '信息中心论坛点赞';

CREATE TABLE IF NOT EXISTS info_resource_favorite (
    id          int8      NOT NULL,
    resource_id int8     NOT NULL,
    user_id     int8     NOT NULL,
    tenant_id   varchar(20) DEFAULT '000000' NOT NULL,
    create_time timestamp DEFAULT NULL,
    CONSTRAINT pk_info_resource_favorite PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_info_resource_fav_user_resource ON info_resource_favorite (user_id, resource_id);
CREATE INDEX IF NOT EXISTS idx_info_resource_fav_resource ON info_resource_favorite (resource_id);
CREATE INDEX IF NOT EXISTS idx_info_resource_fav_tenant ON info_resource_favorite (tenant_id);
COMMENT ON TABLE info_resource_favorite IS '信息中心资料收藏';

INSERT INTO info_resource_category (category_id, category_name, category_code, description, icon, order_num, create_time)
VALUES
(300001, '政策制度', 'policy', '规章制度、流程规范和政策类资料。', 'document', 1, now()),
(300002, '技术文档', 'tech', '系统说明、操作手册和技术方案资料。', 'code', 2, now()),
(300003, '常用模板', 'template', '工作模板、表单样例和可复用材料。', 'form', 3, now())
ON CONFLICT (category_id) DO NOTHING;

INSERT INTO info_forum_board (board_id, board_name, board_code, description, order_num, create_time)
VALUES
(310001, '服务咨询', 'consult', '平台使用、资料服务和工具服务相关咨询。', 1, now()),
(310002, '经验交流', 'share', '一线经验、问题处理和改进建议交流。', 2, now()),
(310003, '需求反馈', 'feedback', '新资料、新工具和服务改进需求反馈。', 3, now())
ON CONFLICT (board_id) DO NOTHING;

INSERT INTO info_forum_topic
    (topic_id, board_id, title, content, author_id, author_name, view_count, reply_count, like_count, is_top, is_closed, status, tenant_id, create_time)
VALUES
(320001, 310001, '信息中心数智服务平台一期上线试运行',
 '一期先开放资料共享、应用中心和服务论坛。试运行期间请在论坛反馈资料补充、应用接入和使用体验问题。',
 1, '管理员', 0, 0, 0, '1', '0', '0', '000000', now())
ON CONFLICT (topic_id) DO NOTHING;

-- 资源共享和服务论坛在后台作为一级模块展示，便于按模块授权管理员。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(3000, '资源共享', 0, 6, 'resources', NULL, '', '1', '0', 'M', '0', '0', '', 'documentation', 103, 1, now()),
(3050, '服务论坛', 0, 7, 'forum', NULL, '', '1', '0', 'M', '0', '0', '', 'message', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(3010, '资源管理', 3000, 1, 'resource', 'admin/resources/resource/index', '', '1', '0', 'C', '0', '0', 'infoservice:resource:list', 'document', 103, 1, now()),
(3020, '资源分类', 3000, 2, 'category', 'admin/resources/category/index', '', '1', '0', 'C', '0', '0', 'infoservice:resourceCategory:list', 'tree', 103, 1, now()),
(3030, '论坛主题', 3050, 1, 'topic', 'admin/forum/topic/index', '', '1', '0', 'C', '0', '0', 'infoservice:forumTopic:list', 'message', 103, 1, now()),
(3040, '论坛版块', 3050, 2, 'board', 'admin/forum/board/index', '', '1', '0', 'C', '0', '0', 'infoservice:forumBoard:list', 'list', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(3011, '资料查询', 3010, 1, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resource:query', '#', 103, 1, now()),
(3012, '资料新增', 3010, 2, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resource:add', '#', 103, 1, now()),
(3013, '资料修改', 3010, 3, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resource:edit', '#', 103, 1, now()),
(3014, '资料删除', 3010, 4, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resource:remove', '#', 103, 1, now()),
(3015, '资料上传', 3010, 5, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resource:upload', '#', 103, 1, now()),
(3021, '分类查询', 3020, 1, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resourceCategory:query', '#', 103, 1, now()),
(3022, '分类新增', 3020, 2, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resourceCategory:add', '#', 103, 1, now()),
(3023, '分类修改', 3020, 3, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resourceCategory:edit', '#', 103, 1, now()),
(3024, '分类删除', 3020, 4, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:resourceCategory:remove', '#', 103, 1, now()),
(3031, '主题查询', 3030, 1, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumTopic:query', '#', 103, 1, now()),
(3032, '主题新增', 3030, 2, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumTopic:add', '#', 103, 1, now()),
(3033, '主题修改', 3030, 3, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumTopic:edit', '#', 103, 1, now()),
(3034, '主题删除', 3030, 4, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumTopic:remove', '#', 103, 1, now()),
(3041, '版块查询', 3040, 1, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumBoard:query', '#', 103, 1, now()),
(3042, '版块新增', 3040, 2, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumBoard:add', '#', 103, 1, now()),
(3043, '版块修改', 3040, 3, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumBoard:edit', '#', 103, 1, now()),
(3044, '版块删除', 3040, 4, '', '', '', '1', '0', 'F', '0', '0', 'infoservice:forumBoard:remove', '#', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;
