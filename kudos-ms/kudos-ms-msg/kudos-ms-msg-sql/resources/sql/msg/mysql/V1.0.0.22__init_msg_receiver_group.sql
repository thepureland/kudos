-- region DDL

create table if not exists `msg_receiver_group` (
    `id`                            varchar(36) default (uuid()) not null primary key comment '主键',
    `receiver_group_type_dict_code` varchar(16) not null         comment '接收者群组类型字典码',
    `define_table`                  varchar(64) not null         comment '群组定义的表',
    `name_column`                   varchar(64) not null         comment '群组名称在具体群组表中的字段名',
    `remark`                        varchar(128)                 comment '备注，或其国际化key',
    `active`                        boolean     not null default true  comment '是否启用',
    `built_in`                      boolean     not null default false comment '是否内置',
    `create_user_id`                varchar(36)                  comment '创建者id',
    `create_user_name`              varchar(32)                  comment '创建者名称',
    `create_time`                   timestamp(6)                 comment '创建时间',
    `update_user_id`                varchar(36)                  comment '更新者id',
    `update_user_name`              varchar(32)                  comment '更新者名称',
    `update_time`                   timestamp(6)                 comment '更新时间',
    unique key `uq_msg_receiver_group__type_code` (`receiver_group_type_dict_code`)
) engine=InnoDB default charset=utf8mb4 comment '消息接收者群组';

-- endregion DDL


-- region DML

-- endregion DML
