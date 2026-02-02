--region DDL

create table if not exists "msg_instance" (
       "id"       CHAR(36) default RANDOM_UUID() not null primary key,
       "locale_dict_code" varchar(5),
       "title" VARCHAR(256),
       "content" varchar,
       "template_id" CHAR(36),
       "send_type_dict_code" varchar(6),
       "event_type_dict_code" VARCHAR(32),
       "msg_type_dict_code" VARCHAR(16),
       "valid_time_start" TIMESTAMP default now() not null,
       "valid_time_end" TIMESTAMP default (now()+99999) not null,
       "tenant_id" VARCHAR(36) not null,
       constraint "fk_msg_instance"
            foreign key ("template_id") references "msg_template" ("id")
);

COMMENT ON TABLE "msg_instance" IS '消息实例';
COMMENT ON COLUMN "msg_instance"."id" IS '主键';
COMMENT ON COLUMN "msg_instance"."locale_dict_code" IS '国家-语言字典码';
COMMENT ON COLUMN "msg_instance"."title" IS '标题，可能还含有用户名等实际要发送时才能确定的模板变量';
COMMENT ON COLUMN "msg_instance"."content" IS '通知内容，可能还含有用户名等实际要发送时才能确定的模板变量';
COMMENT ON COLUMN "msg_instance"."template_id" IS '消息模板id，为null时表示没有依赖静态模板，可能是依赖动态模板或无模板';
COMMENT ON COLUMN "msg_instance"."send_type_dict_code" IS '发送类型字典码';
COMMENT ON COLUMN "msg_instance"."event_type_dict_code" IS '事件类型字典码';
COMMENT ON COLUMN "msg_instance"."msg_type_dict_code" IS '消息类型字典码';
COMMENT ON COLUMN "msg_instance"."valid_time_start" IS '有效期起';
COMMENT ON COLUMN "msg_instance"."valid_time_end" IS '有效期止';
COMMENT ON COLUMN "msg_instance"."tenant_id" IS '租户ID';

--endregion DDL


--region DML

--endregion DML
