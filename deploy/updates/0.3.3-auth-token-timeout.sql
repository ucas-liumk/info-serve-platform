-- info-serve v0.3.3 token timeout fix
-- Run against PostgreSQL database: ry-cloud.

UPDATE sys_client
SET active_timeout = 86400,
    timeout = 86400,
    update_time = now()
WHERE client_id IN (
    'e5cd7e4891bf95d1d19206ce24a7b32e',
    '428a8310cd442757ae699df5d894f051'
);
