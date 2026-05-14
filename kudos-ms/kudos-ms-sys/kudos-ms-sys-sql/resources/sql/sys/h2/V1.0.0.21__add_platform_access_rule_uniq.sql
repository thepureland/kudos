--region DDL
-- 补齐 sys_access_rule 平台级唯一性裂缝：
--
-- 原 unique (system_code, tenant_id) 用 ANSI SQL 默认的 NULLS DISTINCT 语义，
-- 即两个 tenant_id IS NULL 的行不算重复 —— 所以同一 system_code 下**平台级规则可被插入多行**。
-- Cache 层（P0 #1）已用空串归一化统一访问路径，但 DB 层没有保护。
--
-- 改用 NULLS NOT DISTINCT 让 NULL 视为相等，从而对平台级规则也强制 system_code 唯一。
-- H2 2.2+ 与 PG 15+ 均原生支持该修饰符。
alter table "sys_access_rule" drop constraint if exists "uq_sys_access_rule";

create unique nulls not distinct index if not exists "uq_sys_access_rule"
    on "sys_access_rule" ("system_code", "tenant_id");
--endregion DDL
