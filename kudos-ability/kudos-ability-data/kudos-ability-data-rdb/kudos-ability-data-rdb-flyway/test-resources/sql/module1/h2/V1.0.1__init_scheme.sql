CREATE TABLE IF NOT EXISTS "test_table_flyway"
(
    "id"            int2 NOT NULL,
    "balance"       float8 NOT NULL,
    CONSTRAINT "pk_test_table_flyway" PRIMARY KEY ("id")
);

comment ON TABLE "test_table_flyway" IS '测试表';
COMMENT ON COLUMN "test_table_flyway"."id" IS '主键';
COMMENT ON COLUMN "test_table_flyway"."balance" IS '余额';