--region DML
-- 必须先回填再收紧：本迁移之前 "built_in" 允许为空（建表时只有 default false，没有 not null），
-- 已存在的行可能带 NULL，直接 set not null 会失败。
update "user_account" set "built_in" = false where "built_in" is null;
update "user_org" set "built_in" = false where "built_in" is null;
--endregion DML


--region DDL
-- 收紧为 not null，使这两张表可以接入 IManagedDbEntity / ManagedTable：
-- IHasBuiltIn 声明的是 `var builtIn: Boolean`（非空），列可空会让 ktorm 读到 NULL 时抛异常。
-- 同模块的 user_account_third / user_contact_way / user_account_protection 建表时就是 not null，
-- 无需处理；user_login_remember_me / user_log_login / user_org_user 缺少全套管理字段，不在此列。
alter table "user_account"
    alter column "built_in" set not null;

alter table "user_org"
    alter column "built_in" set not null;
--endregion DDL
