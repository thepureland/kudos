--region DDL
create table if not exists "auth_user"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "username"        character varying(32)  not null,
    "tenant_id"        character varying(32)  not null,
    "login_password"        character varying(128) not null,
    "security_password" character varying(128),
    "user_type_dict_code" varchar(5),
    "user_status_dict_code" varchar(5),
    "default_locale" varchar(5),
    "default_timezone" varchar(16),
    "default_currency" varchar(3),
    "last_login_time" timestamp(6),
    "last_login_ip" bigint,
    "last_logout_time" timestamp(6),
    "login_error_times" int4,
    "security_password_error_times" int4,
    "session_key" varchar(40),
    "authentication_key" varchar(64),
    "dept_id" character varying(128),
    "supervisor_id" character varying(32)  not null,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user" character varying(36),
    "create_time" timestamp(6),
    "update_user" character varying(36),
    "update_time" timestamp(6)
    );

comment on table "sys_portal" is '门户';
comment on column "sys_portal"."code" is '编码';
comment on column "sys_portal"."name" is '名称';
comment on column "sys_portal"."remark" is '备注';
comment on column "sys_portal"."active" is '是否启用';
comment on column "sys_portal"."built_in" is '是否内置';
comment on column "sys_portal"."create_user" is '创建用户';
comment on column "sys_portal"."create_time" is '创建时间';
comment on column "sys_portal"."update_user" is '更新用户';
comment on column "sys_portal"."update_time" is '更新时间';
--endregion DDL


--region DML

--endregion DML