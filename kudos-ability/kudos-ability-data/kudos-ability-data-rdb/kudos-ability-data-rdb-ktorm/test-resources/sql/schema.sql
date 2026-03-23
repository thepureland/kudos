drop TABLE IF EXISTS "test_table_ktorm";
CREATE TABLE IF NOT EXISTS "test_table_ktorm"
(
    "id"            int2 not null,
    "name"          varchar(255) NOT NULL,
    "birthday"      timestamp(6),
    "active" bool,
    "weight"        float8,
    "height"        int2,
    CONSTRAINT "pk_test_table_ktorm" PRIMARY KEY ("id")
);

comment on table "test_table_ktorm" is '测试表';
COMMENT ON COLUMN "test_table_ktorm"."id" IS '主键';
COMMENT ON COLUMN "test_table_ktorm"."name" IS '名字';
COMMENT ON COLUMN "test_table_ktorm"."birthday" IS '生日';
COMMENT ON COLUMN "test_table_ktorm"."active" IS '是否生效';
COMMENT ON COLUMN "test_table_ktorm"."weight" IS '体重';
COMMENT ON COLUMN "test_table_ktorm"."height" IS '身高';

drop TABLE IF EXISTS "test_built_in_ktorm";
CREATE TABLE IF NOT EXISTS "test_built_in_ktorm"
(
    "id"       int2         not null,
    "name"     varchar(255) NOT NULL,
    "built_in" bool         not null,
    CONSTRAINT "pk_test_built_in_ktorm" PRIMARY KEY ("id")
);

COMMENT ON TABLE "test_built_in_ktorm" IS '带内置标记的测试表';
COMMENT ON COLUMN "test_built_in_ktorm"."id" IS '主键';
COMMENT ON COLUMN "test_built_in_ktorm"."name" IS '名称';
COMMENT ON COLUMN "test_built_in_ktorm"."built_in" IS '是否内置';