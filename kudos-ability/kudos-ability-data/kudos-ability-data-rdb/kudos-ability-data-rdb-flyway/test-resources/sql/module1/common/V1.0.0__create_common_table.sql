-- Lives in sql/module1/common/: applies to every database type and is merged into the same
-- version stream as the dbType-specific scripts (proves the shared-location support).
CREATE TABLE IF NOT EXISTS "test_table_flyway_common"
(
    "id" int2 NOT NULL,
    CONSTRAINT "pk_test_table_flyway_common" PRIMARY KEY ("id")
);

MERGE INTO "test_table_flyway_common" ("id") VALUES (1);
