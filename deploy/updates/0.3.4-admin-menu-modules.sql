-- info-serve v0.3.4 后台管理菜单按模块一级展示
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_app_center.sql / postgres_info_service.sql / postgres_portal_kernel.sql 同步维护。

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_dept, create_by, create_time, remark
)
VALUES
    (2000, '工具即用', 0, 5, 'appcenter', NULL, '', '1', '0', 'M', '0', '0', '', 'shopping', 103, 1, now(), '工具即用后台管理'),
    (3000, '资源共享', 0, 6, 'resources', NULL, '', '1', '0', 'M', '0', '0', '', 'documentation', 103, 1, now(), '资源共享后台管理'),
    (3050, '服务论坛', 0, 7, 'forum', NULL, '', '1', '0', 'M', '0', '0', '', 'message', 103, 1, now(), '服务论坛后台管理'),
    (2099, '门户配置', 0, 8, 'portal', NULL, '', '1', '0', 'M', '0', '0', '', 'component', 103, 1, now(), '门户公共配置'),
    (4000, '应知应会', 0, 9, 'required-knowledge', NULL, '', '1', '0', 'M', '0', '0', '', 'education', 103, 1, now(), '应知应会后台管理')
ON CONFLICT (menu_id) DO UPDATE
SET menu_name = EXCLUDED.menu_name,
    parent_id = EXCLUDED.parent_id,
    order_num = EXCLUDED.order_num,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    query_param = EXCLUDED.query_param,
    is_frame = EXCLUDED.is_frame,
    is_cache = EXCLUDED.is_cache,
    menu_type = EXCLUDED.menu_type,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status,
    perms = EXCLUDED.perms,
    icon = EXCLUDED.icon,
    remark = EXCLUDED.remark;

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_dept, create_by, create_time, remark
)
VALUES
    (4001, '题库管理', 4000, 1, 'questions', 'admin/required-knowledge/questions/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:question:list', 'question', 103, 1, now(), '应知应会题库管理'),
    (4002, '考试配置', 4000, 2, 'exams', 'admin/required-knowledge/exams/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:exam:list', 'education', 103, 1, now(), '应知应会考试配置'),
    (4003, 'OCR 导入', 4000, 3, 'ocr', 'admin/required-knowledge/ocr/index', '', '1', '0', 'C', '0', '0', 'requiredKnowledge:ocr:list', 'upload', 103, 1, now(), '应知应会材料识别导入')
ON CONFLICT (menu_id) DO UPDATE
SET menu_name = EXCLUDED.menu_name,
    parent_id = EXCLUDED.parent_id,
    order_num = EXCLUDED.order_num,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    query_param = EXCLUDED.query_param,
    is_frame = EXCLUDED.is_frame,
    is_cache = EXCLUDED.is_cache,
    menu_type = EXCLUDED.menu_type,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status,
    perms = EXCLUDED.perms,
    icon = EXCLUDED.icon,
    remark = EXCLUDED.remark;

UPDATE sys_menu
SET menu_name = '应用管理',
    parent_id = 2000,
    order_num = 1,
    path = 'application',
    component = 'admin/appcenter/application/index',
    icon = 'list'
WHERE menu_id = 2010;

UPDATE sys_menu
SET menu_name = '应用分类',
    parent_id = 2000,
    order_num = 2,
    path = 'category',
    component = 'admin/appcenter/category/index',
    icon = 'tree'
WHERE menu_id = 2020;

UPDATE sys_menu
SET menu_name = '需求反馈',
    parent_id = 2000,
    order_num = 3,
    path = 'demand',
    component = 'admin/appcenter/demand/index',
    icon = 'message'
WHERE menu_id = 2030;

UPDATE sys_menu
SET menu_name = '资源管理',
    parent_id = 3000,
    order_num = 1,
    path = 'resource',
    component = 'admin/resources/resource/index',
    icon = 'document'
WHERE menu_id = 3010;

UPDATE sys_menu
SET menu_name = '资源分类',
    parent_id = 3000,
    order_num = 2,
    path = 'category',
    component = 'admin/resources/category/index',
    icon = 'tree'
WHERE menu_id = 3020;

UPDATE sys_menu
SET menu_name = '论坛主题',
    parent_id = 3050,
    order_num = 1,
    path = 'topic',
    component = 'admin/forum/topic/index',
    icon = 'message'
WHERE menu_id = 3030;

UPDATE sys_menu
SET menu_name = '论坛版块',
    parent_id = 3050,
    order_num = 2,
    path = 'board',
    component = 'admin/forum/board/index',
    icon = 'list'
WHERE menu_id = 3040;

UPDATE sys_menu
SET parent_id = 2099,
    order_num = 1,
    path = 'module',
    component = 'admin/portal/module/index',
    icon = 'component'
WHERE menu_id = 2090;

-- 补齐新父级授权，保持存量角色原有可见模块不丢失。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 2000
FROM sys_role_menu
WHERE menu_id IN (2010, 2011, 2012, 2013, 2014, 2020, 2021, 2022, 2023, 2024, 2030, 2031, 2032, 2033)
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 3000
FROM sys_role_menu
WHERE menu_id IN (3010, 3011, 3012, 3013, 3014, 3015, 3020, 3021, 3022, 3023, 3024)
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 3050
FROM sys_role_menu
WHERE menu_id IN (3030, 3031, 3032, 3033, 3034, 3040, 3041, 3042, 3043, 3044)
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 2099
FROM sys_role_menu
WHERE menu_id IN (2090, 2091, 2092, 2093)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 如果存量角色已被授予“应知应会”一级入口，迁移后补齐二级菜单。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
CROSS JOIN (VALUES (4001), (4002), (4003)) AS m(menu_id)
WHERE rm.menu_id = 4000
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 4000
FROM sys_role_menu
WHERE menu_id IN (4001, 4002, 4003)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 旧版 2000 是“门户应用管理”父目录。只保留真正拥有工具即用子菜单的角色，避免资源/论坛管理员看到空工具菜单。
DELETE FROM sys_role_menu rm
WHERE rm.menu_id = 2000
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu app
      WHERE app.role_id = rm.role_id
        AND app.menu_id IN (2010, 2011, 2012, 2013, 2014, 2020, 2021, 2022, 2023, 2024, 2030, 2031, 2032, 2033)
  );
