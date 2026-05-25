-- region DDL

create table if not exists `msg_instance` (
    `id`                   varchar(36) default (uuid()) not null primary key comment '主键',
    `locale_dict_code`     varchar(5)                                          comment '国家-语言字典码',
    `title`                varchar(256)                                        comment '标题，可能还含有用户名等实际要发送时才能确定的模板变量',
    `content`              text                                                comment '通知内容，可能还含有用户名等实际要发送时才能确定的模板变量',
    `template_id`          varchar(36)                                         comment '消息模板id，为null时表示没有依赖静态模板，可能是依赖动态模板或无模板',
    `send_type_dict_code`  varchar(6)                                          comment '发送类型字典码',
    `event_type_dict_code` varchar(32)                                         comment '事件类型字典码',
    `msg_type_dict_code`   varchar(16)                                         comment '消息类型字典码',
    `valid_time_start`     timestamp   not null default current_timestamp(6)   comment '有效期起',
    `valid_time_end`       timestamp   not null default (current_timestamp(6) + interval 99999 day) comment '有效期止',
    `tenant_id`            varchar(36) not null                                comment '租户ID',
    constraint `fk_msg_instance` foreign key (`template_id`) references `msg_template` (`id`)
) engine=InnoDB default charset=utf8mb4 comment '消息实例';

-- endregion DDL


-- region DML

-- endregion DML
