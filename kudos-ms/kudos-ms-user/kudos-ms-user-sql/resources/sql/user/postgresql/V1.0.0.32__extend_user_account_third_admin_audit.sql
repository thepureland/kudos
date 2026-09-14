alter table "user_account_third_audit"
    add column if not exists "operation_reason" character varying(512);

alter table "user_account_third_audit"
    add column if not exists "before_snapshot" character varying(2048);

alter table "user_account_third_audit"
    add column if not exists "after_snapshot" character varying(2048);

comment on column "user_account_third_audit"."operation_reason" is '管理员身份生命周期操作原因';
comment on column "user_account_third_audit"."before_snapshot" is '变更前安全快照，subject 仅含 SHA-256';
comment on column "user_account_third_audit"."after_snapshot" is '变更后安全快照，subject 仅含 SHA-256';
