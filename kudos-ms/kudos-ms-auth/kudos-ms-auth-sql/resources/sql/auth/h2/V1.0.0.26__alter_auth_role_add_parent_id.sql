--region DDL
-- Role inheritance: parent_id is nullable; NULL = root role (no parent).
-- A foreign key would prevent breaking the hierarchy by orphan-deleting a parent,
-- but the service layer already gates parent-id mutations (cycle + same tenant + same
-- subsystem checks) and reading orphaned subtrees is a recoverable condition (children
-- still work as if root). Skipping the FK keeps migration safer for legacy tenants whose
-- auth_role tables may already have rows whose code/tenant_id would otherwise need to
-- be relaxed for the constraint to apply.
alter table "auth_role"
    add column if not exists "parent_id" character varying(36) null;

-- Index for the ancestor walk performed in ResourceIdsByUserIdCache. Without this, every
-- permission check on a user with N roles would do N parent lookups via full scan.
create index if not exists "idx_auth_role_parent_id" on "auth_role" ("parent_id");

comment on column "auth_role"."parent_id" is '父角色ID，NULL 表示根角色';
--endregion DDL


--region DML
-- No backfill: existing rows keep parent_id = NULL, behaving as roots.
--endregion DML
