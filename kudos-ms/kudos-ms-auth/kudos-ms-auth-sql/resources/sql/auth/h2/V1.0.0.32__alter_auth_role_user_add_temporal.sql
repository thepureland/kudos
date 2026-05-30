--region DDL
-- Temporal (时效性) role grants: an assignment may carry an optional validity window.
--   start_time  the grant becomes effective at/after this instant (NULL = effective immediately)
--   end_time    the grant expires after this instant (NULL = never expires)
-- A grant is "active now" when (start_time IS NULL OR start_time <= now) AND
-- (end_time IS NULL OR end_time >= now). Existing rows have both NULL → permanent, exactly the
-- pre-feature behaviour, so nothing is silently revoked.
--
-- Enforcement: the permission-resolution DAO queries filter to active-now grants at compute time,
-- and a purge sweep (IAuthRoleUserTemporalService.purgeExpired) deletes expired rows and evicts the
-- affected users' caches. Future-dated activation relies on the cache being refreshed/evicted at or
-- after start_time.
alter table "auth_role_user"
    add column if not exists "start_time" timestamp(6);

alter table "auth_role_user"
    add column if not exists "end_time" timestamp(6);

comment on column "auth_role_user"."start_time" is '授权生效时间，NULL 表示立即生效';
comment on column "auth_role_user"."end_time"   is '授权失效时间，NULL 表示永不失效';

-- The purge sweep scans for rows whose end_time has passed; index it so the sweep stays cheap.
create index if not exists "idx_auth_role_user_end_time" on "auth_role_user" ("end_time");
--endregion DDL


--region DML
-- No backfill: existing grants keep start_time/end_time NULL (permanent).
--endregion DML
