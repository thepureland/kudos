-- region DDL

create table if not exists `msg_receive` (
    `id`                       varchar(36) default (uuid()) not null primary key comment '主键',
    `receiver_id`              varchar(36) not null         comment '接收者ID',
    `send_id`                  varchar(36) not null         comment '发送ID',
    `receive_status_dict_code` char(2)     not null         comment '接收状态字典码',
    `create_time`              timestamp(6) not null default current_timestamp(6) comment '创建时间',
    `update_time`              timestamp(6)                 comment '更新时间',
    `tenant_id`                varchar(36) not null         comment '租户ID',
    constraint `fk_msg_receive` foreign key (`send_id`) references `msg_send` (`id`)
) engine=InnoDB default charset=utf8mb4 comment '消息接收';

-- endregion DDL


-- region DML

-- endregion DML
