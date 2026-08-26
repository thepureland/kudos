-- 把既有登录密码搬进 auth_credential。
--
-- 直接切换而不是双写：本能力尚未上线，没有需要保护的生产数据，双写只会多出一套需要长期维持一致的写路径。
-- 回填仍然保留，因为开发与测试库里已经有账号行，让它们在切换后继续可登录。
--
-- 依赖同库：Flyway 的执行顺序是 sys → user → auth，因此本脚本运行时 user_account 已存在且列已可空。
-- 分库部署无法这样回填，需要改走应用层迁移——这一点写在 kudos-ms-auth-sql 的 README 里。
INSERT INTO "auth_credential" (
    "id", "tenant_id", "user_id", "type", "secret_hash_or_ref", "status", "version",
    "enrolled_at", "expires_at", "last_used_at", "metadata", "revoked_at", "revoke_reason",
    "create_time", "update_time"
)
SELECT
    RANDOM_UUID(), a."tenant_id", a."id", 'PASSWORD', a."login_password", 'ACTIVE', 0,
    COALESCE(a."create_time", CURRENT_TIMESTAMP), NULL, NULL, NULL, NULL, NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM "user_account" a
WHERE a."login_password" IS NOT NULL
  AND CHAR_LENGTH(a."login_password") > 0
  -- 幂等：重复执行或与已有行冲突时不重复写入（该表按 tenant+user+type+status 唯一）
  AND NOT EXISTS (
      SELECT 1 FROM "auth_credential" c
      WHERE c."tenant_id" = a."tenant_id" AND c."user_id" = a."id"
        AND c."type" = 'PASSWORD' AND c."status" = 'ACTIVE'
  );

-- 清空源列。留着它就等于留下一份没人维护的可用哈希：账号下次改密后它立刻过期，却依旧能通过校验，
-- 而这正是本次迁移要淘汰的副本。本脚本属于 auth-sql，只在装配了 auth 的部署上执行，此时凭证存储 SPI
-- 必然在位，该列已不再被读写；无 auth 的独立部署不会执行到这里，其密码仍留在列里。
UPDATE "user_account" a
SET "login_password" = ''
WHERE a."login_password" IS NOT NULL
  AND CHAR_LENGTH(a."login_password") > 0
  AND EXISTS (
      SELECT 1 FROM "auth_credential" c
      WHERE c."tenant_id" = a."tenant_id" AND c."user_id" = a."id"
        AND c."type" = 'PASSWORD' AND c."status" = 'ACTIVE'
  );
