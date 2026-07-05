-- info-serve v0.3.5 数据治理工具入口集成
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_app_center.sql / postgres_portal_kernel.sql 同步维护

ALTER TABLE app_application
    ADD COLUMN IF NOT EXISTS required_role_key varchar(100) DEFAULT NULL;

COMMENT ON COLUMN app_application.required_role_key IS '可见角色键；空为登录用户可见，非空时需匹配当前用户角色键';

CREATE INDEX IF NOT EXISTS idx_app_application_required_role
    ON app_application (tenant_id, required_role_key)
    WHERE del_flag = '0';

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
     description, tags, access_url, app_type, required_role_key, status, is_security,
     use_count, recommend_count, order_num, create_time)
VALUES
    (5001, 'DataEase 态势后台', 'dataease-admin', 'latest', 4, 'DE', '#2563eb',
     '态势大屏制作、数据集配置与可视化发布后台，仅面向管理员开放。',
     '态势大屏,BI,DataEase', 'http://127.0.0.1:8100', 'online', 'superadmin', '0', '1', 0, 0, 41, now()),
    (5002, 'Apache Hop 数据加工', 'apache-hop', 'latest', 4, 'HOP', '#0f766e',
     '门户态势分析层数据加工、清洗与作业验证入口，仅面向管理员开放。',
     '数据治理,ETL,Hop', 'http://127.0.0.1:18091/ui', 'online', 'superadmin', '0', '1', 0, 0, 42, now()),
    (5003, 'Budibase 低代码工厂', 'budibase', 'latest', 4, 'BB', '#c2410c',
     '快速搭建需求台账、任务推进、问题闭环等内部低代码应用，仅面向管理员开放。',
     '低代码,应用工厂,Budibase', 'http://127.0.0.1:18100', 'online', 'superadmin', '0', '1', 0, 0, 43, now())
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
    required_role_key = EXCLUDED.required_role_key,
    status = EXCLUDED.status,
    is_security = EXCLUDED.is_security,
    order_num = EXCLUDED.order_num,
    update_time = now();

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
