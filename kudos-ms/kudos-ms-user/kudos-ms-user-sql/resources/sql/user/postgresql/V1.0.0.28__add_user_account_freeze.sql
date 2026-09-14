-- 给 user_account 补充账号冻结(挂起/锁定)6 列。
--
-- 语义：
--   freeze_type        非空表示存在一条冻结记录；冻结类型代码（manual / auto / admin / scheduled 等，由字典控制）
--   freeze_time        冻结记录写入时刻（审计）
--   freeze_start_time  冻结生效起点；为 null 视为"立即生效"
--   freeze_end_time    冻结失效时刻；为 null 视为"永久冻结"，非 null 表示到时自动解除
--   freeze_title       冻结原因标题（短）
--   freeze_content     冻结详细说明（长）
--
-- 登录判定逻辑：当 freeze_type IS NOT NULL 且 (freeze_start_time IS NULL 或 now >= freeze_start_time)
-- 且 (freeze_end_time IS NULL 或 now < freeze_end_time) 时视为"当前冻结"，登录被拒。

alter table "user_account" add column if not exists "freeze_type"       varchar(16);
alter table "user_account" add column if not exists "freeze_time"       timestamp(6);
alter table "user_account" add column if not exists "freeze_start_time" timestamp(6);
alter table "user_account" add column if not exists "freeze_end_time"   timestamp(6);
alter table "user_account" add column if not exists "freeze_title"      varchar(64);
alter table "user_account" add column if not exists "freeze_content"    varchar(256);

comment on column "user_account"."freeze_type"       is '冻结类型字典码；非空表示存在一条冻结记录';
comment on column "user_account"."freeze_time"       is '冻结记录创建时刻';
comment on column "user_account"."freeze_start_time" is '冻结生效起点；null 视为立即生效';
comment on column "user_account"."freeze_end_time"   is '冻结失效时刻；null 视为永久冻结';
comment on column "user_account"."freeze_title"      is '冻结原因标题';
comment on column "user_account"."freeze_content"    is '冻结详细说明';
