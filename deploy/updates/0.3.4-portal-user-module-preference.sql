-- info-serve v0.3.4 门户首页用户模块排序偏好
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_portal_kernel.sql 同步维护（种子管新装、本文件管存量）

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
