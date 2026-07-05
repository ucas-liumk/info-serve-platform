-- info-serve v0.3.4 appcenter category and naming update
-- Run against PostgreSQL database: ry-cloud

ALTER TABLE app_application
    ADD COLUMN IF NOT EXISTS app_type varchar(20) DEFAULT 'online' NOT NULL,
    ADD COLUMN IF NOT EXISTS package_oss_id int8 DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_name varchar(255) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_size int8 DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_url varchar(500) DEFAULT NULL;

WITH desired_categories(category_name, category_code, icon, order_num) AS (
    VALUES
        ('自研应用', 'self_hosted', 'component', 1),
        ('开源应用', 'open_source', 'open', 2),
        ('离线应用', 'offline', 'download', 3)
),
updated_categories AS (
    UPDATE app_category c
    SET category_name = d.category_name,
        icon = d.icon,
        order_num = d.order_num,
        status = '0',
        del_flag = '0',
        update_time = now()
    FROM desired_categories d
    WHERE c.category_code = d.category_code
      AND c.del_flag = '0'
    RETURNING c.category_code
),
category_id_base AS (
    SELECT COALESCE(MAX(category_id), 0) AS max_id FROM app_category
),
missing_categories AS (
    SELECT
        category_id_base.max_id + ROW_NUMBER() OVER (ORDER BY d.order_num) AS category_id,
        d.category_name,
        d.category_code,
        d.icon,
        d.order_num
    FROM desired_categories d
    CROSS JOIN category_id_base
    WHERE NOT EXISTS (
        SELECT 1
        FROM updated_categories u
        WHERE u.category_code = d.category_code
    )
)
INSERT INTO app_category (category_id, category_name, category_code, icon, order_num, status, create_time, update_time)
SELECT category_id, category_name, category_code, icon, order_num, '0', now(), now()
FROM missing_categories;

UPDATE app_category
SET status = '1',
    update_time = now()
WHERE del_flag = '0'
  AND category_code NOT IN ('self_hosted', 'open_source', 'offline');

INSERT INTO app_application (
    app_id, app_name, app_code, version, category_id, icon, accent,
    description, tags, access_url, app_type, status, is_security,
    use_count, recommend_count, order_num, tenant_id, create_time, update_time
)
SELECT
    100, '应知应会', 'required-knowledge', 'latest',
    (SELECT category_id FROM app_category WHERE category_code = 'self_hosted' AND del_flag = '0' LIMIT 1),
    'education', '#2563eb',
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
SET category_id = (SELECT category_id FROM app_category WHERE category_code = 'self_hosted' AND del_flag = '0' LIMIT 1),
    app_type = 'business',
    access_url = COALESCE(NULLIF(access_url, ''), '/admin/required-knowledge'),
    tags = COALESCE(NULLIF(tags, ''), '自研应用,题库,考试'),
    update_time = now()
WHERE tenant_id = '000000'
  AND app_code = 'required-knowledge'
  AND del_flag = '0';

UPDATE app_application
SET category_id = (SELECT category_id FROM app_category WHERE category_code = 'offline' AND del_flag = '0' LIMIT 1),
    app_type = 'offline',
    update_time = now()
WHERE app_type = 'offline'
  AND del_flag = '0';

UPDATE app_application
SET category_id = (SELECT category_id FROM app_category WHERE category_code = 'self_hosted' AND del_flag = '0' LIMIT 1),
    update_time = now()
WHERE app_type = 'business'
  AND del_flag = '0';

UPDATE app_application
SET category_id = (SELECT category_id FROM app_category WHERE category_code = 'open_source' AND del_flag = '0' LIMIT 1),
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
