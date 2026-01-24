-- 测试数据SQL文件：AbstractRdbTestBaseTest
CREATE TABLE IF NOT EXISTS test_table (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

INSERT INTO test_table (id, name) VALUES ('abstract-rdb-test', 'abstract-rdb-test');
