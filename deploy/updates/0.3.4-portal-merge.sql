-- info-serve v0.3.4 门户服务合并（appcenter + infoservice -> ruoyi-portal）
-- Run against PostgreSQL database: ry-cloud

-- 通知系统统一至内核（app_message，收件人取自 sys_user）。
-- 以下三张表为历史幻影实体：任何环境的初始化脚本均未创建过它们，
-- 代码曾引用但表从未存在（广播路径一直抛错）。防御性清理：
DROP TABLE IF EXISTS info_portal_message;
DROP TABLE IF EXISTS info_portal_user;
DROP TABLE IF EXISTS app_portal_user;
