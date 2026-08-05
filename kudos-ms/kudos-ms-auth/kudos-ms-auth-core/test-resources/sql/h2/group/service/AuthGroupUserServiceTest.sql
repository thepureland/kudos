-- user_account: 每条 id 唯一，使用 svc-groupuser-test-* 前缀隔离
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('9c1b2a3d-0000-0000-0000-000000000080', 'svc-groupuser-test-1-Np4kQmR7', 'svc-tenant-gpu-test-1-Np4kQmR7', 'encrypted-pwd-1-Np4kQmR7', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserServiceTest', true, false, 'system', '系统'),
    ('9c1b2a3d-0000-0000-0000-000000000081', 'svc-groupuser-test-2-Np4kQmR7', 'svc-tenant-gpu-test-1-Np4kQmR7', 'encrypted-pwd-2-Np4kQmR7', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserServiceTest', true, false, 'system', '系统'),
    ('9c1b2a3d-0000-0000-0000-000000000082', 'svc-groupuser-test-3-Np4kQmR7', 'svc-tenant-gpu-test-1-Np4kQmR7', 'encrypted-pwd-3-Np4kQmR7', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserServiceTest', true, false, 'system', '系统');

-- auth_group: 每条 id 唯一
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('9c1b2a3d-0000-0000-0000-000000000083', 'svc-groupuser-test-1-Np4kQmR7', 'svc-groupuser-test-1-name-Np4kQmR7', 'svc-tenant-gpu-test-1-Np4kQmR7', 'ams', 'from AuthGroupUserServiceTest', true, false, 'system', '系统'),
    ('9c1b2a3d-0000-0000-0000-000000000084', 'svc-groupuser-test-2-Np4kQmR7', 'svc-groupuser-test-2-name-Np4kQmR7', 'svc-tenant-gpu-test-1-Np4kQmR7', 'ams', 'from AuthGroupUserServiceTest', true, false, 'system', '系统');

-- auth_group_user: 已存在关系供 exists 和 unbind 用例使用
merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name") values
    ('9c1b2a3d-0000-0000-0000-000000000085', '9c1b2a3d-0000-0000-0000-000000000083', '9c1b2a3d-0000-0000-0000-000000000080', 'system', '系统'),
    ('9c1b2a3d-0000-0000-0000-000000000086', '9c1b2a3d-0000-0000-0000-000000000083', '9c1b2a3d-0000-0000-0000-000000000081', 'system', '系统');

-- 组成员有效期扫描用例（announceWindowChanges）：一个组带一个角色，成员通过组间接持有该角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('9c1b2a3d-0000-0000-0000-000000000087', 'svc-gu-sweep-role-Np4k', 'svc-gu-sweep-role-name', 'svc-tenant-gpu-test-1-Np4kQmR7', 'ams', 'from AuthGroupUserServiceTest', true, false, 'system', '系统');

merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('9c1b2a3d-0000-0000-0000-000000000088', 'svc-gu-sweep-group-Np4k', 'svc-gu-sweep-group-name', 'svc-tenant-gpu-test-1-Np4kQmR7', 'ams', 'from AuthGroupUserServiceTest', true, false, 'system', '系统');

merge into "auth_group_role" ("id", "group_id", "role_id", "create_user_id", "create_user_name") values
    ('9c1b2a3d-0000-0000-0000-000000000089', '9c1b2a3d-0000-0000-0000-000000000088', '9c1b2a3d-0000-0000-0000-000000000087', 'system', '系统');
