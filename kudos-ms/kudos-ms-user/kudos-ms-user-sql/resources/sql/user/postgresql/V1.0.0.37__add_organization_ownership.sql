--region DDL
-- Organization mode (kudos.ms.organization.enabled). Purely additive: legacy rows keep a null
-- organization and their tenant-scoped uniqueness is unchanged. No existing data is converted (G-13).

-- user_org: a customer organization is a root node (node_kind=ORGANIZATION, organization_id=id,
-- no parent); its departments carry organization_id=root id. Legacy nodes leave both columns null.
alter table "user_org" add column "organization_id" character varying(36);
alter table "user_org" add column "node_kind" character varying(16);
alter table "user_org" add constraint "ck_user_org_node_kind" check (
    ("node_kind" is null and "organization_id" is null) or
    ("node_kind" = 'ORGANIZATION' and "organization_id" = "id" and "parent_id" is null) or
    ("node_kind" = 'DEPARTMENT' and "organization_id" is not null and "parent_id" is not null)
);

-- user_account: an organization account is shared by every tenant of its organization.
alter table "user_account" add column "organization_id" character varying(36);

-- Owner key: TENANT:<tenant> for legacy rows (exactly the old uniqueness), ORG:<organization> for
-- organization rows (whose tenant_id is empty).
alter table "user_org" add column "owner_key" character varying(80) generated always as (
    case when "organization_id" is null then 'TENANT:' || "tenant_id" else 'ORG:' || "organization_id" end
) stored;
alter table "user_account" add column "owner_key" character varying(80) generated always as (
    case when "organization_id" is null then 'TENANT:' || "tenant_id" else 'ORG:' || "organization_id" end
) stored;
alter table "user_org" drop constraint "uk_user_org_tenant_name";
alter table "user_org" add constraint "uk_user_org_owner_name" unique ("owner_key", "name");
alter table "user_account" drop constraint "uk_user_account_tenant_username";
alter table "user_account" add constraint "uk_user_account_owner_username" unique ("owner_key", "username");
create index "idx_user_org_organization" on "user_org" ("organization_id", "parent_id");
create index "idx_user_account_organization" on "user_account" ("organization_id", "username");
-- The dropped tenant unique keys were the only indexes on the legacy lookups; keep them indexed.
create index "idx_user_account_tenant_username" on "user_account" ("tenant_id", "username");
create index "idx_user_org_tenant_parent" on "user_org" ("tenant_id", "parent_id");

comment on column "user_org"."organization_id" is '所属客户组织（根节点id）；旧模式为空';
comment on column "user_org"."node_kind" is '节点类型：ORGANIZATION 客户组织根，DEPARTMENT 部门；旧模式为空';
comment on column "user_org"."owner_key" is '归属键（生成列）';
comment on column "user_account"."organization_id" is '所属客户组织；旧模式为空';
comment on column "user_account"."owner_key" is '归属键（生成列）';
--endregion DDL
