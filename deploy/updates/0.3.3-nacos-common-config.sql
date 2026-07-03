-- info-serve v0.3.3 Nacos common config fixes
-- Run against MySQL database: ry-config.

UPDATE `ry-config`.config_info
SET content = REPLACE(
        REPLACE(content, 'max-file-size: 10MB', 'max-file-size: 200MB'),
        'max-request-size: 20MB',
        'max-request-size: 200MB'
    ),
    gmt_modified = NOW()
WHERE data_id = 'application-common.yml'
  AND group_id = 'DEFAULT_GROUP';

UPDATE `ry-config`.config_info
SET content = REPLACE(
        content,
        '    - info_forum_like\n',
        '    - info_forum_like\n    - info_resource_favorite\n'
    ),
    gmt_modified = NOW()
WHERE data_id = 'application-common.yml'
  AND group_id = 'DEFAULT_GROUP'
  AND content NOT LIKE '%info_resource_favorite%';

UPDATE `ry-config`.config_info
SET md5 = MD5(content),
    gmt_modified = NOW()
WHERE data_id = 'application-common.yml'
  AND group_id = 'DEFAULT_GROUP';
