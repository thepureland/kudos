CREATE TABLE IF NOT EXISTS "test_table"
(
    "id"            int2 NOT NULL,
    "balance"       float8 NOT NULL,
    CONSTRAINT "pk_test_table" PRIMARY KEY ("id")
);

comment ON TABLE "test_table" IS '测试表';
COMMENT ON COLUMN "test_table"."id" IS '主键';
COMMENT ON COLUMN "test_table"."balance" IS '余额';


-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS undo_log
(
    id            SERIAL       NOT NULL,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info BYTEA        NOT NULL,
    log_status    INT          NOT NULL,
    log_created   TIMESTAMP(0) NOT NULL,
    log_modified  TIMESTAMP(0) NOT NULL,
    CONSTRAINT pk_undo_log PRIMARY KEY (id),
    CONSTRAINT ux_undo_log UNIQUE (xid, branch_id)
);

COMMENT ON TABLE undo_log IS 'AT transaction mode undo table';
COMMENT ON COLUMN undo_log.branch_id IS 'branch transaction id';
COMMENT ON COLUMN undo_log.xid IS 'global transaction id';
COMMENT ON COLUMN undo_log.context IS 'undo_log context,such as serialization';
COMMENT ON COLUMN undo_log.rollback_info IS 'rollback info';
COMMENT ON COLUMN undo_log.log_status IS '0:normal status,1:defense status';
COMMENT ON COLUMN undo_log.log_created IS 'create datetime';
COMMENT ON COLUMN undo_log.log_modified IS 'modify datetime';

CREATE SEQUENCE IF NOT EXISTS undo_log_id_seq INCREMENT BY 1 MINVALUE 1 ;