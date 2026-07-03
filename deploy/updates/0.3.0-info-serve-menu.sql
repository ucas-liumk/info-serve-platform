-- 现有库菜单迁移：后台菜单按门户 App 板块统一到“门户应用管理”下。
-- 适用场景：数据库已经初始化过，不能通过重新导入 postgres_app_center.sql / postgres_info_service.sql 生效。

UPDATE sys_menu
SET menu_name = U&'\95E8\6237\5E94\7528\7BA1\7406',
    order_num = 5,
    path = 'portal-apps',
    component = NULL,
    icon = 'shopping',
    visible = '0',
    status = '0'
WHERE menu_id = 2000;

UPDATE sys_menu
SET menu_name = U&'\5DE5\5177\5E94\7528',
    parent_id = 2000,
    order_num = 1,
    path = 'application',
    component = 'admin/appcenter/application/index'
WHERE menu_id = 2010;

UPDATE sys_menu
SET menu_name = U&'\5DE5\5177\5206\7C7B',
    parent_id = 2000,
    order_num = 2,
    path = 'category',
    component = 'admin/appcenter/category/index'
WHERE menu_id = 2020;

UPDATE sys_menu
SET menu_name = U&'\8D44\6599\5171\4EAB',
    parent_id = 2000,
    order_num = 3,
    path = 'resource',
    component = 'admin/resources/resource/index'
WHERE menu_id = 3010;

UPDATE sys_menu
SET menu_name = U&'\8D44\6599\5206\7C7B',
    parent_id = 2000,
    order_num = 4,
    path = 'resource-category',
    component = 'admin/resources/category/index'
WHERE menu_id = 3020;

UPDATE sys_menu
SET menu_name = U&'\8BBA\575B\4E3B\9898',
    parent_id = 2000,
    order_num = 5,
    path = 'forum-topic',
    component = 'admin/forum/topic/index'
WHERE menu_id = 3030;

UPDATE sys_menu
SET menu_name = U&'\8BBA\575B\7248\5757',
    parent_id = 2000,
    order_num = 6,
    path = 'forum-board',
    component = 'admin/forum/board/index'
WHERE menu_id = 3040;

-- 老的“信息服务”顶级目录不再展示，资料和论坛已经并入“门户应用管理”。
UPDATE sys_menu
SET menu_name = U&'\4FE1\606F\670D\52A1',
    visible = '1',
    status = '1',
    order_num = 99
WHERE menu_id = 3000;

-- 如果某些角色原来只勾选了资料/论坛子菜单，补上新的父菜单，避免树形菜单缺父节点。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 2000
FROM sys_role_menu
WHERE menu_id IN (2010, 2020, 3010, 3020, 3030, 3040)
ON CONFLICT (role_id, menu_id) DO NOTHING;
