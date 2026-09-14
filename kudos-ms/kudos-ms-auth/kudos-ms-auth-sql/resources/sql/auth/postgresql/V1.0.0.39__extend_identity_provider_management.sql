alter table "auth_identity_provider" add column if not exists "create_user_id" character varying(36);
alter table "auth_identity_provider" add column if not exists "create_reason" character varying(512);
alter table "auth_identity_provider" add column if not exists "update_user_id" character varying(36);
alter table "auth_identity_provider" add column if not exists "update_reason" character varying(512);

create table if not exists "auth_identity_provider_claim_mapping"
(
    "id"                    character(36) not null primary key,
    "tenant_id"             character varying(36) not null,
    "subject_claims"        character varying(512) not null,
    "username_claims"       character varying(512),
    "display_name_claims"   character varying(512),
    "email_claims"          character varying(512),
    "email_verified_claims" character varying(512),
    "phone_claims"          character varying(512),
    "phone_verified_claims" character varying(512),
    "avatar_claims"         character varying(512),
    "locale_claims"         character varying(512),
    "union_id_claims"       character varying(512),
    "create_user_id"        character varying(36) not null,
    "create_reason"         character varying(512) not null,
    "create_time"           timestamp(6) not null,
    "update_user_id"        character varying(36) not null,
    "update_reason"         character varying(512) not null,
    "update_time"           timestamp(6) not null,
    constraint "fk_auth_identity_provider_claim_mapping_provider" foreign key ("id")
        references "auth_identity_provider" ("id")
);

create index if not exists "idx_auth_identity_provider_claim_mapping_tenant"
    on "auth_identity_provider_claim_mapping" ("tenant_id");

comment on table "auth_identity_provider_claim_mapping" is '租户 Provider 实例的类型化 claim path 映射';
comment on column "auth_identity_provider_claim_mapping"."subject_claims" is '有序 claim path CSV，第一个非空值胜出';
