-- 外部角色来源用例：approver 只有一条"直授"角色（用于验证来源故障时不牵连既有权限），
-- 审批角色与作废角色都只由测试内注册的 position 来源贡献，库里没有任何 auth_role_user 行指向它们。

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('9a1f5c22-0000-0000-0000-0000000000e1', 'rs-user-approver-9a1f5c22', 'rs-tenant-9a1f5c22', 'pwd', '00000000-0000-0000-0000-000000000000', 'rs', true, false, 'system', '系统'),
    ('9a1f5c22-0000-0000-0000-0000000000e2', 'rs-user-nobody-9a1f5c22', 'rs-tenant-9a1f5c22', 'pwd', '00000000-0000-0000-0000-000000000000', 'rs', true, false, 'system', '系统');

-- b1 由岗位贡献；b2 由岗位贡献但已停用（验证来源不能豁免任何既有规则）；b3 为直授对照组
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('9a1f5c22-0000-0000-0000-0000000000b1', 'rs-role-approver-9a1f5c22', 'rs-role-approver', 'rs-tenant-9a1f5c22', 'ams', 'rs', true, false, 'system', '系统'),
    ('9a1f5c22-0000-0000-0000-0000000000b2', 'rs-role-void-9a1f5c22', 'rs-role-void', 'rs-tenant-9a1f5c22', 'ams', 'rs', false, false, 'system', '系统'),
    ('9a1f5c22-0000-0000-0000-0000000000b3', 'rs-role-reader-9a1f5c22', 'rs-role-reader', 'rs-tenant-9a1f5c22', 'ams', 'rs', true, false, 'system', '系统');

merge into "auth_role_resource" ("id", "role_id", "permission_code", "effect", "create_user_id", "create_user_name") values
    ('9a1f5c22-0000-0000-0000-0000000000c1', '9a1f5c22-0000-0000-0000-0000000000b1', 'rs:invoice:approve', 'ALLOW', 'system', '系统'),
    ('9a1f5c22-0000-0000-0000-0000000000c2', '9a1f5c22-0000-0000-0000-0000000000b2', 'rs:invoice:void', 'ALLOW', 'system', '系统'),
    ('9a1f5c22-0000-0000-0000-0000000000c3', '9a1f5c22-0000-0000-0000-0000000000b3', 'rs:invoice:read', 'ALLOW', 'system', '系统');

-- 唯一的直授：来源故障时它必须照常生效
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('9a1f5c22-0000-0000-0000-0000000000d1', '9a1f5c22-0000-0000-0000-0000000000b3', '9a1f5c22-0000-0000-0000-0000000000e1', 'system', '系统');
