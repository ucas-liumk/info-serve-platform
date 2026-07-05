-- 门户内核：模块注册表（后台可配置的门户模块启停/排序/权限可见性）
-- 统计口径不入表：由各 BC 契约按 module_code 约定提供

CREATE TABLE IF NOT EXISTS portal_module (
    module_id   int8         NOT NULL,
    module_code varchar(50)  NOT NULL,
    module_name varchar(50)  NOT NULL,
    description varchar(200) DEFAULT NULL,
    image       varchar(255) DEFAULT NULL,
    entry_path  varchar(200) DEFAULT NULL,
    perms       varchar(100) DEFAULT NULL,
    status      char(1)      DEFAULT '0' NOT NULL,
    sort_order  int4         DEFAULT 0 NOT NULL,
    create_by   varchar(64)  DEFAULT NULL,
    create_time timestamp    DEFAULT NULL,
    update_by   varchar(64)  DEFAULT NULL,
    update_time timestamp    DEFAULT NULL,
    CONSTRAINT pk_portal_module PRIMARY KEY (module_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portal_module_code ON portal_module (module_code);
COMMENT ON TABLE portal_module IS '门户模块注册表';

CREATE TABLE IF NOT EXISTS portal_user_module_preference (
    preference_id int8        NOT NULL,
    tenant_id     varchar(20) DEFAULT '000000' NOT NULL,
    user_id       int8        NOT NULL,
    module_code   varchar(50) NOT NULL,
    sort_order    int4        DEFAULT 0 NOT NULL,
    create_time   timestamp   DEFAULT NULL,
    update_time   timestamp   DEFAULT NULL,
    CONSTRAINT pk_portal_user_module_preference PRIMARY KEY (preference_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portal_user_module_preference_user_code ON portal_user_module_preference (tenant_id, user_id, module_code);
CREATE INDEX IF NOT EXISTS idx_portal_user_module_preference_order ON portal_user_module_preference (tenant_id, user_id, sort_order);
COMMENT ON TABLE portal_user_module_preference IS '门户首页用户模块排序偏好';
COMMENT ON COLUMN portal_user_module_preference.module_code IS '模块编码，对应 portal_module.module_code';
COMMENT ON COLUMN portal_user_module_preference.sort_order IS '用户自定义排序，前 6 项进入首页首屏';

INSERT INTO portal_module (module_id, module_code, module_name, description, entry_path, status, sort_order, create_time)
VALUES
    (1, 'resources', '资料共享', '数据汇聚  共享共用', '/portal/resources', '0', 1, now()),
    (2, 'appcenter', '工具即用', '开箱即用  提升效率', '/portal/tools',     '0', 2, now()),
    (3, 'qa',        '智能问答', '智慧问答  快速响应', NULL,               '1', 3, now()),
    (4, 'news',      '时事热点', '热点速递  洞察先机', NULL,               '1', 4, now()),
    (5, 'forum',     '服务论坛', '交流互动  共建共治', '/portal/forum',    '0', 5, now())
ON CONFLICT (module_id) DO NOTHING;

-- 管理后台菜单：门户模块注册表（挂在 2000 门户应用管理 目录下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
    (2090, '模块注册表', 2000, 9, 'module', 'portal/module/index', '', '1', '0', 'C', '0', '0', 'portal:module:list', 'component', 103, 1, now(), '门户模块启停/排序/权限配置'),
    (2091, '模块新增', 2090, 1, '', '', '', '1', '0', 'F', '0', '0', 'portal:module:add', '#', 103, 1, now(), ''),
    (2092, '模块修改', 2090, 2, '', '', '', '1', '0', 'F', '0', '0', 'portal:module:edit', '#', 103, 1, now(), ''),
    (2093, '模块删除', 2090, 3, '', '', '', '1', '0', 'F', '0', '0', 'portal:module:remove', '#', 103, 1, now(), '')
ON CONFLICT (menu_id) DO NOTHING;
