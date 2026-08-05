-- 机器主体用例：全程不涉及任何 user_account 行——这正是要验证的点。
-- 服务主体由测试内注册的 SERVICE 目录提供，角色与权限绑定在此。

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('8d4e1a70-0000-0000-0000-0000000000b1', 'mp-role-etl-8d4e1a70', 'mp-role-etl', 'mp-tenant-8d4e1a70', 'ams', 'mp', true, false, 'system', '系统'),
    ('8d4e1a70-0000-0000-0000-0000000000b2', 'mp-role-other-8d4e1a70', 'mp-role-other', 'mp-tenant-other-8d4e1a70', 'ams', 'mp', true, false, 'system', '系统');

merge into "auth_role_resource" ("id", "role_id", "permission_code", "effect", "create_user_id", "create_user_name") values
    ('8d4e1a70-0000-0000-0000-0000000000c1', '8d4e1a70-0000-0000-0000-0000000000b1', 'mp:etl:run', 'ALLOW', 'system', '系统');
