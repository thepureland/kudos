--region DDL
create table if not exists "auth_group_role"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "group_id"        character(36)  not null,
    "role_id" character(36) not null,
    "granted_by" character varying(36),
    "parent_grant_id" character varying(36),
    "revoked" boolean default FALSE not null,
    "revoke_reason" character varying(256),
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_group_role" unique ("group_id", "role_id")
    );

comment on table "auth_group_role" is '组-角色';
comment on column "auth_group_role"."id" is '主键';
comment on column "auth_group_role"."group_id" is '组ID';
comment on column "auth_group_role"."role_id" is '角色ID';
-- Binding a role to a group is a BROADCAST grant: it hands the role to every current member at
-- once. That makes it the most dangerous write in the whole module, and the one that most needs to
-- be attributable and revocable — hence the same governance columns as the other two relations.
comment on column "auth_group_role"."granted_by" is '操作人ID（权限语义，区别于审计字段create_user_id）';
comment on column "auth_group_role"."parent_grant_id" is '上游授权ID，构成授权链；NULL=平台管理员直接绑定';
comment on column "auth_group_role"."revoked" is '是否已撤销（软失效，保留授权链与审计）';
comment on column "auth_group_role"."revoke_reason" is '撤销原因';
comment on column "auth_group_role"."create_user_id" is '创建者ID';
comment on column "auth_group_role"."create_user_name" is '创建者名称';
comment on column "auth_group_role"."create_time" is '创建时间';
comment on column "auth_group_role"."update_user_id" is '更新者ID';
comment on column "auth_group_role"."update_user_name" is '更新者名称';
comment on column "auth_group_role"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML