-- region DDL

create table if not exists `msg_template` (
    `id`                   varchar(36) default (uuid()) not null primary key comment '主键',
    `send_type_dict_code`  varchar(6)  not null         comment '发送类型代码',
    `event_type_dict_code` varchar(32) not null         comment '事件类型代码。send_type_dict_code为auto时，字典类型为auto_event_type;为manual时，则为manual_event_type',
    `msg_type_dict_code`   varchar(16) not null         comment '消息类型代码',
    `receiver_group_code`  varchar(36)                  comment '模板分组编码,uuid,用于区分同一事件下不同操作原因的多套模板',
    `locale_dict_code`     varchar(5)                   comment '国家-语言字典码',
    `title`                varchar(256)                 comment '模板标题',
    `content`              text                         comment '模板内容',
    `default_active`       boolean      not null default false comment '是否启用默认值',
    `default_title`        varchar(256)                 comment '模板标题默认值',
    `default_content`      text                         comment '模板内容默认值',
    `tenant_id`            varchar(36) not null         comment '租户ID',
    key `idx_msg_template__tenant_event` (`tenant_id`, `event_type_dict_code`, `msg_type_dict_code`, `locale_dict_code`)
) engine=InnoDB default charset=utf8mb4 comment '消息模板';

-- endregion DDL


-- region DML

-- endregion DML
