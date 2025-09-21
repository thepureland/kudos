create table if not exists "sys_mq_fail_msg"
(
    id              varchar(36) PRIMARY KEY NOT NULL COMMENT '主键ID',
    topic           varchar(255)            NOT NULL COMMENT '消息主题',
    msg_header_json text COMMENT '消息头json串',
    msg_body_json   text COMMENT '消息体json串',
    create_time     timestamp(6)            NOT NULL COMMENT '创建时间'
);

comment on table "sys_mq_fail_msg" is 'MQ失败信息';
