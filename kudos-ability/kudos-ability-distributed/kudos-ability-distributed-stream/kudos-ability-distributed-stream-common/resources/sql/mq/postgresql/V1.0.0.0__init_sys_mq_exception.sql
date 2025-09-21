CREATE TABLE IF NOT EXISTS sys_mq_fail_msg
(
    id              varchar(36) PRIMARY KEY,
    topic           varchar(255) NOT NULL,
    msg_header_json text,
    msg_body_json   text,
    create_time     timestamp(6) NOT NULL
);

COMMENT ON TABLE sys_mq_fail_msg IS 'MQ失败信息';
COMMENT ON COLUMN sys_mq_fail_msg.id IS '主键ID';
COMMENT ON COLUMN sys_mq_fail_msg.topic IS '消息主题';
COMMENT ON COLUMN sys_mq_fail_msg.msg_header_json IS '消息头json串';
COMMENT ON COLUMN sys_mq_fail_msg.msg_body_json IS '消息体json串';
COMMENT ON COLUMN sys_mq_fail_msg.create_time IS '创建时间';