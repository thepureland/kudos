--region DDL
-- Approval flow: when approval_required is true, assigning this role to a user should go through
-- a grant-request → approve cycle (see auth_role_grant_request) rather than an immediate bind.
--
-- This column is a *marker only*. The backend ships the request/approve mechanism but does NOT
-- force the standard batchBind path to reject approval-required roles — whether to enforce that
-- (vs. leaving direct assignment open for super-admins) is a deployment policy decision, made at
-- the gateway / calling layer. Default false keeps every existing role on the immediate-bind path.
alter table "auth_role"
    add column if not exists "approval_required" boolean default false not null;

comment on column "auth_role"."approval_required" is '分配该角色是否需要审批';
--endregion DDL


--region DML
-- No backfill: existing roles keep approval_required = false (immediate bind).
--endregion DML
