-- 首任管理员并发竞态用例:同一个新租户,两个不同的候选人同时被任命。
-- 门闩是"该角色还没有持有者"——角色的属性,不是任何一个候选人的属性,
-- 所以 screenGrants 的按主体加锁根本锁不到一起,必须锁角色行本身。

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('a1710000-0000-0000-0000-000000000001', 'tbr-candidate-a-7q4c', 'tbr-tenant-7q4c', 'pwd', '00000000-0000-0000-0000-000000000000', 'from TenantBootstrapConcurrencyTest', true, false, 'system', '系统'),
    ('a1710000-0000-0000-0000-000000000002', 'tbr-candidate-b-7q4c', 'tbr-tenant-7q4c', 'pwd', '00000000-0000-0000-0000-000000000000', 'from TenantBootstrapConcurrencyTest', true, false, 'system', '系统');
