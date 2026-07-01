-- info-serve v0.3.3 portal notification update
-- Run against PostgreSQL database: ry-cloud

COMMENT ON TABLE app_message IS '门户通知消息';

INSERT INTO app_message (message_id, user_id, title, content, msg_type, is_read, create_time)
SELECT
    2073003000000000000 + user_id,
    user_id,
    '版本升级：v0.3.3 门户通知上线',
    'v0.3.3 新增门户右上角统一通知，支持版本升级、资源新增、应用上架、论坛反馈等消息提醒。',
    'version',
    '0',
    now()
FROM sys_user
WHERE status = '0' AND del_flag = '0'
ON CONFLICT (message_id) DO NOTHING;
