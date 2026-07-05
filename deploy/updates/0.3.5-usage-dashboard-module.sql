-- info-serve v0.3.5 应用态势看板入口
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
    (6, 'usage-dashboard', '应用态势', '洞察系统应用  透明运行态势', '/portal/usage-dashboard', '0', 6, now())
ON CONFLICT (module_id) DO UPDATE
SET module_code = EXCLUDED.module_code,
    module_name = EXCLUDED.module_name,
    description = EXCLUDED.description,
    entry_path = EXCLUDED.entry_path,
    status = EXCLUDED.status,
    sort_order = EXCLUDED.sort_order,
    update_time = now();
