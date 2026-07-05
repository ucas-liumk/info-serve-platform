-- info-serve v0.3.5 数据治理工具入口集成
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_app_center.sql / postgres_portal_kernel.sql 同步维护

ALTER TABLE app_application
    ADD COLUMN IF NOT EXISTS access_mode varchar(20) DEFAULT 'all' NOT NULL;

CREATE TABLE IF NOT EXISTS app_access_scope (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_access_scope_target
    ON app_access_scope (tenant_id, app_id, target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_app_access_scope_app
    ON app_access_scope (tenant_id, app_id);

INSERT INTO app_category (category_id, category_name, category_code, icon, order_num, status, create_time)
VALUES (4, '治理工具', 'governance', 'database', 4, '0', now())
ON CONFLICT (category_id) DO UPDATE
SET category_name = EXCLUDED.category_name,
    category_code = EXCLUDED.category_code,
    icon = EXCLUDED.icon,
    order_num = EXCLUDED.order_num,
    status = EXCLUDED.status,
    update_time = now();

INSERT INTO app_application
    (app_id, app_name, app_code, version, category_id, icon, accent,
     description, tags, access_url, app_type, status, is_security,
     access_mode, use_count, recommend_count, order_num, create_time)
VALUES
    (5001, 'DataEase 态势后台', 'dataease-admin', 'latest', 4, 'DE', '#2563eb',
     '态势大屏制作、数据集配置与可视化发布后台，仅面向管理员开放。',
     '态势大屏,BI,DataEase', 'http://127.0.0.1:8100', 'online', '0', '1', 'role', 0, 0, 41, now()),
    (5002, 'Apache Hop 数据加工', 'apache-hop', 'latest', 4, 'HOP', '#0f766e',
     '门户态势分析层数据加工、清洗与作业验证入口，仅面向管理员开放。',
     '数据治理,ETL,Hop', 'http://127.0.0.1:18091/ui', 'online', '0', '1', 'role', 0, 0, 42, now()),
    (5003, 'Budibase 低代码工厂', 'budibase', 'latest', 4, 'BB', '#c2410c',
     '快速搭建需求台账、任务推进、问题闭环等内部低代码应用，仅面向管理员开放。',
     '低代码,应用工厂,Budibase', 'http://127.0.0.1:18100', 'online', '0', '1', 'role', 0, 0, 43, now())
ON CONFLICT (tenant_id, app_code) WHERE del_flag = '0' DO UPDATE
SET app_name = EXCLUDED.app_name,
    version = EXCLUDED.version,
    category_id = EXCLUDED.category_id,
    icon = EXCLUDED.icon,
    accent = EXCLUDED.accent,
    description = EXCLUDED.description,
    tags = EXCLUDED.tags,
    access_url = EXCLUDED.access_url,
    app_type = EXCLUDED.app_type,
    access_mode = EXCLUDED.access_mode,
    status = EXCLUDED.status,
    is_security = EXCLUDED.is_security,
    order_num = EXCLUDED.order_num,
    update_time = now();

WITH target_app AS (
    SELECT app_id, tenant_id
    FROM app_application
    WHERE tenant_id = '000000' AND app_code = 'dataease-admin' AND del_flag = '0'
    LIMIT 1
), next_id AS (
    SELECT GREATEST(2073005000000005001::int8, COALESCE(MAX(scope_id) + 1, 2073005000000005001::int8)) AS scope_id
    FROM app_access_scope
)
INSERT INTO app_access_scope (scope_id, app_id, target_type, target_id, tenant_id, create_time)
SELECT next_id.scope_id, target_app.app_id, 'role', 1, target_app.tenant_id, now()
FROM target_app, next_id
WHERE NOT EXISTS (
    SELECT 1 FROM app_access_scope
    WHERE tenant_id = target_app.tenant_id AND app_id = target_app.app_id AND target_type = 'role' AND target_id = 1
);

WITH target_app AS (
    SELECT app_id, tenant_id
    FROM app_application
    WHERE tenant_id = '000000' AND app_code = 'apache-hop' AND del_flag = '0'
    LIMIT 1
), next_id AS (
    SELECT GREATEST(2073005000000005002::int8, COALESCE(MAX(scope_id) + 1, 2073005000000005002::int8)) AS scope_id
    FROM app_access_scope
)
INSERT INTO app_access_scope (scope_id, app_id, target_type, target_id, tenant_id, create_time)
SELECT next_id.scope_id, target_app.app_id, 'role', 1, target_app.tenant_id, now()
FROM target_app, next_id
WHERE NOT EXISTS (
    SELECT 1 FROM app_access_scope
    WHERE tenant_id = target_app.tenant_id AND app_id = target_app.app_id AND target_type = 'role' AND target_id = 1
);

WITH target_app AS (
    SELECT app_id, tenant_id
    FROM app_application
    WHERE tenant_id = '000000' AND app_code = 'budibase' AND del_flag = '0'
    LIMIT 1
), next_id AS (
    SELECT GREATEST(2073005000000005003::int8, COALESCE(MAX(scope_id) + 1, 2073005000000005003::int8)) AS scope_id
    FROM app_access_scope
)
INSERT INTO app_access_scope (scope_id, app_id, target_type, target_id, tenant_id, create_time)
SELECT next_id.scope_id, target_app.app_id, 'role', 1, target_app.tenant_id, now()
FROM target_app, next_id
WHERE NOT EXISTS (
    SELECT 1 FROM app_access_scope
    WHERE tenant_id = target_app.tenant_id AND app_id = target_app.app_id AND target_type = 'role' AND target_id = 1
);

DELETE FROM portal_user_module_preference
WHERE module_code = 'lowcode';

DELETE FROM portal_module
WHERE module_code = 'lowcode';

UPDATE portal_module
SET sort_order = 3,
    update_time = now()
WHERE module_code = 'usage-dashboard';

UPDATE portal_module
SET sort_order = 4,
    update_time = now()
WHERE module_code = 'forum';

UPDATE portal_module
SET sort_order = 80,
    update_time = now()
WHERE module_code = 'qa';

UPDATE portal_module
SET sort_order = 90,
    update_time = now()
WHERE module_code = 'news';
