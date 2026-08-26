-- 登录密码的归属迁往 auth 域的 auth_credential。
-- 本迁移只放开非空约束，不删除列：kudos-ms-user-core 仍可独立部署（此时没有 auth 提供的凭证存储 SPI
-- 实现，密码继续读写本列），列的删除留待所有部署完成切换之后。
ALTER TABLE "user_account" ALTER COLUMN "login_password" SET NULL;

COMMENT ON COLUMN "user_account"."login_password" IS
    '登录密码（已弃用）：装配了 auth 凭证存储 SPI 时以 auth_credential 为准，本列仅供无 auth 的独立部署使用';
