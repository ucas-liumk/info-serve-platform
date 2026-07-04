-- info-serve v0.3.4 门户模块注册表
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_portal_kernel.sql 同步维护（种子管新装、本文件管存量）

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

INSERT INTO portal_module (module_id, module_code, module_name, description, entry_path, status, sort_order, create_time)
VALUES
    (1, 'resources', '资料共享', '数据汇聚  共享共用', '/portal/resources', '0', 1, now()),
    (2, 'appcenter', '工具即用', '开箱即用  提升效率', '/portal/tools',     '0', 2, now()),
    (3, 'qa',        '智能问答', '智慧问答  快速响应', NULL,               '1', 3, now()),
    (4, 'news',      '时事热点', '热点速递  洞察先机', NULL,               '1', 4, now()),
    (5, 'forum',     '服务论坛', '交流互动  共建共治', '/portal/forum',    '0', 5, now())
ON CONFLICT (module_id) DO NOTHING;
