-- 现有库迁移：新增门户“需求反馈”表、后台菜单和权限。
-- 适用场景：数据库已经初始化过，不能通过重新导入 postgres_app_center.sql 生效。

CREATE TABLE IF NOT EXISTS app_demand (
    demand_id      int8          NOT NULL,
    demand_type    varchar(20)   NOT NULL,
    app_id         int8          DEFAULT NULL,
    app_name       varchar(100)  NOT NULL,
    reference_url  varchar(500)  DEFAULT NULL,
    content        text          NOT NULL,
    contact        varchar(100)  DEFAULT NULL,
    requester_id   int8          DEFAULT NULL,
    requester_name varchar(100)  DEFAULT NULL,
    status         char(1)       DEFAULT '0',
    handle_remark  varchar(1000) DEFAULT NULL,
    handled_by     int8          DEFAULT NULL,
    handled_time   timestamp     DEFAULT NULL,
    tenant_id      varchar(20)   DEFAULT '000000',
    del_flag       char(1)       DEFAULT '0',
    create_dept    int8          DEFAULT NULL,
    create_by      int8          DEFAULT NULL,
    create_time    timestamp     DEFAULT NULL,
    update_by      int8          DEFAULT NULL,
    update_time    timestamp     DEFAULT NULL,
    CONSTRAINT pk_app_demand PRIMARY KEY (demand_id)
);

CREATE INDEX IF NOT EXISTS idx_app_demand_status ON app_demand (tenant_id, status, create_time);
CREATE INDEX IF NOT EXISTS idx_app_demand_requester ON app_demand (requester_id, create_time);
COMMENT ON TABLE app_demand IS U&'\5E94\7528\9700\6C42\53CD\9988';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES (2030, U&'\9700\6C42\53CD\9988', 2000, 3, 'demand', 'admin/appcenter/demand/index', '', '1', '0', 'C', '0', '0', 'appcenter:demand:list', 'message', 103, 1, now())
ON CONFLICT (menu_id) DO UPDATE
SET menu_name = EXCLUDED.menu_name,
    parent_id = EXCLUDED.parent_id,
    order_num = EXCLUDED.order_num,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status,
    perms = EXCLUDED.perms,
    icon = EXCLUDED.icon;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time)
VALUES
(2031, U&'\9700\6C42\67E5\8BE2', 2030, 1, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:demand:query',  '#', 103, 1, now()),
(2032, U&'\9700\6C42\5904\7406', 2030, 2, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:demand:edit',   '#', 103, 1, now()),
(2033, U&'\9700\6C42\5220\9664', 2030, 3, '', '', '', '1', '0', 'F', '0', '0', 'appcenter:demand:remove', '#', 103, 1, now())
ON CONFLICT (menu_id) DO NOTHING;

-- 已拥有“门户应用管理”的角色，自动获得需求反馈菜单与按钮权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
CROSS JOIN (VALUES (2030), (2031), (2032), (2033)) AS m(menu_id)
WHERE rm.menu_id = 2000
ON CONFLICT (role_id, menu_id) DO NOTHING;
