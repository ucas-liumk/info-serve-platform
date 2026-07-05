-- info-serve v0.3.5 AppCenter access scope
-- Run against PostgreSQL database: ry-cloud

ALTER TABLE app_application
    ADD COLUMN IF NOT EXISTS access_mode varchar(20) DEFAULT 'all' NOT NULL;

UPDATE app_application
   SET access_mode = 'all'
 WHERE access_mode IS NULL
    OR access_mode = '';

CREATE TABLE IF NOT EXISTS app_access_scope (
    scope_id    int8        NOT NULL,
    app_id      int8        NOT NULL,
    target_type varchar(20) NOT NULL,
    target_id   int8        NOT NULL,
    tenant_id   varchar(20) DEFAULT '000000',
    create_dept int8        DEFAULT NULL,
    create_by   int8        DEFAULT NULL,
    create_time timestamp   DEFAULT NULL,
    update_by   int8        DEFAULT NULL,
    update_time timestamp   DEFAULT NULL,
    remark      varchar(500) DEFAULT NULL,
    CONSTRAINT pk_app_access_scope PRIMARY KEY (scope_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_access_scope_target
    ON app_access_scope (tenant_id, app_id, target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_app_access_scope_app
    ON app_access_scope (tenant_id, app_id);

DO $$
DECLARE
    v_app_id int8;
    v_scope_id int8;
BEGIN
    SELECT app_id
      INTO v_app_id
      FROM app_application
     WHERE tenant_id = '000000'
       AND app_code = 'required-knowledge'
       AND del_flag = '0'
     ORDER BY app_id
     LIMIT 1;

    IF v_app_id IS NOT NULL THEN
        UPDATE app_application
           SET access_mode = 'user',
               update_time = now()
         WHERE app_id = v_app_id
           AND NOT EXISTS (
               SELECT 1
                 FROM app_access_scope
                WHERE app_id = v_app_id
           );

        IF NOT EXISTS (
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
    END IF;
END $$;
