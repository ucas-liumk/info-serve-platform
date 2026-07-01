-- info-serve v0.3.3 resource sharing fixes
-- Run against PostgreSQL database: ry-cloud.
-- The config_info block is safe to run only where the Nacos config table exists.

CREATE TABLE IF NOT EXISTS info_resource_favorite (
    id          int8      NOT NULL,
    resource_id int8     NOT NULL,
    user_id     int8     NOT NULL,
    create_time timestamp DEFAULT NULL,
    CONSTRAINT pk_info_resource_favorite PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_info_resource_fav_user_resource ON info_resource_favorite (user_id, resource_id);
CREATE INDEX IF NOT EXISTS idx_info_resource_fav_resource ON info_resource_favorite (resource_id);
COMMENT ON TABLE info_resource_favorite IS '淇℃伅涓績璧勬枡鏀惰棌';

DO $$
BEGIN
    IF to_regclass('public.config_info') IS NOT NULL THEN
        UPDATE config_info
        SET content = replace(replace(content, 'max-file-size: 10MB', 'max-file-size: 200MB'), 'max-request-size: 20MB', 'max-request-size: 200MB'),
            gmt_modified = now()
        WHERE data_id = 'application-common.yml'
          AND group_id = 'DEFAULT_GROUP'
          AND content LIKE '%max-file-size: 10MB%';
    END IF;
END $$;

