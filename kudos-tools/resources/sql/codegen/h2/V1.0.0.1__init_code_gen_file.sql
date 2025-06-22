CREATE TABLE IF NOT EXISTS "code_gen_file"
(
    "id"          CHAR(36) default RANDOM_UUID() not null primary key,
    "filename"    VARCHAR(64)                    NOT NULL,
    "object_name" VARCHAR(64)                    NOT NULL
);

COMMENT ON TABLE "code_gen_file" IS '代码生成-文件信息';
COMMENT ON COLUMN "code_gen_file"."id" IS '主键';
COMMENT ON COLUMN "code_gen_file"."filename" IS '文件名';
COMMENT ON COLUMN "code_gen_file"."object_name" IS '对象名';