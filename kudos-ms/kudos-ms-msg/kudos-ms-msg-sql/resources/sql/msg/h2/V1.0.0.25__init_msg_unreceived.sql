--region DDL

create table if not exists "msg_unreceived" (
    "id"                       CHAR(36)     default RANDOM_UUID() not null primary key,
    "receiver_id"              CHAR(36)     NOT NULL,
    "send_id"                  CHAR(36)     NOT NULL,
    "publish_method_dict_code" VARCHAR(16)  NOT NULL,
    "fail_reason"              VARCHAR(512),
    "retry_count"              INT          default 0           not null,
    "last_retry_time"          timestamp(6),
    "resolved"                 boolean      default false       not null,
    "create_time"              timestamp(6) default now()       not null,
    "update_time"              timestamp(6),
    "tenant_id"                VARCHAR(36)  NOT NULL,
    constraint "fk_msg_unreceived_send"
        foreign key ("send_id") references "msg_send" ("id")
);

COMMENT ON TABLE "msg_unreceived" IS '消息发送失败的接收人，供重试/审计';
COMMENT ON COLUMN "msg_unreceived"."id" IS '主键';
COMMENT ON COLUMN "msg_unreceived"."receiver_id" IS '原本应该收到消息的用户ID';
COMMENT ON COLUMN "msg_unreceived"."send_id" IS '关联的发送批次ID（msg_send.id）';
COMMENT ON COLUMN "msg_unreceived"."publish_method_dict_code" IS '失败发生的渠道（publish_method 字典码）';
COMMENT ON COLUMN "msg_unreceived"."fail_reason" IS '失败原因，文本：NO_CONTACT / SMTP_REJECT / TIMEOUT 等';
COMMENT ON COLUMN "msg_unreceived"."retry_count" IS '已重试次数';
COMMENT ON COLUMN "msg_unreceived"."last_retry_time" IS '最近一次重试的时间';
COMMENT ON COLUMN "msg_unreceived"."resolved" IS '是否已处理（重试成功 / admin 关闭后置 true）';
COMMENT ON COLUMN "msg_unreceived"."create_time" IS '创建时间';
COMMENT ON COLUMN "msg_unreceived"."update_time" IS '更新时间';
COMMENT ON COLUMN "msg_unreceived"."tenant_id" IS '租户ID';

create index if not exists "idx_msg_unreceived_send" on "msg_unreceived" ("send_id");
create index if not exists "idx_msg_unreceived_receiver_unresolved"
    on "msg_unreceived" ("receiver_id", "resolved");

--endregion DDL


--region DML

--endregion DML
