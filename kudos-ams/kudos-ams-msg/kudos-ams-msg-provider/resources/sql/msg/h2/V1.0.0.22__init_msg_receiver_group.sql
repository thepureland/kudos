--region DDL

create table if not exists "msg_receiver_group" (
      "id"       CHAR(36) default RANDOM_UUID() not null primary key,
      "receiver_group_type_dict_code" VARCHAR(16) not null,
      "define_table" VARCHAR(64) not null,
      "name_column" VARCHAR(64) not null,
      "remark"      VARCHAR(128),
      "active"   BOOLEAN  default TRUE          not null,
      "built_in" BOOLEAN  default FALSE         not null,
      "create_user_id"      character varying(36),
      "create_user_name"    character varying(32),
      "create_time"         timestamp(6),
      "update_user_id"      character varying(36),
      "update_user_name"    character varying(32),
      "update_time"         timestamp(6),
);

create unique index "uq_msg_receiver_group__type_code"
    on "msg_receiver_group" ("receiver_group_type_dict_code");

COMMENT ON TABLE "msg_receiver_group" IS '消息接收者群组';
COMMENT ON COLUMN "msg_receiver_group"."id" IS '主键';
COMMENT ON COLUMN "msg_receiver_group"."receiver_group_type_dict_code" IS '接收者群组类型字典码';
COMMENT ON COLUMN "msg_receiver_group"."define_table" IS '群组定义的表';
COMMENT ON COLUMN "msg_receiver_group"."name_column" IS '群组名称在具体群组表中的字段名';
comment on column "msg_receiver_group"."remark" is '备注，或其国际化key';
comment on column "msg_receiver_group"."active" is '是否启用';
comment on column "msg_receiver_group"."built_in" is '是否内置';
comment on column "msg_receiver_group"."create_user_id" is '创建者id';
comment on column "msg_receiver_group"."create_user_name" is '创建者名称';
comment on column "msg_receiver_group"."create_time" is '创建时间';
comment on column "msg_receiver_group"."update_user_id" is '更新者id';
comment on column "msg_receiver_group"."update_user_name" is '更新者名称';
comment on column "msg_receiver_group"."update_time" is '更新时间';
        
--endregion DDL


--region DML

--endregion DML