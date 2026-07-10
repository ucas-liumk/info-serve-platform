-- info-serve v0.3.7 资料栏目/分类两级体系
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_info_service.sql 同步维护（种子管新装、本文件管存量）
-- 幂等可重放：ADD COLUMN IF NOT EXISTS + 双重防冲突 INSERT + 水位线 UPDATE（重放不波及上线后新建的栏目）

-- 1) 分类表增加父级列（NULL=栏目，非空=分类，两级封顶）
ALTER TABLE info_resource_category ADD COLUMN IF NOT EXISTS parent_id int8 DEFAULT NULL;
COMMENT ON COLUMN info_resource_category.parent_id IS '父栏目ID（NULL=栏目，非空=分类，两级封顶）';

-- 2) 默认栏目「综合资料」（category_id=300000 / code general，承接存量分类）
--    用 WHERE NOT EXISTS 同时防主键冲突与活跃编码冲突：uk_info_resource_category_code 是
--    部分唯一索引（WHERE del_flag='0'），ON CONFLICT (category_id) 兜不住编码撞车；
--    若存量库已有活跃的 general 编码则本步跳过，第 3 步因水位线子查询为空同样不会执行。
INSERT INTO info_resource_category (category_id, parent_id, category_name, category_code, description, icon, order_num, create_time)
SELECT 300000, NULL, '综合资料', 'general', '默认栏目，承接通用共享资料分类。', 'folder', 0, now()
WHERE NOT EXISTS (
    SELECT 1 FROM info_resource_category
    WHERE category_id = 300000
       OR (category_code = 'general' AND del_flag = '0')
);

-- 3) 迁移前的存量分类（含种子三行与用户平铺时代自建分类）挂入默认栏目。
--    以默认栏目的 create_time 为水位线：只迁移早于（含等于）首次投放时刻的行，
--    重放时上线后经管理页新建的栏目（create_time 晚于水位线）不受影响，幂等成立。
UPDATE info_resource_category
SET parent_id = 300000
WHERE parent_id IS NULL
  AND category_id <> 300000
  AND (create_time IS NULL
       OR create_time <= (SELECT create_time FROM info_resource_category WHERE category_id = 300000));
