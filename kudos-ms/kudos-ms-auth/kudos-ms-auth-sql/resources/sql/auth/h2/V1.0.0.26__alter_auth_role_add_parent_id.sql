--region DDL
-- Role inheritance: parent_id is nullable; NULL = root role (no parent).
-- A foreign key is intentionally omitted — the service layer gates parent-id mutations
-- (cycle + same tenant + same subsystem checks), and reading an orphaned subtree is a
-- recoverable condition (children behave as if root). Skipping the FK keeps the migration
-- safe for legacy tenants whose auth_role rows may otherwise violate it.
alter table "auth_role"
    add column if not exists "parent_id" character varying(36) null;

-- Index for the ancestor walk performed in ResourceIdsByUserIdCache. Without it, every
-- permission check on a user with N roles would do N parent lookups via full scan.
create index if not exists "idx_auth_role_parent_id" on "auth_role" ("parent_id");

comment on column "auth_role"."parent_id" is '父角色ID，NULL 表示根角色';
--endregion DDL


--region DML
-- No backfill: existing rows keep parent_id = NULL, behaving as roots.
--endregion DML
