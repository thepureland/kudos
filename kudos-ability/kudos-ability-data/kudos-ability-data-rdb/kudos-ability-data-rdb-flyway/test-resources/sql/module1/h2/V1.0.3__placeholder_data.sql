DROP TABLE IF EXISTS "test_table_flyway_placeholder";

CREATE TABLE IF NOT EXISTS "test_table_flyway_placeholder"
(
    "name" varchar(64) NOT NULL,
    CONSTRAINT "pk_test_table_flyway_placeholder" PRIMARY KEY ("name")
);

MERGE INTO "test_table_flyway_placeholder" ("name") VALUES
   ('${flyway_test_name}');
