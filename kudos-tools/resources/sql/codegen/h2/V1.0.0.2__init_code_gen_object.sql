CREATE TABLE IF NOT EXISTS "code_gen_object"
(
    "id"          CHAR(36)  default RANDOM_UUID() not null primary key,
    "name"        VARCHAR(64)                     NOT NULL,
    "comment"     VARCHAR(128),
    "create_time" timestamp default now()         NOT NULL,
    "create_user" varchar(36)                     NOT NULL,
    "update_time" timestamp,
    "update_user" varchar(36),
    "gen_count"   int4                            NOT NULL
);

COMMENT ON TABLE "code_gen_object" IS '代码生成-对象信息';
COMMENT ON COLUMN "code_gen_object"."id" IS '主键';
COMMENT ON COLUMN "code_gen_object"."name" IS '对象名称';
COMMENT ON COLUMN "code_gen_object"."comment" IS '注释';
COMMENT ON COLUMN "code_gen_object"."create_time" IS '创建时间';
COMMENT ON COLUMN "code_gen_object"."create_user" IS '创建用户';
COMMENT ON COLUMN "code_gen_object"."update_time" IS '更新时间';
COMMENT ON COLUMN "code_gen_object"."update_user" IS '更新用户';
COMMENT ON COLUMN "code_gen_object"."gen_count" IS '生成次数';