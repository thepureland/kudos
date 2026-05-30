--region DDL
-- Separation of Duties (SoD): a pair of mutually exclusive roles within a tenant.
-- No user may hold both role_a and role_b simultaneously (via any path: direct assignment,
-- group membership, or parent-chain inheritance).
--
-- Canonical ordering: role_a_id < role_b_id (string comparison). The service layer enforces
-- this so there is never a duplicate pair (A,B) and (B,A) in the table.
--
-- Scope: tenant-scoped. Cross-tenant pairs are meaningless because role IDs are already
-- globally unique, but restricting to the tenant makes the query predicate tighter and
-- lets the unique constraint be naturally per-tenant.
create table if not exists "auth_role_exclusion"
(
    "id"              character(36)          default RANDOM_UUID() not null primary key,
    "role_a_id"       character varying(36)  not null,
    "role_b_id"       character varying(36)  not null,
    "tenant_id"       character varying(36)  not null,
    "description"     character varying(256),
    "create_user_id"  character varying(36),
    "create_user_name" character varying(32),
    "create_time"     timestamp(6),
    "update_user_id"  character varying(36),
    "update_user_name" character varying(32),
    "update_time"     timestamp(6),
    constraint "uk_auth_role_exclusion" unique ("role_a_id", "role_b_id", "tenant_id"),
    -- Belt-and-braces: prevent self-exclusion rows from ever landing in DB.
    constraint "chk_auth_role_exclusion_no_self" check ("role_a_id" <> "role_b_id")
);

comment on table  "auth_role_exclusion" is 'SoD 互斥角色对';
comment on column "auth_role_exclusion"."id"          is '主键';
comment on column "auth_role_exclusion"."role_a_id"   is '角色A的ID（规范化为 < role_b_id）';
comment on column "auth_role_exclusion"."role_b_id"   is '角色B的ID（规范化为 > role_a_id）';
comment on column "auth_role_exclusion"."tenant_id"   is '租户ID';
comment on column "auth_role_exclusion"."description" is '规则描述';

-- Lookup index for the validation query (given a set of role IDs, find all exclusion pairs).
create index if not exists "idx_auth_role_exclusion_a" on "auth_role_exclusion" ("role_a_id");
create index if not exists "idx_auth_role_exclusion_b" on "auth_role_exclusion" ("role_b_id");
create index if not exists "idx_auth_role_exclusion_tenant" on "auth_role_exclusion" ("tenant_id");
--endregion DDL


--region DML
-- No seed data: exclusion rules are purely admin-configured.
--endregion DML
