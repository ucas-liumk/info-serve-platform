alter table sys_user
    add column if not exists force_password_change char default '0'::bpchar;

comment on column sys_user.force_password_change is '是否需要强制修改密码（0否 1是）';

update sys_user
set force_password_change = '0'
where force_password_change is null;
