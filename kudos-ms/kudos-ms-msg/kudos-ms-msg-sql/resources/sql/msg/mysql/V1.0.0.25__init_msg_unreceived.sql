-- region DDL

create table if not exists `msg_unreceived` (
    `id`                       varchar(36)  default (uuid()) not null primary key comment '主键',
    `receiver_id`              varchar(36)  not null         comment '原本应该收到消息的用户ID',
    `send_id`                  varchar(36)  not null         comment '关联的发送批次ID（msg_send.id）',
    `publish_method_dict_code` varchar(16)  not null         comment '失败发生的渠道（publish_method 字典码）',
    `fail_reason`              varchar(512)                  comment '失败原因，文本：NO_CONTACT / SMTP_REJECT / TIMEOUT 等',
    `retry_count`              int          not null default 0 comment '已重试次数',
    `last_retry_time`          timestamp(6)                  comment '最近一次重试的时间',
    `resolved`                 boolean      not null default false comment '是否已处理（重试成功 / admin 关闭后置 true）',
    `create_time`              timestamp(6) not null default current_timestamp(6) comment '创建时间',
    `update_time`              timestamp(6)                  comment '更新时间',
    `tenant_id`                varchar(36)  not null         comment '租户ID',
    key `idx_msg_unreceived_send` (`send_id`),
    key `idx_msg_unreceived_receiver_unresolved` (`receiver_id`, `resolved`),
    constraint `fk_msg_unreceived_send` foreign key (`send_id`) references `msg_send` (`id`)
) engine=InnoDB default charset=utf8mb4 comment '消息发送失败的接收人，供重试/审计';

-- endregion DDL


-- region DML

-- endregion DML
