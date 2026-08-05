--region DDL
-- Per-principal token epoch: the lever that makes a stateless token revocable.
--
-- **The problem this solves.** A JWT is believed because it is signed, not because it is checked
-- against anything, so a permission taken away at 10:00 keeps working until the token minted at
-- 09:55 expires. Shortening the TTL trades that window against re-issue traffic and never closes
-- it. The fix is to give the token something cheap to compare against: a *permission version* the
-- PEP can verify per request out of cache.
--
-- **Why a row here rather than a counter on user_account.** Two reasons. The version is mostly
-- derived — it changes because a principal's resolved grants changed, and that is computed, not
-- stored, so there is nothing to increment. What this table adds is the part that is *not* derived:
-- an administrative "kill every token this principal holds, right now", which must work even when
-- the permissions did not change at all (a leaked laptop, a shared password). Keeping that in the
-- auth module also avoids a write to a kudos-ms-user table on every revocation.
--
-- Absence of a row means epoch 0. Rows are created lazily on the first bump, so the common case
-- costs nothing.
create table if not exists "auth_principal_version"
(
    "id"               character(36) default RANDOM_UUID() not null primary key,
    "principal_id"     character varying(36) not null,
    "principal_type"   character varying(16) default 'USER' not null,
    "epoch"            bigint  default 0     not null,
    "reason"           character varying(256),
    "create_user_id"   character varying(36),
    "create_user_name" character varying(32),
    "create_time"      timestamp(6),
    "update_user_id"   character varying(36),
    "update_user_name" character varying(32),
    "update_time"      timestamp(6)
);

-- A principal has exactly one epoch. The surrogate key is only there because the module's DAO base
-- expects one; this constraint is what actually holds the invariant, and without it two rows could
-- disagree about whether a token is still valid.
create unique index if not exists "uq_auth_principal_version" on "auth_principal_version" ("principal_id");

comment on table "auth_principal_version" is '主体令牌纪元（用于强制旧令牌失效）';
comment on column "auth_principal_version"."id" is '主键';
comment on column "auth_principal_version"."principal_id" is '主体ID（用户/服务账号/API Key）';
comment on column "auth_principal_version"."principal_type" is '主体类型：USER/SERVICE/API_KEY';
-- Monotonic. It only ever goes up, so an administrator's "log them out" can never be undone by a
-- later permission change happening to reproduce an earlier state.
comment on column "auth_principal_version"."epoch" is '令牌纪元，单调递增；旧令牌携带的纪元小于此值即失效';
comment on column "auth_principal_version"."reason" is '最近一次递增的原因（凭据泄露、离职、强制重新登录等）';
comment on column "auth_principal_version"."create_user_id" is '创建者ID';
comment on column "auth_principal_version"."create_user_name" is '创建者名称';
comment on column "auth_principal_version"."create_time" is '创建时间';
comment on column "auth_principal_version"."update_user_id" is '更新者ID';
comment on column "auth_principal_version"."update_user_name" is '更新者名称';
comment on column "auth_principal_version"."update_time" is '更新时间';
--endregion DDL


--region DML

--endregion DML
