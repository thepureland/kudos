-- 测试数据SQL文件：RdbAndRedisCacheTestBaseTest
CREATE TABLE IF NOT EXISTS test_table_cache (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

-- MERGE (not INSERT): the in-memory ds-master lives for the whole JVM (DB_CLOSE_DELAY=-1) and this
-- script is re-run per test, so a plain INSERT hits the primary key on the second run.
MERGE INTO test_table_cache (id, name) KEY(id) VALUES ('rdb-redis-test', 'rdb-redis-test');
