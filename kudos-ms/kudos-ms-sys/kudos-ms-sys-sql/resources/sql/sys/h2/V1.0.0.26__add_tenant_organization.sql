--region DDL
-- Organization mode (kudos.ms.organization.enabled). Purely additive: existing tenants keep a null
-- organization and behave exactly as before. Organization mode is first-version greenfield only (G-13).
alter table "sys_tenant" add column "organization_id" character varying(36);
alter table "sys_tenant" add column "organization_open" boolean default FALSE not null;
create index "idx_sys_tenant_organization" on "sys_tenant" ("organization_id");

comment on column "sys_tenant"."organization_id" is '所属客户组织（user_org 根节点）；平台 tenant 与旧模式为空';
comment on column "sys_tenant"."organization_open" is '是否已对组织成员开放';

-- Single-row marker written when organization mode starts successfully for the first time (G-18).
-- Once present, the deployment can no longer start with organization mode off (G-19).
create table "sys_organization_mode"
(
    "id"           character varying(36) not null primary key,
    "enabled_time" timestamp(6)          not null
);

comment on table "sys_organization_mode" is '组织模式启用标记';
comment on column "sys_organization_mode"."id" is '固定主键';
comment on column "sys_organization_mode"."enabled_time" is '首次启用时间';
--endregion DDL
