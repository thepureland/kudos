-- 在 PostgreSQL 中创建表
CREATE TABLE IF NOT EXISTS test_table_flyway (
      id        SMALLINT       NOT NULL,
      balance   DOUBLE PRECISION NOT NULL,
      CONSTRAINT pk_test_table_flyway PRIMARY KEY (id)
);

-- 添加表和列的注释
COMMENT ON TABLE test_table_flyway IS '测试表';
COMMENT ON COLUMN test_table_flyway.id IS '主键';
COMMENT ON COLUMN test_table_flyway.balance IS '余额';
