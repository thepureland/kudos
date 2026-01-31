--region DDL

create table if not exists "user_contact_way"
(
    "id"                           CHAR(36)  default RANDOM_UUID() not null primary key,
    "user_id"                      CHAR(36)     not null,
    "contact_way_dict_code"        CHAR(3)                         not null,
    "contact_way_value"            VARCHAR(128)                    not null,
    "contact_way_status_dict_code" CHAR(2)   default '00'          not null,
    "priority"                     INT2,
    "remark"                       VARCHAR(128),
    "active"                       BOOLEAN   default TRUE          not null,
    "built_in"                     BOOLEAN   default FALSE         not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "fk_user_contact_way"
        foreign key ("user_id") references "user_account" ("id")
);

comment on table "user_contact_way" is '用户联系方式';
comment on column "user_contact_way"."user_id" is '用户ID';
comment on column "user_contact_way"."contact_way_dict_code" is '联系方式字典码';
comment on column "user_contact_way"."contact_way_value" is '联系方式值';
comment on column "user_contact_way"."contact_way_status_dict_code" is '联系方式状态字典码';
comment on column "user_contact_way"."priority" is '优先级';
comment on column "user_contact_way"."remark" is '备注';
comment on column "user_contact_way"."active" is '是否启用';
comment on column "user_contact_way"."built_in" is '是否内置';
comment on column "user_contact_way"."create_user_id" is '创建者ID';
comment on column "user_contact_way"."create_user_name" is '创建者名称';
comment on column "user_contact_way"."create_time" is '创建时间';
comment on column "user_contact_way"."update_user_id" is '更新者ID';
comment on column "user_contact_way"."update_user_name" is '更新者名称';
comment on column "user_contact_way"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
