CREATE TABLE IF NOT EXISTS "code_gen_column"
(
    "id"          CHAR(36)             default RANDOM_UUID() not null primary key,
    "name"        VARCHAR(63) NOT NULL,
    "object_name" VARCHAR(63) NOT NULL,
    "comment"     VARCHAR(127),
    "search_item" bool        NOT NULL DEFAULT false,
    "list_item"   bool        NOT NULL DEFAULT false,
    "edit_item"   bool        NOT NULL DEFAULT false,
    "detail_item" bool        NOT NULL DEFAULT false,
    "cache_item"  bool        NOT NULL DEFAULT false
);

COMMENT ON TABLE "code_gen_column" IS '代码生成-列信息';
COMMENT ON COLUMN "code_gen_column"."id" IS '主键';
COMMENT ON COLUMN "code_gen_column"."name" IS '字段名';
COMMENT ON COLUMN "code_gen_column"."object_name" IS '对象名称';
COMMENT ON COLUMN "code_gen_column"."comment" IS '注释';
COMMENT ON COLUMN "code_gen_column"."search_item" IS '是否查询项';
COMMENT ON COLUMN "code_gen_column"."list_item" IS '是否列表项';
COMMENT ON COLUMN "code_gen_column"."edit_item" IS '是否编辑项';
COMMENT ON COLUMN "code_gen_column"."detail_item" IS '是否详情项';
COMMENT ON COLUMN "code_gen_column"."cache_item" IS '是否缓存项';
