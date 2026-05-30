--region DDL
-- Data scope (数据权限): per-role row-level visibility policy. When a user holds the role, the
-- rows of business data they may see/operate are constrained by this scope:
--   ALL            no row restriction (see everything in the tenant)
--   ORG_AND_CHILD  the user's own org plus all descendant orgs
--   ORG            the user's own org only
--   SELF           only rows the user created
--   CUSTOM         an explicit set of orgs listed in auth_role_org
--
-- Stored as the enum's code string (see DataScopeEnum). Default 'ALL' so every existing role
-- keeps its current unrestricted behaviour — turning the feature on never silently tightens
-- anyone's visibility. Resolution (across a user's roles) takes the MOST permissive scope; a
-- NULL is treated as ALL for the same backward-compatibility reason.
alter table "auth_role"
    add column if not exists "data_scope" character varying(32) default 'ALL';

comment on column "auth_role"."data_scope" is '数据权限范围：ALL/ORG_AND_CHILD/ORG/SELF/CUSTOM';
--endregion DDL


--region DML
-- No backfill needed: the column default 'ALL' already applies to existing rows.
--endregion DML
