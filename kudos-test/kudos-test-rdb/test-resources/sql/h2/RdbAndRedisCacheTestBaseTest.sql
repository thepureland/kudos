-- 测试数据SQL文件：RdbAndRedisCacheTestBaseTest
CREATE TABLE IF NOT EXISTS test_table (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

INSERT INTO test_table (id, name) VALUES ('rdb-redis-test', 'rdb-redis-test');
