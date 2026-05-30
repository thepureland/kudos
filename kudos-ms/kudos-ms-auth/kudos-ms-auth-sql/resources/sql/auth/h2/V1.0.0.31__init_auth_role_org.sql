--region DDL
-- Custom data-scope org grants: the explicit list of organizations a role may access when its
-- data_scope = 'CUSTOM'. Rows are ignored for any other scope. No FK to user_org by design —
-- orgs live in a different microservice (kudos-ms-user) and an org deleted out from under a
-- grant simply drops out of the resolved set (the resolver reads live), which is recoverable;
-- a hard FK across the MS boundary would couple the two schemas.
create table if not exists "auth_role_org"
(
    "id"               character(36)         default RANDOM_UUID() not null primary key,
    "role_id"          character varying(36) not null,
    "org_id"           character varying(36) not null,
    "create_user_id"   character varying(36),
    "create_user_name" character varying(32),
    "create_time"      timestamp(6),
    "update_user_id"   character varying(36),
    "update_user_name" character varying(32),
    "update_time"      timestamp(6),
    constraint "uk_auth_role_org" unique ("role_id", "org_id")
);

comment on table  "auth_role_org" is '角色自定义数据权限机构';
comment on column "auth_role_org"."id"      is '主键';
comment on column "auth_role_org"."role_id" is '角色ID';
comment on column "auth_role_org"."org_id"  is '机构ID';

-- Resolution reads custom grants by role id; reverse lookup (which roles grant an org) supports
-- impact analysis when an org is removed.
create index if not exists "idx_auth_role_org_role" on "auth_role_org" ("role_id");
create index if not exists "idx_auth_role_org_org" on "auth_role_org" ("org_id");
--endregion DDL


--region DML
-- No seed data: custom org grants are purely admin-configured.
--endregion DML
