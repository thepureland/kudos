-- region DDL

create table if not exists `msg_send` (
    `id`                            varchar(36) default (uuid()) not null primary key comment '主键',
    `receiver_group_type_dict_code` varchar(16) not null         comment '接收者群组类型字典码',
    `receiver_group_id`             varchar(36)                  comment '接收者群组ID',
    `instance_id`                   varchar(36) not null         comment '消息实例ID',
    `msg_type_dict_code`            varchar(16) not null         comment '消息类型字典码',
    `locale_dict_code`              varchar(5)                   comment '国家-语言字典码',
    `send_status_dict_code`         char(2)     not null         comment '发送状态字典码',
    `create_time`                   timestamp(6) not null default current_timestamp(6) comment '创建时间',
    `update_time`                   timestamp(6)                 comment '更新时间',
    `success_count`                 int          default 0       comment '发送成功数量',
    `fail_count`                    int          default 0       comment '发送失败数量',
    `job_id`                        varchar(36)                  comment '定时任务ID',
    `tenant_id`                     varchar(36) not null         comment '租户ID',
    constraint `fk_msg_send` foreign key (`instance_id`) references `msg_instance` (`id`)
) engine=InnoDB default charset=utf8mb4 comment '消息发送';

-- endregion DDL


-- region DML

-- endregion DML
