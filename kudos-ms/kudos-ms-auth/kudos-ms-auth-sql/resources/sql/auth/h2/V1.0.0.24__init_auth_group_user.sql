--region DDL
create table if not exists "auth_group_user"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "group_id"        character(36)  not null,
    "user_id" character(36) not null,
    "principal_type" character varying(16) default 'USER' not null,
    "start_time" timestamp(6),
    "end_time" timestamp(6),
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
    constraint "uk_auth_group_user" unique ("group_id", "user_id")
    );

comment on table "auth_group_user" is '组-用户';
comment on column "auth_group_user"."id" is '主键';
comment on column "auth_group_user"."group_id" is '组ID';
comment on column "auth_group_user"."user_id" is '用户ID';
-- Putting somebody into a group IS an act of granting: it hands them every role the group carries.
-- So this relation needs the same governance columns as auth_role_user, or the group path becomes
-- the way to bypass the constraints enforced on direct grants (which is exactly what it used to be).
-- Groups are flat containers and never carry delegation authority themselves, hence no
-- delegable_depth here: roles obtained through a group are always terminal (depth 0).
comment on column "auth_group_user"."principal_type" is '主体类型：USER/SERVICE/API_KEY';
comment on column "auth_group_user"."start_time" is '成员身份生效时间，NULL 表示立即生效';
comment on column "auth_group_user"."end_time" is '成员身份失效时间，NULL 表示永不失效';
comment on column "auth_group_user"."granted_by" is '操作人ID（权限语义，区别于审计字段create_user_id）';
-- Reserved, and deliberately unused: groups are flat containers that carry no delegation authority,
-- so roles obtained through one are always terminal (see the note above on the absence of
-- delegable_depth). Nothing writes this column and no cascade walks it — the delegation chain and
-- its cascading revocation live entirely on auth_role_user. The column stays because the day a
-- deployment does want delegated group membership, adding it back to a populated table is a
-- migration and leaving it here costs nothing.
comment on column "auth_group_user"."parent_grant_id" is '预留：上游授权ID。组不承载转授权，本列当前无写入方，级联回收只走 auth_role_user';
comment on column "auth_group_user"."revoked" is '是否已撤销（软失效，保留授权链与审计）';
comment on column "auth_group_user"."revoke_reason" is '撤销原因';
comment on column "auth_group_user"."create_user_id" is '创建者ID';
comment on column "auth_group_user"."create_user_name" is '创建者名称';
comment on column "auth_group_user"."create_time" is '创建时间';
comment on column "auth_group_user"."update_user_id" is '更新者ID';
comment on column "auth_group_user"."update_user_name" is '更新者名称';
comment on column "auth_group_user"."update_time" is '更新时间';

-- Expiry/activation scan by end_time and start_time (see IAuthGroupUserService.announceWindowChanges).
create index if not exists "idx_auth_group_user_end_time" on "auth_group_user" ("end_time");
-- For the reserved parent_grant_id above; costs one index on a column that is currently always NULL,
-- which is cheaper than adding it later to a large populated table.
create index if not exists "idx_auth_group_user_parent_grant" on "auth_group_user" ("parent_grant_id");

--endregion DDL


--region DML

--endregion DML