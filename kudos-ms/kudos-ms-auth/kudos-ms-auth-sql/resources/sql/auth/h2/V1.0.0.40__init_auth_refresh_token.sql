-- Opaque refresh-token records. The bearer value is never persisted; token_hash is SHA-256.
create table if not exists "auth_refresh_token"
(
    "id"             character(36)          not null primary key,
    "session_id"     character varying(36)  not null,
    "family_id"      character varying(36)  not null,
    "parent_id"      character varying(36),
    "replaced_by_id" character varying(36),
    "token_hash"     character(64)          not null,
    "token_epoch"    bigint default 0       not null,
    "issued_at"      timestamp(6)           not null,
    "expires_at"     timestamp(6)           not null,
    "consumed_at"    timestamp(6),
    "revoked_at"     timestamp(6),
    "revoke_reason"  character varying(128),
    constraint "uq_auth_refresh_token_hash" unique ("token_hash"),
    constraint "uq_auth_refresh_token_parent" unique ("parent_id")
);

create index if not exists "idx_auth_refresh_token_session" on "auth_refresh_token" ("session_id");
create index if not exists "idx_auth_refresh_token_family" on "auth_refresh_token" ("family_id");

comment on table "auth_refresh_token" is '刷新令牌轮换记录（只保存SHA-256）';
comment on column "auth_refresh_token"."session_id" is '逻辑认证会话ID';
comment on column "auth_refresh_token"."family_id" is '令牌族ID，检测重用时整族撤销';
comment on column "auth_refresh_token"."parent_id" is '轮换父令牌ID';
comment on column "auth_refresh_token"."replaced_by_id" is '成功轮换产生的子令牌ID';
comment on column "auth_refresh_token"."token_hash" is '原始刷新令牌的SHA-256，原文只在签发响应返回';
comment on column "auth_refresh_token"."token_epoch" is '签发时主体令牌纪元；管理员递增后本族不可刷新';
comment on column "auth_refresh_token"."consumed_at" is '令牌首次成功轮换时间';
comment on column "auth_refresh_token"."revoked_at" is '撤销时间';
comment on column "auth_refresh_token"."revoke_reason" is '撤销原因';
