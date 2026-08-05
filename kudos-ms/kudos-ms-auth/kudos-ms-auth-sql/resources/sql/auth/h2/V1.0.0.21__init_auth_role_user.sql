--region DDL
create table if not exists "auth_role_user"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "role_id"        character(36)  not null,
    "user_id" character(36) not null,
    "principal_type" character varying(16) default 'USER' not null,
    "granted_by" character varying(36),
    "parent_grant_id" character varying(36),
    "delegable_depth" int default 0 not null,
    "scope_snapshot" character varying(1024),
    "revoked" boolean default FALSE not null,
    "revoke_reason" character varying(256),
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_role_user" unique ("role_id", "user_id")
    );

comment on table "auth_role_user" is '角色-用户';
comment on column "auth_role_user"."id" is '主键';
comment on column "auth_role_user"."role_id" is '角色id';
comment on column "auth_role_user"."user_id" is '用户id';
-- A row here is ONE GRANT, and the columns below describe that grant rather than the role:
--   principal_type   the subject kind — human account, service account, api key … Machine
--                    principals go through the same authorization model as people.
--   granted_by       who exercised their authority to create it. Distinct from create_user_id,
--                    which is an audit stamp: this one carries permission semantics and is what
--                    the delegation chain is built from.
--   parent_grant_id  the upstream grant this one was delegated from; NULL for a direct grant by a
--                    platform administrator. Revocation cascades down this edge.
--   delegable_depth  how many further hops this grant may be passed on; 0 = the holder may not
--                    re-delegate. Must be <= auth_role.delegable_max and strictly less than the
--                    granting row's own depth (monotonic narrowing).
--   scope_snapshot   the scope frozen at grant time, so a grantor later changing org/position
--                    cannot silently widen what they already handed out.
--   revoked          soft revocation. Rows are never physically deleted on revoke: the chain and
--                    the audit trail must survive.
comment on column "auth_role_user"."principal_type" is '主体类型：USER/SERVICE/API_KEY';
comment on column "auth_role_user"."granted_by" is '授权人ID（权限语义，区别于审计字段create_user_id）';
comment on column "auth_role_user"."parent_grant_id" is '上游授权ID，构成授权链；NULL=平台管理员直授';
comment on column "auth_role_user"."delegable_depth" is '本条授权还可再往下转授的层数，0=不可转授';
comment on column "auth_role_user"."scope_snapshot" is '授权时冻结的范围快照（JSON）';
comment on column "auth_role_user"."revoked" is '是否已撤销（软失效，保留授权链与审计）';
comment on column "auth_role_user"."revoke_reason" is '撤销原因';
comment on column "auth_role_user"."create_user_id" is '创建者id';
comment on column "auth_role_user"."create_user_name" is '创建者名称';
comment on column "auth_role_user"."create_time" is '创建时间';
comment on column "auth_role_user"."update_user_id" is '更新者id';
comment on column "auth_role_user"."update_user_name" is '更新者名称';
comment on column "auth_role_user"."update_time" is '更新时间';

-- Cascade revocation walks the chain downwards from a grant id; index the edge it follows.
create index if not exists "idx_auth_role_user_parent_grant" on "auth_role_user" ("parent_grant_id");

--endregion DDL


--region DML

--endregion DML