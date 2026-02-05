-- 机构 Hash 缓存需在 sys_cache 中标记为 hash 类型（表需有 hash 列，通常由 sys 模块初始化）
update "sys_cache" set "hash" = true where "name" = 'USER_ORG__HASH';
