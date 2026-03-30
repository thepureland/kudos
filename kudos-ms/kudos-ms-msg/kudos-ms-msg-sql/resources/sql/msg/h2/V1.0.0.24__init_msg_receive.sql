--region DDL

create table if not exists "msg_receive" (
    "id"       CHAR(36) default RANDOM_UUID() not null primary key,
    "receiver_id" CHAR(36) NOT NULL,
    "send_id" CHAR(36) NOT NULL,
    "receive_status_dict_code" char(2) NOT NULL,
    "create_time" timestamp(6) default now() NOT NULL,
    "update_time" timestamp(6),
    "tenant_id" VARCHAR(36) not null,
    constraint "fk_msg_receive"
        foreign key ("send_id") references "msg_send" ("id")
);

COMMENT ON TABLE "msg_receive" IS '消息接收';
COMMENT ON COLUMN "msg_receive"."id" IS '主键';
COMMENT ON COLUMN "msg_receive"."receiver_id" IS '接收者ID';
COMMENT ON COLUMN "msg_receive"."send_id" IS '发送ID';
COMMENT ON COLUMN "msg_receive"."receive_status_dict_code" IS '接收状态字典码';
COMMENT ON COLUMN "msg_receive"."create_time" IS '创建时间';
COMMENT ON COLUMN "msg_receive"."update_time" IS '更新时间';
COMMENT ON COLUMN "msg_receive"."tenant_id" IS '租户ID';

--endregion DDL


--region DML

--endregion DML
