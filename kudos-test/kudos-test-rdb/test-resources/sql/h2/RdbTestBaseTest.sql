-- 测试数据SQL文件：AbstractRdbTestBaseTest
CREATE TABLE IF NOT EXISTS test_table (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

-- MERGE (not INSERT): the in-memory ds-master lives for the whole JVM (DB_CLOSE_DELAY=-1) and this
-- script is re-run per test, so a plain INSERT hits the primary key on the second run.
MERGE INTO test_table (id, name) KEY(id) VALUES ('abstract-rdb-test', 'abstract-rdb-test');
