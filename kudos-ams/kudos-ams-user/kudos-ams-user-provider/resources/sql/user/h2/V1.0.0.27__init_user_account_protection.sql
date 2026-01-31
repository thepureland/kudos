--region DDL

create table if not exists "user_account_protection"
(
    "id"                   CHAR(36)      default RANDOM_UUID()     not null primary key,
    "user_id"              CHAR(36)     not null,
    "question1"            VARCHAR(64)             not null,
    "answer1"              VARCHAR(64)             not null,
    "question2"            VARCHAR(64),
    "answer2"              VARCHAR(64),
    "question3"            VARCHAR(64),
    "answer3"              VARCHAR(64),
    "safe_contact_way_id"  CHAR(36),
    "total_validate_count" INT4      default 0     not null,
    "match_question_count" INT4      default 1     not null,
    "error_times"          INT4      default 0     not null,
    "remark"               VARCHAR(128),
    "active"               BOOLEAN   default TRUE  not null,
    "built_in"             BOOLEAN   default FALSE not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "fk_user_account_protection"
    foreign key ("user_id") references "user_account" ("id")
    );

comment on table "user_account_protection" is '用户账号保护';
comment on column "user_account_protection"."id" is '主键';
comment on column "user_account_protection"."question1" is '问题１';
comment on column "user_account_protection"."answer1" is '答案1';
comment on column "user_account_protection"."question2" is '问题2';
comment on column "user_account_protection"."answer2" is '答案2';
comment on column "user_account_protection"."question3" is '问题3';
comment on column "user_account_protection"."answer3" is '答案3';
comment on column "user_account_protection"."safe_contact_way_id" is '安全的联系方式id';
comment on column "user_account_protection"."total_validate_count" is '总的找回密码次数';
comment on column "user_account_protection"."match_question_count" is '必须答对的问题数';
comment on column "user_account_protection"."error_times" is '错误次数';
comment on column "user_account_protection"."remark" is '备注，或其国际化key';
comment on column "user_account_protection"."active" is '是否启用';
comment on column "user_account_protection"."built_in" is '是否内置';
comment on column "user_account_protection"."create_user_id" is '创建者ID';
comment on column "user_account_protection"."create_user_name" is '创建者名称';
comment on column "user_account_protection"."create_time" is '创建时间';
comment on column "user_account_protection"."update_user_id" is '更新者ID';
comment on column "user_account_protection"."update_user_name" is '更新者名称';
comment on column "user_account_protection"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
