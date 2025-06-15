-- 在 PostgreSQL 中创建表
CREATE TABLE IF NOT EXISTS test_table (
      id        SMALLINT       NOT NULL,
      balance   DOUBLE PRECISION NOT NULL,
      CONSTRAINT pk_test_table PRIMARY KEY (id)
);

-- 添加表和列的注释
COMMENT ON TABLE test_table IS '测试表';
COMMENT ON COLUMN test_table.id IS '主键';
COMMENT ON COLUMN test_table.balance IS '余额';
