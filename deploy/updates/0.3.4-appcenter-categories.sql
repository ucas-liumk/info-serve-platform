-- info-serve v0.3.4 appcenter category and naming update
-- Run against PostgreSQL database: ry-cloud

ALTER TABLE app_application
    ADD COLUMN IF NOT EXISTS app_type varchar(20) DEFAULT 'online' NOT NULL,
    ADD COLUMN IF NOT EXISTS package_oss_id int8 DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_name varchar(255) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_size int8 DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_url varchar(500) DEFAULT NULL;

INSERT INTO app_category (category_id, category_name, category_code, icon, order_num, status, create_time, update_time)
VALUES
    (1, '自研应用', 'self_hosted', 'component', 1, '0', now(), now()),
    (2, '开源应用', 'open_source', 'open', 2, '0', now(), now()),
    (3, '离线应用', 'offline', 'download', 3, '0', now(), now())
ON CONFLICT (category_id) DO UPDATE
SET category_name = EXCLUDED.category_name,
    category_code = EXCLUDED.category_code,
    icon = EXCLUDED.icon,
    order_num = EXCLUDED.order_num,
    status = EXCLUDED.status,
    del_flag = '0',
    update_time = now();

INSERT INTO app_application (
    app_id, app_name, app_code, version, category_id, icon, accent,
    description, tags, access_url, app_type, status, is_security,
    use_count, recommend_count, order_num, tenant_id, create_time, update_time
)
SELECT
    100, '应知应会', 'required-knowledge', 'latest', 1, 'education', '#2563eb',
    '面向内部学习、题库、考试与材料导入的自研应用入口。',
    '自研应用,题库,考试', '/admin/required-knowledge', 'business', '0', '1',
    0, 0, 1, '000000', now(), now()
WHERE NOT EXISTS (
    SELECT 1
    FROM app_application
    WHERE tenant_id = '000000'
      AND app_code = 'required-knowledge'
      AND del_flag = '0'
);

UPDATE app_application
SET category_id = 1,
    app_type = 'business',
    access_url = COALESCE(NULLIF(access_url, ''), '/admin/required-knowledge'),
    tags = COALESCE(NULLIF(tags, ''), '自研应用,题库,考试'),
    update_time = now()
WHERE tenant_id = '000000'
  AND app_code = 'required-knowledge'
  AND del_flag = '0';

UPDATE app_application
SET category_id = 3,
    app_type = 'offline',
    update_time = now()
WHERE app_type = 'offline'
  AND del_flag = '0';

UPDATE app_application
SET category_id = 1,
    update_time = now()
WHERE app_type = 'business'
  AND del_flag = '0';

UPDATE app_application
SET category_id = 2,
    app_type = 'online',
    update_time = now()
WHERE COALESCE(app_type, 'online') = 'online'
  AND app_code <> 'required-knowledge'
  AND del_flag = '0';

UPDATE sys_menu
SET menu_name = '应用中心',
    remark = '应用中心后台管理'
WHERE menu_id = 2000;

UPDATE portal_module
SET module_name = '应用中心',
    description = '应用聚合  即取即用',
    entry_path = '/portal/tools',
    status = '0',
    update_time = now()
WHERE module_code = 'appcenter';
