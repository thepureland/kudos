--region DDL

create table if not exists "msg_template" (
    "id"       CHAR(36) default RANDOM_UUID() not null primary key,
    "send_type_dict_code" varchar(6) NOT NULL,
    "event_type_dict_code" VARCHAR(32) NOT NULL,
    "msg_type_dict_code" VARCHAR(16) NOT NULL,
    "receiver_group_code" char(36),
    "locale_dict_code" char(5),
    "title" VARCHAR(256),
    "content" varchar,
    "default_active" bool NOT NULL DEFAULT false,
    "default_title" VARCHAR(256),
    "default_content" varchar,
    "tenant_id" VARCHAR(36) not null
);

COMMENT ON TABLE "msg_template" IS '消息模板';
COMMENT ON COLUMN "msg_template"."id" IS '主键';
COMMENT ON COLUMN "msg_template"."msg_type_dict_code" IS '发送类型代码';
COMMENT ON COLUMN "msg_template"."event_type_dict_code" IS '事件类型代码。send_type_dict_code为auto时，字典类型为auto_event_type;为manual时，则为manual_event_type';
COMMENT ON COLUMN "msg_template"."msg_type_dict_code" IS '消息类型代码';
COMMENT ON COLUMN "msg_template"."receiver_group_code" IS '模板分组编码,uuid,用于区分同一事件下不同操作原因的多套模板';
COMMENT ON COLUMN "msg_template"."locale_dict_code" IS '国家-语言字典码';
COMMENT ON COLUMN "msg_template"."title" IS '模板标题';
COMMENT ON COLUMN "msg_template"."content" IS '模板内容';
COMMENT ON COLUMN "msg_template"."default_active" IS '是否启用默认值';
COMMENT ON COLUMN "msg_template"."default_title" IS '模板标题默认值';
COMMENT ON COLUMN "msg_template"."default_content" IS '模板内容默认值';
COMMENT ON COLUMN "msg_template"."tenant_id" IS '租户ID';

--endregion DDL


--region DML

--endregion DML


