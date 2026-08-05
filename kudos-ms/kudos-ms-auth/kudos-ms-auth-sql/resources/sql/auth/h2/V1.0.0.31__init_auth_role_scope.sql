--region DDL
-- Role data-scope grants: the explicit values a role may reach when its data_scope = 'CUSTOM'.
-- Rows are ignored for any other scope.
--
-- **Why a dimension column.** Organization is the most common way to slice rows, but it is not the
-- only one — region, brand, project and business line all appear, and an application needing one of
-- those should not have to fork the authorization model to get it. The dimension is therefore data,
-- not a table name. Applications register a dimension by contributing an `IScopeDimensionProvider`,
-- which is what teaches the resolver how (or whether) values in that dimension nest.
--
-- No FK on scope_value by design — the values live in other microservices (orgs in kudos-ms-user),
-- and a value deleted out from under a grant simply drops out of the resolved set, which is
-- recoverable; a hard FK across the MS boundary would couple the two schemas.
create table if not exists "auth_role_scope"
(
    "id"               character(36)         default RANDOM_UUID() not null primary key,
    "role_id"          character varying(36) not null,
    "dimension"        character varying(32) default 'org' not null,
    "scope_value"      character varying(64) not null,
    "create_user_id"   character varying(36),
    "create_user_name" character varying(32),
    "create_time"      timestamp(6),
    "update_user_id"   character varying(36),
    "update_user_name" character varying(32),
    "update_time"      timestamp(6),
    constraint "uk_auth_role_scope" unique ("role_id", "dimension", "scope_value")
);

comment on table  "auth_role_scope" is '角色自定义数据范围';
comment on column "auth_role_scope"."id"          is '主键';
comment on column "auth_role_scope"."role_id"     is '角色ID';
comment on column "auth_role_scope"."dimension"   is '范围维度：org / region / project …，由应用注册';
comment on column "auth_role_scope"."scope_value" is '该维度上的取值（如机构ID）';

-- Resolution reads a role's grants by (role, dimension); the reverse lookup supports impact analysis
-- when a value disappears from the dimension it belongs to.
create index if not exists "idx_auth_role_scope_role" on "auth_role_scope" ("role_id", "dimension");
create index if not exists "idx_auth_role_scope_value" on "auth_role_scope" ("dimension", "scope_value");
--endregion DDL


--region DML
-- No seed data: scope grants are purely admin-configured.
--endregion DML
