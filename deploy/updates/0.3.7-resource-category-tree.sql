-- info-serve v0.3.7 资料栏目/分类两级体系
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_info_service.sql 同步维护（种子管新装、本文件管存量）
-- 幂等可重放：ADD COLUMN IF NOT EXISTS + ON CONFLICT DO NOTHING + 条件 UPDATE

-- 1) 分类表增加父级列（NULL=栏目，非空=分类，两级封顶）
ALTER TABLE info_resource_category ADD COLUMN IF NOT EXISTS parent_id int8 DEFAULT NULL;
COMMENT ON COLUMN info_resource_category.parent_id IS '父栏目ID（NULL=栏目，非空=分类，两级封顶）';

-- 2) 默认栏目「综合资料」（category_id=300000 / code general，承接存量分类）
INSERT INTO info_resource_category (category_id, parent_id, category_name, category_code, description, icon, order_num, create_time)
VALUES (300000, NULL, '综合资料', 'general', '默认栏目，承接通用共享资料分类。', 'folder', 0, now())
ON CONFLICT (category_id) DO NOTHING;

-- 3) 存量分类（含孤儿分类兜底）挂入默认栏目：仅补未挂父级的行，重放无副作用
UPDATE info_resource_category
SET parent_id = 300000
WHERE parent_id IS NULL
  AND category_id <> 300000;
