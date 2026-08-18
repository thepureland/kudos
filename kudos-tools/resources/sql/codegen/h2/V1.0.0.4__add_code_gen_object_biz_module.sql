-- 业务模块（生成代码的一级包目录）。为空时代码生成器回退到"实体短名小写"。
-- 多张表同属一个业务模块时（如 sys_dict / sys_dict_item 同属 dict），需在向导里显式填写。
ALTER TABLE "code_gen_object"
    ADD COLUMN IF NOT EXISTS "biz_module" VARCHAR(64);

COMMENT ON COLUMN "code_gen_object"."biz_module" IS '业务模块(生成代码的一级包目录)';
