--region DDL
create table if not exists "auth_role_resource"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "role_id"        character(36)  not null,
    "resource_id" character(36),
    "permission_code" character varying(128),
    "effect" character varying(8) default 'ALLOW' not null,
    "condition" character varying(1024),
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_role_resource" unique ("role_id", "resource_id")
    );

comment on table "auth_role_resource" is '角色-权限';
comment on column "auth_role_resource"."id" is '主键';
comment on column "auth_role_resource"."role_id" is '角色id';
-- A grant addresses a permission in one of two ways:
--   resource_id      the legacy handle — a sys_resource primary key. Environment-specific: the same
--                    logical permission has a different id in dev and in prod, and rebuilding the
--                    resource row silently drops every grant pointing at it.
--   permission_code  the durable handle — a semantic code such as `sys:user:delete`, with `*`
--                    wildcards (`sys:user:*`, `sys:*`). Stable across environments, expresses
--                    "the whole module" without enumerating rows, and can name things that are not
--                    UI tree nodes at all (scheduled jobs, message topics, plain APIs).
-- Both are nullable so a binding may use either; the code is the direction of travel.
--
--   effect     ALLOW | DENY. DENY wins over any ALLOW and cannot be overridden by a more specific
--              rule — a deliberate trade of expressiveness for explainability: "why was I denied"
--              always has exactly one answer.
--   condition  optional restricted expression (IP range, time window, custom key/values) evaluated
--              by a pluggable evaluator. Covers the common environmental slice of ABAC without
--              dragging in a policy engine.
comment on column "auth_role_resource"."resource_id" is '资源id（与permission_code二选一）';
comment on column "auth_role_resource"."permission_code" is '权限编码，如 sys:user:delete，支持*通配';
comment on column "auth_role_resource"."effect" is '授权效果：ALLOW/DENY，DENY优先且不可被覆盖';
comment on column "auth_role_resource"."condition" is '生效条件表达式（IP/时段/自定义键值），NULL=无条件';
comment on column "auth_role_resource"."create_user_id" is '创建者id';
comment on column "auth_role_resource"."create_user_name" is '创建者名称';
comment on column "auth_role_resource"."create_time" is '创建时间';
comment on column "auth_role_resource"."update_user_id" is '更新者id';
comment on column "auth_role_resource"."update_user_name" is '更新者名称';
comment on column "auth_role_resource"."update_time" is '更新时间';

-- Code-addressed bindings need their own uniqueness; NULLs compare as distinct, so rows that use
-- only resource_id do not collide here (and vice versa for uk_auth_role_resource).
create unique index if not exists "uk_auth_role_resource_code" on "auth_role_resource" ("role_id", "permission_code");

--endregion DDL


--region DML

--endregion DML
