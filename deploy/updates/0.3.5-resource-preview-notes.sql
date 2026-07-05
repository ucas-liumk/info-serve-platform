-- info-serve v0.3.5 资料预览笔记与阅看记录
-- Run against PostgreSQL database: ry-cloud
-- 与 source/script/sql/postgres/postgres_info_service.sql 同步维护（种子管新装、本文件管存量）

CREATE TABLE IF NOT EXISTS info_resource_note (
    note_id     int8          NOT NULL,
    resource_id int8          NOT NULL,
    user_id     int8          NOT NULL,
    author_name varchar(80)   DEFAULT NULL,
    content     varchar(2000) NOT NULL,
    visibility  varchar(20)   DEFAULT 'private' NOT NULL,
    tenant_id   varchar(20)   DEFAULT '000000',
    del_flag    char(1)       DEFAULT '0',
    create_dept int8          DEFAULT NULL,
    create_by   int8          DEFAULT NULL,
    create_time timestamp     DEFAULT NULL,
    update_by   int8          DEFAULT NULL,
    update_time timestamp     DEFAULT NULL,
    remark      varchar(500)  DEFAULT NULL,
    CONSTRAINT pk_info_resource_note PRIMARY KEY (note_id)
);
CREATE INDEX IF NOT EXISTS idx_info_resource_note_user ON info_resource_note (resource_id, user_id, create_time);
CREATE INDEX IF NOT EXISTS idx_info_resource_note_public ON info_resource_note (resource_id, visibility, create_time) WHERE del_flag = '0';
COMMENT ON TABLE info_resource_note IS '信息中心资料阅读笔记';

CREATE TABLE IF NOT EXISTS info_resource_view_record (
    record_id   int8        NOT NULL,
    resource_id int8        NOT NULL,
    user_id     int8        NOT NULL,
    user_name   varchar(80) DEFAULT NULL,
    action_type varchar(30) DEFAULT 'view',
    tenant_id   varchar(20) DEFAULT '000000',
    create_dept int8        DEFAULT NULL,
    create_by   int8        DEFAULT NULL,
    create_time timestamp   DEFAULT NULL,
    update_by   int8        DEFAULT NULL,
    update_time timestamp   DEFAULT NULL,
    CONSTRAINT pk_info_resource_view_record PRIMARY KEY (record_id)
);
CREATE INDEX IF NOT EXISTS idx_info_resource_view_record_resource ON info_resource_view_record (resource_id, create_time);
CREATE INDEX IF NOT EXISTS idx_info_resource_view_record_user ON info_resource_view_record (user_id, create_time);
COMMENT ON TABLE info_resource_view_record IS '信息中心资料阅看记录';
