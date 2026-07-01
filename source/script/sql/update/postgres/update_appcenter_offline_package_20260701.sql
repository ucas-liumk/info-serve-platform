-- 应用中心：应用类型与离线安装包字段

ALTER TABLE app_application
    ADD COLUMN IF NOT EXISTS app_type varchar(20) DEFAULT 'online' NOT NULL,
    ADD COLUMN IF NOT EXISTS package_oss_id int8 DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_name varchar(255) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_size int8 DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS package_url varchar(500) DEFAULT NULL;

UPDATE app_application
SET app_type = 'online'
WHERE app_type IS NULL OR app_type = '';

UPDATE sys_menu
SET menu_name = '应用管理'
WHERE menu_id = 2010;

UPDATE sys_menu
SET menu_name = '应用分类'
WHERE menu_id = 2020;
