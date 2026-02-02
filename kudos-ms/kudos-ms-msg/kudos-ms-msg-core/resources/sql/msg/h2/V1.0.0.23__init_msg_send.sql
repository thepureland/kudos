--region DDL

create table if not exists "msg_send" (
    "id"       CHAR(36) default RANDOM_UUID() not null primary key,
    "receiver_group_type_dict_code" VARCHAR(16) NOT NULL,
    "receiver_group_id" VARCHAR(36),
    "instance_id" CHAR(36) NOT NULL,
    "msg_type_dict_code" VARCHAR(16) NOT NULL,
    "locale_dict_code" varchar(5),
    "send_status_dict_code" char(2) NOT NULL,
    "create_time" timestamp(6) default now() NOT NULL,
    "update_time" timestamp(6),
    "success_count" int4 DEFAULT 0,
    "fail_count" int4 DEFAULT 0,
    "job_id" varchar(36),
    "tenant_id" VARCHAR(36) not null,
    constraint "fk_msg_send"
    foreign key ("instance_id") references "msg_instance" ("id")
);

COMMENT ON TABLE "msg_send" IS '消息发送';
COMMENT ON COLUMN "msg_send"."id" IS '主键';
COMMENT ON COLUMN "msg_send"."receiver_group_type_dict_code" IS '接收者群组类型字典码';
COMMENT ON COLUMN "msg_send"."receiver_group_id" IS '接收者群组ID';
COMMENT ON COLUMN "msg_send"."instance_id" IS '消息实例ID';
COMMENT ON COLUMN "msg_send"."msg_type_dict_code" IS '消息类型字典码';
COMMENT ON COLUMN "msg_send"."locale_dict_code" IS '国家-语言字典码';
COMMENT ON COLUMN "msg_send"."send_status_dict_code" IS '发送状态字典码';
COMMENT ON COLUMN "msg_send"."create_time" IS '创建时间';
COMMENT ON COLUMN "msg_send"."update_time" IS '更新时间';
COMMENT ON COLUMN "msg_send"."success_count" IS '发送成功数量';
COMMENT ON COLUMN "msg_send"."fail_count" IS '发送失败数量';
COMMENT ON COLUMN "msg_send"."job_id" IS '定时任务ID';
COMMENT ON COLUMN "msg_send"."tenant_id" IS '租户ID';

--endregion DDL


--region DML

--endregion DML
