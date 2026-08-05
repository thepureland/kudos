--region DDL
-- Instance-level grants: "share *this* document with *that* person".
--
-- **Why this is a separate table rather than more roles.** RBAC answers "may this kind of person do
-- this kind of thing"; it has no honest way to say "may this person touch row #4711". Encoding an
-- instance in a role means a role per instance, which turns the role table into a copy of the data
-- table and every share into schema churn. The point-to-point case is a different question and gets
-- its own answer, evaluated alongside role-derived grants in the same decision algebra.
--
-- Scope stays deliberately narrow: one (principal, resource type, instance, action) tuple with an
-- effect and an optional validity window. It is not a relationship graph — see the module README's
-- non-goals on ReBAC: transitive "members of the team that owns the folder" reasoning is a
-- different system with a different cost profile.
create table if not exists "auth_instance_grant"
(
    "id"               character(36)          default RANDOM_UUID() not null primary key,
    "principal_id"     character varying(36)  not null,
    "principal_type"   character varying(16)  default 'USER' not null,
    "tenant_id"        character varying(36)  not null,
    "resource_type"    character varying(64)  not null,
    "instance_id"      character varying(64)  not null,
    "action"           character varying(32)  not null,
    "effect"           character varying(8)   default 'ALLOW' not null,
    "start_time"       timestamp(6),
    "end_time"         timestamp(6),
    "granted_by"       character varying(36),
    "revoked"          boolean default FALSE  not null,
    "revoke_reason"    character varying(256),
    "create_user_id"   character varying(36),
    "create_user_name" character varying(32),
    "create_time"      timestamp(6),
    "update_user_id"   character varying(36),
    "update_user_name" character varying(32),
    "update_time"      timestamp(6),
    constraint "uk_auth_instance_grant" unique ("principal_id", "resource_type", "instance_id", "action")
);

comment on table  "auth_instance_grant" is '实例级授权（点对点分享）';
comment on column "auth_instance_grant"."id"             is '主键';
comment on column "auth_instance_grant"."principal_id"   is '被授权主体ID';
comment on column "auth_instance_grant"."principal_type" is '主体类型：USER/SERVICE/API_KEY';
comment on column "auth_instance_grant"."tenant_id"      is '租户ID';
comment on column "auth_instance_grant"."resource_type"  is '资源类型，如 doc / order';
comment on column "auth_instance_grant"."instance_id"    is '资源实例ID';
comment on column "auth_instance_grant"."action"         is '动作，如 read / update';
comment on column "auth_instance_grant"."effect"         is '授权效果：ALLOW/DENY，DENY优先';
comment on column "auth_instance_grant"."start_time"     is '生效时间，NULL 表示立即生效';
comment on column "auth_instance_grant"."end_time"       is '失效时间，NULL 表示永不失效';
comment on column "auth_instance_grant"."granted_by"     is '授权人ID';
comment on column "auth_instance_grant"."revoked"        is '是否已撤销（软失效，保留审计）';

-- The decision path reads by (principal, resource type, instance); the reverse lookup answers
-- "who has this document been shared with", which every sharing UI needs.
create index if not exists "idx_auth_instance_grant_principal" on "auth_instance_grant" ("principal_id", "resource_type");
create index if not exists "idx_auth_instance_grant_instance" on "auth_instance_grant" ("resource_type", "instance_id");
--endregion DDL


--region DML
-- No seed data.
--endregion DML
