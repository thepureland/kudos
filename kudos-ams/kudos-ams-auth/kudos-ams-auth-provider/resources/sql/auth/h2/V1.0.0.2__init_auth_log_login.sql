--region DDL
create table if not exists "auth_log_login"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "user_id"     character varying(36),
    "username"    character varying(32)  not null,
    "tenant_id"   character varying(36)  not null,
    "login_time"  timestamp(6)           not null,
    "login_ip"    bigint,
    "login_location" character varying(128),
    "login_device"   character varying(64),
    "login_browser"  character varying(64),
    "login_os"       character varying(64),
    "user_agent"     character varying(512),
    "login_success"   boolean default TRUE not null,
    "failure_reason" character varying(256),
    "session_id"     character varying(64),
    "remark"         character varying(256),
    "create_time"    timestamp(6)
    );

-- 创建索引以提升查询性能
create index if not exists "idx_auth_log_login_user_id" on "auth_log_login" ("user_id");
create index if not exists "idx_auth_log_login_tenant_id" on "auth_log_login" ("tenant_id");
create index if not exists "idx_auth_log_login_login_time" on "auth_log_login" ("login_time");
create index if not exists "idx_auth_log_login_username" on "auth_log_login" ("username");

comment on table "auth_log_login" is '登录日志';
comment on column "auth_log_login"."id" is '主键';
comment on column "auth_log_login"."user_id" is '用户ID';
comment on column "auth_log_login"."username" is '用户名';
comment on column "auth_log_login"."tenant_id" is '租户ID';
comment on column "auth_log_login"."login_time" is '登录时间';
comment on column "auth_log_login"."login_ip" is '登录IP';
comment on column "auth_log_login"."login_location" is '登录地点';
comment on column "auth_log_login"."login_device" is '登录设备';
comment on column "auth_log_login"."login_browser" is '浏览器';
comment on column "auth_log_login"."login_os" is '操作系统';
comment on column "auth_log_login"."user_agent" is '用户代理字符串';
comment on column "auth_log_login"."login_success" is '是否登录成功';
comment on column "auth_log_login"."failure_reason" is '失败原因';
comment on column "auth_log_login"."session_id" is '会话ID';
comment on column "auth_log_login"."remark" is '备注';
comment on column "auth_log_login"."create_time" is '创建时间';

--endregion DDL


--region DML

--endregion DML
