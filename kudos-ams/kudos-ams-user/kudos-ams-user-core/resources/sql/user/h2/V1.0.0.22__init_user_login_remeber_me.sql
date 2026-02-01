--region DDL
create table if not exists "user_login_remember_me"
(
    "id"        CHAR(36)    not null primary key,
    "user_id"   CHAR(36)    not null,
    "username"  VARCHAR(32) not null,
    "tenant_id" CHAR(36) not null,
    "token"     VARCHAR(64) not null,
    "last_used" TIMESTAMP(6) not null,
    constraint "user_login_remember_me"
    foreign key ("user_id") references "user_account" ("id")
    );

create unique index "uq_user_login_remember_me_username_tenant_id"
    on "user_login_remember_me" ("username", "tenant_id");


comment on table "user_login_remember_me" is '登陆持久化';
comment on column "user_login_remember_me"."id" is '主键，series，登陆令牌散列，仅在用户使用密码重新登录时创建';
comment on column "user_login_remember_me"."user_id" is '用户ID';
comment on column "user_login_remember_me"."username" is '用户名';
comment on column "user_login_remember_me"."tenant_id" is '租户id';
comment on column "user_login_remember_me"."token" is '自动登陆会话令牌，会在每一个新的session中都重新生成';
comment on column "user_login_remember_me"."last_used" is '最后一次使用时间';
--endregion DDL


--region DML

--endregion DML
