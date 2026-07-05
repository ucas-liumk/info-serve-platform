-- info-serve v0.3.5 required-knowledge AppCenter entry
-- Run against PostgreSQL database: ry-cloud

DO $$
DECLARE
    v_category_id int8;
    v_app_id int8;
    v_scope_id int8;
BEGIN
    SELECT category_id
      INTO v_category_id
      FROM app_category
     WHERE category_code = 'self_hosted'
       AND del_flag = '0'
     ORDER BY category_id
     LIMIT 1;

    IF v_category_id IS NULL THEN
        v_category_id := 2073005000000000001;
        WHILE EXISTS (SELECT 1 FROM app_category WHERE category_id = v_category_id) LOOP
            v_category_id := v_category_id + 1;
        END LOOP;

        INSERT INTO app_category
            (category_id, category_name, category_code, icon, order_num, status, del_flag, create_time, remark)
        VALUES
            (v_category_id, '自研应用', 'self_hosted', 'component', 1, '0', '0', now(), '自研业务应用');
    ELSE
        UPDATE app_category
           SET category_name = '自研应用',
               icon = 'component',
               order_num = 1,
               status = '0',
               update_time = now(),
               remark = '自研业务应用'
         WHERE category_id = v_category_id;
    END IF;

    SELECT app_id
      INTO v_app_id
      FROM app_application
     WHERE tenant_id = '000000'
       AND app_code = 'required-knowledge'
       AND del_flag = '0'
     ORDER BY app_id
     LIMIT 1;

    IF v_app_id IS NULL THEN
        v_app_id := 2073005000000000101;
        WHILE EXISTS (SELECT 1 FROM app_application WHERE app_id = v_app_id) LOOP
            v_app_id := v_app_id + 1;
        END LOOP;

        INSERT INTO app_application
            (app_id, app_name, app_code, version, category_id, icon, accent,
             description, tags, access_url, app_type, status, is_security,
             access_mode, use_count, recommend_count, order_num, tenant_id, del_flag, create_time, remark)
        VALUES
            (v_app_id, '应知应会', 'required-knowledge', 'v1', v_category_id, 'education', '#2563eb',
             '内部学习、题库、考试与材料导入的自研应用入口。',
             '自研应用,题库,考试', '/portal/required-knowledge', 'business', '0', '0',
             'user', 0, 0, 1, '000000', '0', now(), '门户内置学习考试应用');
    ELSE
        UPDATE app_application
           SET app_name = '应知应会',
               version = 'v1',
               category_id = v_category_id,
               icon = 'education',
               accent = '#2563eb',
               description = '内部学习、题库、考试与材料导入的自研应用入口。',
               tags = '自研应用,题库,考试',
               access_url = '/portal/required-knowledge',
               app_type = 'business',
               status = '0',
               is_security = '0',
               access_mode = COALESCE(NULLIF(access_mode, ''), 'user'),
               order_num = 1,
               update_time = now(),
               remark = '门户内置学习考试应用'
         WHERE app_id = v_app_id;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM app_application
         WHERE app_id = v_app_id
           AND access_mode = 'user'
    ) AND NOT EXISTS (
        SELECT 1
          FROM app_access_scope
         WHERE tenant_id = '000000'
           AND app_id = v_app_id
           AND target_type = 'user'
           AND target_id = 1
    ) THEN
        v_scope_id := 2073005000000000901;
        WHILE EXISTS (SELECT 1 FROM app_access_scope WHERE scope_id = v_scope_id) LOOP
            v_scope_id := v_scope_id + 1;
        END LOOP;

        INSERT INTO app_access_scope
            (scope_id, app_id, target_type, target_id, tenant_id, create_time)
        VALUES
            (v_scope_id, v_app_id, 'user', 1, '000000', now());
    END IF;
END $$;
