--region DDL
create table if not exists "auth_user"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "username"        character varying(32)  not null,
    "display_name"     character varying(64),
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
    "supervisor_id" character varying(36)  not null,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_user_tenant_username" unique ("tenant_id", "username")
    );

comment on table "auth_user" is '用户基本信息';
comment on column "auth_user"."id" is '主键';
comment on column "auth_user"."username" is '用户名';
comment on column "auth_user"."display_name" is '展示名称';
comment on column "auth_user"."tenant_id" is '租户ID';
comment on column "auth_user"."login_password" is '登录密码';
comment on column "auth_user"."security_password" is '安全密码 - 二次验证密码，可选，用于敏感操作验证';
comment on column "auth_user"."user_type_dict_code" is '用户类型字典码';
comment on column "auth_user"."user_status_dict_code" is '用户状态字典码';
comment on column "auth_user"."default_locale" is '默认语言环境 - ISO 639-1格式，如：zh_CN、en_US';
comment on column "auth_user"."default_timezone" is '默认时区 - 如：Asia/Shanghai、UTC';
comment on column "auth_user"."default_currency" is '默认货币 - ISO 4217格式，如：CNY、USD';
comment on column "auth_user"."last_login_time" is '最后登录时间';
comment on column "auth_user"."last_login_ip" is '最后登录IP';
comment on column "auth_user"."last_logout_time" is '最后登出时间';
comment on column "auth_user"."login_error_times" is '登录错误次数';
comment on column "auth_user"."security_password_error_times" is '安全密码错误次数';
comment on column "auth_user"."session_key" is '会话密钥';
comment on column "auth_user"."authentication_key" is '认证密钥';
comment on column "auth_user"."dept_id" is '所属部门ID';
comment on column "auth_user"."supervisor_id" is '直属上级ID';
comment on column "auth_user"."remark" is '备注';
comment on column "auth_user"."active" is '是否激活';
comment on column "auth_user"."built_in" is '是否内置';
comment on column "auth_user"."create_user_id" is '创建者id';
comment on column "auth_user"."create_user_name" is '创建者名称';
comment on column "auth_user"."create_time" is '创建时间';
comment on column "auth_user"."update_user_id" is '更新者id';
comment on column "auth_user"."update_user_name" is '更新者名称';
comment on column "auth_user"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML