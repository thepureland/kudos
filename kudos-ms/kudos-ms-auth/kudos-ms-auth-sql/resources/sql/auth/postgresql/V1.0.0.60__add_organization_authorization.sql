--region DDL
-- Organization mode (kudos.ms.organization.enabled). Purely additive; legacy tenants are untouched.
--
-- Organization-owned roles, groups and SoD rules keep their owner in tenant_id, which for them holds
-- the organization id (UUIDs never collide with tenant ids), so the existing owner-scoped uniqueness
-- and same-owner checks apply unchanged. organization_id marks such rows explicitly.
alter table "auth_role" add column "organization_id" character varying(36);
alter table "auth_group" add column "organization_id" character varying(36);
alter table "auth_role_exclusion" add column "organization_id" character varying(36);
create index "idx_auth_role_organization" on "auth_role" ("organization_id");
create index "idx_auth_group_organization" on "auth_group" ("organization_id");

comment on column "auth_role"."organization_id" is '所属客户组织（组织角色的 tenant_id 同为该组织id）；旧模式为空';
comment on column "auth_group"."organization_id" is '所属客户组织；旧模式为空';
comment on column "auth_role_exclusion"."organization_id" is '所属客户组织；旧模式为空';

-- G-1/G-3: a member's per-tenant difference from their organization default roles.
create table "auth_tenant_role_override"
(
    "id"              character(36)          default gen_random_uuid()::text not null primary key,
    "organization_id" character varying(36)  not null,
    "tenant_id"       character varying(36)  not null,
    "user_id"         character varying(36)  not null,
    "role_id"         character varying(36)  not null,
    "action"          character varying(8)   not null,
    "reason"          character varying(512),
    "create_user_id"  character varying(36),
    "create_time"     timestamp(6),
    constraint "uk_auth_tenant_role_override" unique ("tenant_id", "user_id", "role_id"),
    constraint "ck_auth_tenant_role_override_action" check ("action" in ('ADD', 'REMOVE'))
);
create index "idx_auth_tenant_role_override_user" on "auth_tenant_role_override" ("organization_id", "user_id");

comment on table "auth_tenant_role_override" is '成员在某 tenant 相对组织默认角色的差异覆盖';
comment on column "auth_tenant_role_override"."action" is 'ADD 本站加入，REMOVE 本站移除';

-- G-6/G-10: organization administrators (a designation, not a role).
create table "auth_organization_admin"
(
    "id"              character(36)          default gen_random_uuid()::text not null primary key,
    "organization_id" character varying(36)  not null,
    "user_id"         character varying(36)  not null,
    "create_user_id"  character varying(36),
    "create_time"     timestamp(6),
    constraint "uk_auth_organization_admin" unique ("organization_id", "user_id")
);

comment on table "auth_organization_admin" is '组织管理员指定';

-- G-6/G-7/G-11: built-in management roles. TENANT_PERMISSION_ADMIN rows name one governed tenant
-- each; ORGANIZATION_PERMISSION_ADMIN rows use an empty tenant_id.
create table "auth_management_role_grant"
(
    "id"              character(36)          default gen_random_uuid()::text not null primary key,
    "organization_id" character varying(36)  not null,
    "user_id"         character varying(36)  not null,
    "role_kind"       character varying(32)  not null,
    "tenant_id"       character varying(36)  default '' not null,
    "create_user_id"  character varying(36),
    "create_time"     timestamp(6),
    constraint "uk_auth_management_role_grant" unique ("organization_id", "user_id", "role_kind", "tenant_id"),
    constraint "ck_auth_management_role_grant_kind" check (
        ("role_kind" = 'ORGANIZATION_PERMISSION_ADMIN' and "tenant_id" = '') or
        ("role_kind" = 'TENANT_PERMISSION_ADMIN' and "tenant_id" <> '')
    )
);

comment on table "auth_management_role_grant" is '内置管理角色授予：组织权限管理员、tenant 权限管理员（每行一个管辖 tenant）';

-- One monotonic revision per organization: every change to its authorization facts bumps it in the
-- same transaction, and writers lock this row to serialize concurrent changes.
create table "auth_organization_revision"
(
    "organization_id" character varying(36)  not null primary key,
    "revision"        bigint default 0       not null
);

comment on table "auth_organization_revision" is '组织授权修订号';

create table "auth_organization_audit"
(
    "id"              character(36)          default gen_random_uuid()::text not null primary key,
    "organization_id" character varying(36)  not null,
    "actor_id"        character varying(36),
    "action"          character varying(64)  not null,
    "tenant_id"       character varying(36),
    "target_user_id"  character varying(36),
    "target_id"       character varying(36),
    "detail"          character varying(4000),
    "reason"          character varying(512),
    "create_time"     timestamp(6)           not null
);
create index "idx_auth_organization_audit" on "auth_organization_audit" ("organization_id", "create_time");

comment on table "auth_organization_audit" is '组织授权异动审计';
--endregion DDL
