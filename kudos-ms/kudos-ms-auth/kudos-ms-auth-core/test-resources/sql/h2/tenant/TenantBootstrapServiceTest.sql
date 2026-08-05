-- 租户引导用例：只种入用户，角色由引导服务自己创建——这正是被测行为。
-- e9 属于另一个租户，用于验证引导调用虽跳过转授校验、但绝不跳过租户边界。

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('4c7d81ba-0000-0000-0000-0000000000e1', 'tb-user-admin-4c7d81ba', 'tb-tenant-fresh-4c7d81ba', 'pwd', '00000000-0000-0000-0000-000000000000', 'tb', true, false, 'system', '系统'),
    ('4c7d81ba-0000-0000-0000-0000000000e2', 'tb-user-second-4c7d81ba', 'tb-tenant-fresh-4c7d81ba', 'pwd', '00000000-0000-0000-0000-000000000000', 'tb', true, false, 'system', '系统'),
    ('4c7d81ba-0000-0000-0000-0000000000e9', 'tb-user-outsider-4c7d81ba', 'tb-tenant-other-4c7d81ba', 'pwd', '00000000-0000-0000-0000-000000000000', 'tb', true, false, 'system', '系统');
