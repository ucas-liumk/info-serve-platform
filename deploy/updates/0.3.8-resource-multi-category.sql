-- info-serve v0.3.8 资料多分类（资料↔分类 多对多）
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_info_service.sql 同步维护（种子管新装、本文件管存量）
-- 幂等可重放：CREATE IF NOT EXISTS + ON CONFLICT DO NOTHING（回填以既有单分类为基准，重放不重复）

-- 1) 资料-分类关联表（关联表为筛选/计数/删除守卫唯一事实源；info_resource.category_id 保留为主分类）
CREATE TABLE IF NOT EXISTS info_resource_category_link (
    resource_id  int8        NOT NULL,
    category_id  int8        NOT NULL,
    tenant_id    varchar(20) DEFAULT '000000',
    create_time  timestamp   DEFAULT now(),
    CONSTRAINT pk_info_resource_category_link PRIMARY KEY (resource_id, category_id)
);
CREATE INDEX IF NOT EXISTS idx_ircl_category ON info_resource_category_link (category_id);
CREATE INDEX IF NOT EXISTS idx_ircl_tenant ON info_resource_category_link (tenant_id);
COMMENT ON TABLE info_resource_category_link IS '资料-分类多对多关联（资料可挂多个二级分类；info_resource.category_id 为主分类）';

-- 2) 存量回填：每份资料的单分类进入关联表（含已删/下架资料，守卫与展示逻辑各自过滤）
INSERT INTO info_resource_category_link (resource_id, category_id, tenant_id, create_time)
SELECT resource_id, category_id, tenant_id, now()
FROM info_resource
WHERE category_id IS NOT NULL
ON CONFLICT (resource_id, category_id) DO NOTHING;
